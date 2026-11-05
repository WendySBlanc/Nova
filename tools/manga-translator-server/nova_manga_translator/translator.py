from __future__ import annotations

import hashlib
import json
import sys
import threading
from dataclasses import dataclass
from pathlib import Path
from typing import Callable, Protocol
from urllib.parse import urlsplit


class TranslationCancelled(Exception):
    """Raised when a running chapter translation is cancelled."""


def _install_local_ollama_compatibility() -> None:
    """Keep Qwen thinking enabled through Ollama's OpenAI-compatible endpoint."""
    from utils.endpoints import openai_compatible

    original_post = openai_compatible.post_with_retries
    if getattr(original_post, "_nova_local_ollama_compatibility", False):
        return

    def post_with_retries(url, headers, payload, parse, *args, **kwargs):
        try:
            endpoint = urlsplit(url)
            is_local_ollama = (
                endpoint.hostname in {"127.0.0.1", "localhost", "::1"}
                and endpoint.port == 11434
            )
        except ValueError:
            is_local_ollama = False

        model_name = str(payload.get("model", "")).lower()
        if is_local_ollama and "qwen3" in model_name:
            payload = {**payload, "reasoning_effort": "low"}
        return original_post(url, headers, payload, parse, *args, **kwargs)

    post_with_retries._nova_local_ollama_compatibility = True
    openai_compatible.post_with_retries = post_with_retries


@dataclass(frozen=True)
class TranslationOutcome:
    target_language: str
    config_revision: str


@dataclass(frozen=True)
class TranslationConfigSnapshot:
    config_revision: str
    payload: bytes | None = None


class ChapterTranslator(Protocol):
    def snapshot_configuration(self) -> TranslationConfigSnapshot: ...

    def translate(
        self,
        input_dir: Path,
        output_dir: Path,
        progress: Callable[[int, int], None],
        cancelled: threading.Event,
        configuration: TranslationConfigSnapshot,
    ) -> TranslationOutcome: ...


class MangaTranslatorAdapter:
    """Loads MangaTranslator's saved UI config and invokes its batch pipeline."""

    def __init__(
        self,
        manga_translator_root: Path,
        config_path: Path,
        *,
        models_dir: Path | None = None,
        fonts_dir: Path | None = None,
        force_cpu: bool = False,
    ) -> None:
        self.root = Path(manga_translator_root).resolve()
        self.config_path = Path(config_path).resolve()
        self.models_dir = (models_dir or self.root / "models").resolve()
        self.fonts_dir = (fonts_dir or self.root / "fonts").resolve()
        self.force_cpu = force_cpu

    def snapshot_configuration(self) -> TranslationConfigSnapshot:
        if not self.config_path.is_file():
            raise RuntimeError(f"MangaTranslator config not found: {self.config_path}")
        config_bytes = self.config_path.read_bytes()
        return TranslationConfigSnapshot(
            config_revision=hashlib.sha256(config_bytes).hexdigest()[:16],
            payload=config_bytes,
        )

    def translate(
        self,
        input_dir: Path,
        output_dir: Path,
        progress: Callable[[int, int], None],
        cancelled: threading.Event,
        configuration: TranslationConfigSnapshot,
    ) -> TranslationOutcome:
        if not self.root.is_dir():
            raise RuntimeError(f"MangaTranslator directory not found: {self.root}")
        if configuration.payload is None:
            raise RuntimeError("MangaTranslator configuration snapshot is missing")

        root_text = str(self.root)
        if root_text not in sys.path:
            sys.path.insert(0, root_text)

        import torch
        from core.device import get_best_device
        from core.ml.model_manager import get_model_manager, migrate_model_dirs
        from core.pipeline import batch_translate_images
        from core.validation import autodetect_yolo_model_path
        from ui.ui_models import UIConfigState, map_ui_to_backend_config

        _install_local_ollama_compatibility()

        config_bytes = configuration.payload
        ui_state = UIConfigState.from_dict(json.loads(config_bytes.decode("utf-8")))

        self.models_dir.mkdir(parents=True, exist_ok=True)
        self.fonts_dir.mkdir(parents=True, exist_ok=True)
        migrate_model_dirs(self.models_dir)
        manager = get_model_manager()
        manager.set_models_dir(self.models_dir)
        device = torch.device("cpu") if self.force_cpu else get_best_device()
        manager.set_device(device)

        backend_config = map_ui_to_backend_config(
            ui_state,
            fonts_base_dir=self.fonts_dir,
            target_device=device,
            is_batch=True,
        )
        backend_config.yolo_model_path = str(
            autodetect_yolo_model_path(
                self.models_dir,
                backend_config.detection.bubble_detector_model,
            ).resolve(),
        )
        font_pack = ui_state.batch_font_pack
        if not font_pack:
            raise RuntimeError("MangaTranslator batch font pack is not configured")
        font_dir = (self.fonts_dir / font_pack).resolve()
        if not font_dir.is_dir() or not any(
            path.suffix.lower() in {".otf", ".ttf"} for path in font_dir.iterdir()
        ):
            raise RuntimeError(f"MangaTranslator font pack is invalid: {font_dir}")
        backend_config.rendering.font_dir = str(font_dir)
        pages = sorted(path for path in Path(input_dir).iterdir() if path.is_file())
        total = len(pages)

        def on_progress(value: float, _message: str) -> None:
            if cancelled.is_set():
                raise TranslationCancelled()
            progress(min(total, max(0, round(value * total))), total)

        if cancelled.is_set():
            raise TranslationCancelled()
        batch_translate_images(
            input_dir=input_dir,
            config=backend_config,
            output_dir=output_dir,
            progress_callback=on_progress,
            preserve_structure=False,
        )
        if cancelled.is_set():
            raise TranslationCancelled()

        return TranslationOutcome(
            target_language=str(backend_config.translation.output_language),
            config_revision=configuration.config_revision,
        )
