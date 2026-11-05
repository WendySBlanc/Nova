from __future__ import annotations

import hashlib
import json
import sys
import threading
from dataclasses import dataclass
from pathlib import Path
from typing import Callable, Protocol


class TranslationCancelled(Exception):
    """Raised when a running chapter translation is cancelled."""


@dataclass(frozen=True)
class TranslationOutcome:
    target_language: str
    config_revision: str


class ChapterTranslator(Protocol):
    def translate(
        self,
        input_dir: Path,
        output_dir: Path,
        progress: Callable[[int, int], None],
        cancelled: threading.Event,
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

    def translate(
        self,
        input_dir: Path,
        output_dir: Path,
        progress: Callable[[int, int], None],
        cancelled: threading.Event,
    ) -> TranslationOutcome:
        if not self.root.is_dir():
            raise RuntimeError(f"MangaTranslator directory not found: {self.root}")
        if not self.config_path.is_file():
            raise RuntimeError(f"MangaTranslator config not found: {self.config_path}")

        root_text = str(self.root)
        if root_text not in sys.path:
            sys.path.insert(0, root_text)

        import torch
        from core.device import get_best_device
        from core.ml.model_manager import get_model_manager, migrate_model_dirs
        from core.pipeline import batch_translate_images
        from ui.ui_models import UIConfigState, map_ui_to_backend_config

        config_bytes = self.config_path.read_bytes()
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
        pages = sorted(path for path in Path(input_dir).iterdir() if path.is_file())
        total = len(pages)

        def on_progress(value: float, _message: str) -> None:
            if cancelled.is_set():
                raise TranslationCancelled()
            progress(min(total, max(0, round(value * total))), total)

        if cancelled.is_set():
            raise TranslationCancelled()
        results = batch_translate_images(
            input_dir=input_dir,
            config=backend_config,
            output_dir=output_dir,
            progress_callback=on_progress,
            preserve_structure=False,
        )
        if cancelled.is_set():
            raise TranslationCancelled()
        if results.get("error_count", 0):
            errors = results.get("errors") or {}
            message = "; ".join(f"{name}: {error}" for name, error in errors.items())
            raise RuntimeError(message or "MangaTranslator failed to translate every page")

        revision = hashlib.sha256(config_bytes).hexdigest()[:16]
        return TranslationOutcome(
            target_language=str(backend_config.translation.output_language),
            config_revision=revision,
        )
