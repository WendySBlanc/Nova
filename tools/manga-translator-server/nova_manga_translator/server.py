from __future__ import annotations

import os
from argparse import ArgumentParser
from pathlib import Path

import uvicorn

from .api import create_app
from .translator import MangaTranslatorAdapter


def main() -> None:
    parser = ArgumentParser(description="Nova MangaTranslator private server")
    parser.add_argument("--root", default=os.environ.get("NOVA_MANGA_TRANSLATOR_ROOT"))
    parser.add_argument("--config", default=os.environ.get("NOVA_MANGA_TRANSLATOR_CONFIG"))
    parser.add_argument("--data", default=os.environ.get("NOVA_MANGA_TRANSLATOR_DATA"))
    parser.add_argument("--port", type=int, default=int(os.environ.get("NOVA_MANGA_TRANSLATOR_PORT", "8765")))
    parser.add_argument("--force-cpu", action="store_true")
    args = parser.parse_args()

    token = os.environ.get("NOVA_MANGA_TRANSLATOR_TOKEN")
    if not token:
        parser.error("NOVA_MANGA_TRANSLATOR_TOKEN is required")
    if not args.root or not args.config or not args.data:
        parser.error("--root, --config, and --data are required")

    root = Path(args.root)
    translator = MangaTranslatorAdapter(
        manga_translator_root=root,
        config_path=Path(args.config),
        models_dir=root / "models",
        fonts_dir=root / "fonts",
        force_cpu=args.force_cpu or os.environ.get("NOVA_MANGA_TRANSLATOR_FORCE_CPU") == "1",
    )
    app = create_app(
        data_dir=Path(args.data),
        token=token,
        translator=translator,
    )
    uvicorn.run(
        app,
        host="127.0.0.1",
        port=args.port,
        access_log=False,
    )


if __name__ == "__main__":
    main()
