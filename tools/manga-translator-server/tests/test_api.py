from __future__ import annotations

import io
import json
import threading
import time
import zipfile
from pathlib import Path

from fastapi.testclient import TestClient

from nova_manga_translator.api import create_app
from nova_manga_translator.translator import TranslationCancelled, TranslationOutcome


TOKEN = "nova-test-token"


def chapter_zip(*pages: bytes) -> bytes:
    output = io.BytesIO()
    with zipfile.ZipFile(output, "w", zipfile.ZIP_DEFLATED) as archive:
        for index, page in enumerate(pages):
            archive.writestr(f"page_{index:05d}.jpg", page)
    return output.getvalue()


def auth_headers() -> dict[str, str]:
    return {"Authorization": f"Bearer {TOKEN}"}


def wait_for_status(
    client: TestClient,
    job_id: str,
    expected: str,
    timeout: float = 3.0,
) -> dict:
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        response = client.get(f"/v1/chapters/{job_id}", headers=auth_headers())
        assert response.status_code == 200
        payload = response.json()
        if payload["status"] == expected:
            return payload
        time.sleep(0.01)
    raise AssertionError(f"job {job_id} did not reach {expected}")


class RecordingTranslator:
    def __init__(self) -> None:
        self.calls = 0

    def translate(self, input_dir, output_dir, progress, cancelled):
        self.calls += 1
        pages = sorted(Path(input_dir).glob("page_*.jpg"))
        for index, page in enumerate(pages, start=1):
            if cancelled.is_set():
                raise TranslationCancelled()
            Path(output_dir, page.name).write_bytes(b"translated:" + page.read_bytes())
            progress(index, len(pages))
        return TranslationOutcome(target_language="French", config_revision="config-v1")


class BlockingTranslator:
    def __init__(self) -> None:
        self.started = threading.Event()

    def translate(self, input_dir, output_dir, progress, cancelled):
        pages = sorted(Path(input_dir).glob("page_*.jpg"))
        self.started.set()
        while not cancelled.wait(0.01):
            pass
        raise TranslationCancelled()


def submit(client: TestClient, chapter_key: str, pages: tuple[bytes, ...] = (b"one", b"two")):
    return client.post(
        "/v1/chapters",
        headers=auth_headers(),
        data={"chapter_key": chapter_key},
        files={"archive": ("chapter.zip", chapter_zip(*pages), "application/zip")},
    )


def test_health_requires_valid_bearer_token(tmp_path: Path) -> None:
    app = create_app(tmp_path, TOKEN, RecordingTranslator())
    with TestClient(app) as client:
        assert client.get("/v1/health").status_code == 401
        assert client.get(
            "/v1/health",
            headers={"Authorization": "Bearer wrong"},
        ).status_code == 401

        response = client.get("/v1/health", headers=auth_headers())

    assert response.status_code == 200
    assert response.json() == {"api_version": 1, "status": "ok"}


def test_submit_reports_progress_and_downloads_ordered_result(tmp_path: Path) -> None:
    app = create_app(tmp_path, TOKEN, RecordingTranslator())
    with TestClient(app) as client:
        response = submit(client, "source:manga:chapter:2")
        assert response.status_code == 202
        payload = response.json()
        completed = wait_for_status(client, payload["job_id"], "complete")

        assert completed["completed_pages"] == 2
        assert completed["total_pages"] == 2
        assert completed["target_language"] == "French"
        assert completed["config_revision"] == "config-v1"

        result = client.get(
            f"/v1/chapters/{payload['job_id']}/result",
            headers=auth_headers(),
        )

    assert result.status_code == 200
    with zipfile.ZipFile(io.BytesIO(result.content)) as archive:
        assert archive.namelist() == ["page_00000.jpg", "page_00001.jpg"]
        assert archive.read("page_00000.jpg") == b"translated:one"


def test_repeated_chapter_key_reuses_existing_job(tmp_path: Path) -> None:
    translator = RecordingTranslator()
    app = create_app(tmp_path, TOKEN, translator)
    with TestClient(app) as client:
        first = submit(client, "same-chapter").json()
        wait_for_status(client, first["job_id"], "complete")
        second_response = submit(client, "same-chapter")
        second = second_response.json()

    assert second_response.status_code == 200
    assert second["job_id"] == first["job_id"]
    assert second["cached"] is True
    assert translator.calls == 1


def test_queued_job_resumes_after_server_restart(tmp_path: Path) -> None:
    dormant = create_app(tmp_path, TOKEN, RecordingTranslator(), start_workers=False)
    with TestClient(dormant) as client:
        queued = submit(client, "resume-after-restart").json()
        assert queued["status"] == "queued"

    translator = RecordingTranslator()
    restarted = create_app(tmp_path, TOKEN, translator)
    with TestClient(restarted) as client:
        completed = wait_for_status(client, queued["job_id"], "complete")

    assert completed["completed_pages"] == 2
    assert translator.calls == 1


def test_delete_cancels_running_job(tmp_path: Path) -> None:
    translator = BlockingTranslator()
    app = create_app(tmp_path, TOKEN, translator)
    with TestClient(app) as client:
        job = submit(client, "cancel-me").json()
        assert translator.started.wait(1)

        response = client.delete(
            f"/v1/chapters/{job['job_id']}",
            headers=auth_headers(),
        )
        cancelled = wait_for_status(client, job["job_id"], "cancelled")

    assert response.status_code == 202
    assert cancelled["error"] is None


def test_persisted_records_do_not_contain_access_token(tmp_path: Path) -> None:
    app = create_app(tmp_path, TOKEN, RecordingTranslator())
    with TestClient(app) as client:
        job = submit(client, "secret-safe").json()
        wait_for_status(client, job["job_id"], "complete")

    records = "".join(path.read_text("utf-8") for path in tmp_path.rglob("*.json"))
    assert TOKEN not in records
    assert json.loads(next(tmp_path.rglob("*.json")).read_text("utf-8"))["chapter_key"] == "secret-safe"


def test_submit_rejects_path_traversal_archive(tmp_path: Path) -> None:
    archive_bytes = io.BytesIO()
    with zipfile.ZipFile(archive_bytes, "w") as archive:
        archive.writestr("../outside.jpg", b"unsafe")

    app = create_app(tmp_path, TOKEN, RecordingTranslator())
    with TestClient(app) as client:
        response = client.post(
            "/v1/chapters",
            headers=auth_headers(),
            data={"chapter_key": "unsafe"},
            files={"archive": ("chapter.zip", archive_bytes.getvalue(), "application/zip")},
        )

    assert response.status_code == 400
    assert not Path(tmp_path, "outside.jpg").exists()
