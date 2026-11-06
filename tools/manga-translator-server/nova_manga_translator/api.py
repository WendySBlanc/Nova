from __future__ import annotations

import hmac
import uuid
from contextlib import asynccontextmanager
from pathlib import Path

from fastapi import Depends, FastAPI, File, Form, HTTPException, Request, UploadFile, status
from fastapi.responses import FileResponse, JSONResponse
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer

from .jobs import InvalidChapterArchive, JobManager
from .translator import ChapterTranslator


MAX_UPLOAD_BYTES = 512 * 1024 * 1024
UPLOAD_CHUNK_BYTES = 1024 * 1024
MAX_MULTIPART_OVERHEAD_BYTES = 1024 * 1024


def create_app(
    data_dir: Path,
    token: str,
    translator: ChapterTranslator,
    *,
    start_workers: bool = True,
) -> FastAPI:
    if not token:
        raise ValueError("A non-empty access token is required")
    data_dir = Path(data_dir).resolve()
    manager = JobManager(data_dir, translator, start_workers=start_workers)
    bearer = HTTPBearer(auto_error=False)

    @asynccontextmanager
    async def lifespan(_app: FastAPI):
        manager.resume_pending()
        try:
            yield
        finally:
            manager.close()

    app = FastAPI(title="Nova MangaTranslator", version="1", lifespan=lifespan)
    app.state.job_manager = manager

    @app.middleware("http")
    async def reject_oversized_chapter_requests(request: Request, call_next):
        if request.method == "POST" and request.url.path.endswith("/v1/chapters"):
            content_length = request.headers.get("content-length")
            if content_length is not None:
                try:
                    request_bytes = int(content_length)
                except ValueError:
                    return JSONResponse({"detail": "Invalid Content-Length"}, status_code=400)
                if request_bytes > MAX_UPLOAD_BYTES + MAX_MULTIPART_OVERHEAD_BYTES:
                    return JSONResponse({"detail": "Chapter archive is too large"}, status_code=413)
        return await call_next(request)

    def authorize(
        credentials: HTTPAuthorizationCredentials | None = Depends(bearer),
    ) -> None:
        valid = credentials is not None and hmac.compare_digest(credentials.credentials, token)
        if not valid:
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Invalid access token",
                headers={"WWW-Authenticate": "Bearer"},
            )

    @app.get("/v1/health", dependencies=[Depends(authorize)])
    def health() -> dict:
        configuration = manager.snapshot_configuration()
        return {
            "api_version": 1,
            "status": "ok",
            "config_revision": configuration.config_revision,
        }

    @app.post("/v1/chapters", dependencies=[Depends(authorize)])
    def submit_chapter(
        chapter_key: str = Form(min_length=1, max_length=512),
        force: bool = Form(False),
        replacement_page_index: int | None = Form(None, ge=0),
        archive: UploadFile = File(),
    ):
        configuration = manager.snapshot_configuration()
        existing = manager.find_by_chapter(
            chapter_key,
            configuration.config_revision,
            replacement_page_index,
        )
        if not force and existing is not None and existing.status not in {"failed", "cancelled"}:
            return existing.public_dict(cached=True)

        incoming_dir = data_dir / "incoming"
        incoming_dir.mkdir(parents=True, exist_ok=True)
        incoming = incoming_dir / f"{uuid.uuid4().hex}.zip"
        try:
            with incoming.open("wb") as output:
                uploaded_bytes = 0
                while chunk := archive.file.read(UPLOAD_CHUNK_BYTES):
                    uploaded_bytes += len(chunk)
                    if uploaded_bytes > MAX_UPLOAD_BYTES:
                        raise HTTPException(status_code=413, detail="Chapter archive is too large")
                    output.write(chunk)
            record, cached = manager.submit(
                chapter_key,
                incoming,
                configuration,
                force=force,
                replacement_page_index=replacement_page_index,
            )
        except InvalidChapterArchive as error:
            incoming.unlink(missing_ok=True)
            raise HTTPException(status_code=400, detail=str(error)) from error
        except HTTPException:
            incoming.unlink(missing_ok=True)
            raise
        finally:
            archive.file.close()

        response_status = status.HTTP_200_OK if cached else status.HTTP_202_ACCEPTED
        return JSONResponse(record.public_dict(cached=cached), status_code=response_status)

    @app.get("/v1/chapters/{job_id}", dependencies=[Depends(authorize)])
    def get_chapter(job_id: str) -> dict:
        record = manager.get(job_id)
        if record is None:
            raise HTTPException(status_code=404, detail="Unknown translation job")
        return record.public_dict()

    @app.get("/v1/chapters/{job_id}/result", dependencies=[Depends(authorize)])
    def get_result(job_id: str):
        record = manager.get(job_id)
        if record is None:
            raise HTTPException(status_code=404, detail="Unknown translation job")
        if record.status != "complete" or record.result_path is None:
            raise HTTPException(status_code=409, detail="Translation result is not ready")
        result = Path(record.result_path)
        if not result.is_file():
            raise HTTPException(status_code=410, detail="Translation result is no longer available")
        return FileResponse(result, media_type="application/zip", filename="translated-chapter.zip")

    @app.delete("/v1/chapters/{job_id}", dependencies=[Depends(authorize)])
    def cancel_chapter(job_id: str):
        record = manager.cancel(job_id)
        if record is None:
            raise HTTPException(status_code=404, detail="Unknown translation job")
        return JSONResponse(record.public_dict(), status_code=status.HTTP_202_ACCEPTED)

    return app
