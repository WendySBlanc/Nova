from __future__ import annotations

import sys
import threading
from pathlib import Path
from types import SimpleNamespace

import nova_manga_translator.translator as translator_module
from nova_manga_translator.translator import MangaTranslatorAdapter, TranslationConfigSnapshot


def test_local_ollama_qwen_request_keeps_reasoning_enabled(monkeypatch) -> None:
    captured_payloads = []

    def post_with_retries(_url, _headers, payload, _parse, **_kwargs):
        captured_payloads.append(payload)

    endpoint = SimpleNamespace(post_with_retries=post_with_retries)
    monkeypatch.setitem(sys.modules, "utils", SimpleNamespace())
    monkeypatch.setitem(sys.modules, "utils.endpoints", SimpleNamespace(openai_compatible=endpoint))
    monkeypatch.setitem(sys.modules, "utils.endpoints.openai_compatible", endpoint)

    install_compatibility = getattr(
        translator_module,
        "_install_local_ollama_compatibility",
        lambda: None,
    )
    install_compatibility()
    endpoint.post_with_retries(
        "http://127.0.0.1:11434/v1/chat/completions",
        {},
        {"model": "nova-qwen3.5:9b", "max_tokens": 4096},
        lambda result: result,
        label="OpenAI-Compatible",
    )

    assert captured_payloads == [
        {
            "model": "nova-qwen3.5:9b",
            "max_tokens": 4096,
            "reasoning_effort": "low",
        },
    ]


def test_remote_openai_compatible_request_keeps_original_payload(monkeypatch) -> None:
    captured_payloads = []

    def post_with_retries(_url, _headers, payload, _parse, **_kwargs):
        captured_payloads.append(payload)

    endpoint = SimpleNamespace(post_with_retries=post_with_retries)
    monkeypatch.setitem(sys.modules, "utils", SimpleNamespace())
    monkeypatch.setitem(sys.modules, "utils.endpoints", SimpleNamespace(openai_compatible=endpoint))
    monkeypatch.setitem(sys.modules, "utils.endpoints.openai_compatible", endpoint)

    install_compatibility = getattr(
        translator_module,
        "_install_local_ollama_compatibility",
        lambda: None,
    )
    install_compatibility()
    endpoint.post_with_retries(
        "https://api.example.com/v1/chat/completions",
        {},
        {"model": "qwen3.5:9b", "max_tokens": 4096},
        lambda result: result,
        label="OpenAI-Compatible",
    )

    assert captured_payloads == [{"model": "qwen3.5:9b", "max_tokens": 4096}]


def test_adapter_allows_individual_page_failures(tmp_path: Path, monkeypatch) -> None:
    root = tmp_path / "MangaTranslator"
    models = root / "models"
    fonts = root / "fonts"
    font_pack = fonts / "test-font"
    input_dir = tmp_path / "input"
    output_dir = tmp_path / "output"
    for directory in (models, font_pack, input_dir, output_dir):
        directory.mkdir(parents=True, exist_ok=True)
    (font_pack / "font.ttf").write_bytes(b"font")
    (input_dir / "000000.jpg").write_bytes(b"one")
    (input_dir / "000001.jpg").write_bytes(b"two")

    manager = SimpleNamespace(set_models_dir=lambda _path: None, set_device=lambda _device: None)
    backend_config = SimpleNamespace(
        detection=SimpleNamespace(bubble_detector_model="detector"),
        rendering=SimpleNamespace(font_dir=None),
        translation=SimpleNamespace(output_language="French"),
        yolo_model_path=None,
    )
    ui_state = SimpleNamespace(batch_font_pack="test-font")
    endpoint = SimpleNamespace(post_with_retries=lambda *_args, **_kwargs: None)

    def batch_translate_images(**kwargs):
        (kwargs["output_dir"] / "000000_translated.png").write_bytes(b"translated:one")
        return {
            "error_count": 1,
            "errors": {"000001.jpg": "All bubbles failed."},
        }

    modules = {
        "torch": SimpleNamespace(device=lambda value: value),
        "core": SimpleNamespace(),
        "core.device": SimpleNamespace(get_best_device=lambda: "cpu"),
        "core.ml": SimpleNamespace(),
        "core.ml.model_manager": SimpleNamespace(
            get_model_manager=lambda: manager,
            migrate_model_dirs=lambda _path: None,
        ),
        "core.pipeline": SimpleNamespace(batch_translate_images=batch_translate_images),
        "core.validation": SimpleNamespace(autodetect_yolo_model_path=lambda *_args: models / "detector.pt"),
        "ui": SimpleNamespace(),
        "ui.ui_models": SimpleNamespace(
            UIConfigState=SimpleNamespace(from_dict=lambda _payload: ui_state),
            map_ui_to_backend_config=lambda *_args, **_kwargs: backend_config,
        ),
        "utils": SimpleNamespace(),
        "utils.endpoints": SimpleNamespace(openai_compatible=endpoint),
        "utils.endpoints.openai_compatible": endpoint,
    }
    for name, module in modules.items():
        monkeypatch.setitem(sys.modules, name, module)

    adapter = MangaTranslatorAdapter(
        root,
        tmp_path / "config.json",
        models_dir=models,
        fonts_dir=fonts,
        force_cpu=True,
    )

    outcome = adapter.translate(
        input_dir,
        output_dir,
        lambda _completed, _total: None,
        threading.Event(),
        TranslationConfigSnapshot("revision", b"{}"),
    )

    assert outcome.target_language == "French"
    assert (output_dir / "000000_translated.png").is_file()
