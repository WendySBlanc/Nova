from __future__ import annotations

import io
import json
import threading
import time
import zipfile
from pathlib import Path
from types import SimpleNamespace

from fastapi.testclient import TestClient

from nova_manga_translator.api import create_app
from nova_manga_translator.translator import TranslationCancelled, TranslationOutcome


TOKEN = "nova-test-token"


def chapter_zip(*pages: bytes) -> bytes:
    output = io.BytesIO()
    with zipfile.ZipFile(output, "w", zipfile.ZIP_DEFLATED) as archive:
        for index, page in enumerate(pages):
            archive.writestr(f"{index:06d}.jpg", page)
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
    def __init__(self, revision: str = "config-v1") -> None:
        self.calls = 0
        self.revision = revision

    def snapshot_configuration(self):
        return SimpleNamespace(config_revision=self.revision, payload=b"test-config")

    def translate(self, input_dir, output_dir, progress, cancelled, configuration=None):
        self.calls += 1
        pages = sorted(Path(input_dir).glob("*.jpg"))
        for index, page in enumerate(pages, start=1):
            if cancelled.is_set():
                raise TranslationCancelled()
            Path(output_dir, page.name).write_bytes(b"translated:" + page.read_bytes())
            progress(index, len(pages))
        revision = configuration.config_revision if configuration is not None else self.revision
        return TranslationOutcome(target_language="French", config_revision=revision)


class MangaTranslatorNamingTranslator(RecordingTranslator):
    def translate(self, input_dir, output_dir, progress, cancelled, configuration=None):
        self.calls += 1
        pages = sorted(Path(input_dir).glob("*.jpg"))
        for index, page in enumerate(pages, start=1):
            Path(output_dir, f"{page.stem}_translated.png").write_bytes(
                b"translated:" + page.read_bytes(),
            )
            progress(index, len(pages))
        revision = configuration.config_revision if configuration is not None else self.revision
        return TranslationOutcome(target_language="French", config_revision=revision)


class PartialTranslationTranslator(RecordingTranslator):
    def translate(self, input_dir, output_dir, progress, cancelled, configuration=None):
        self.calls += 1
        pages = sorted(Path(input_dir).glob("*.jpg"))
        Path(output_dir, f"{pages[0].stem}_translated.png").write_bytes(
            b"translated:" + pages[0].read_bytes(),
        )
        progress(len(pages), len(pages))
        revision = configuration.config_revision if configuration is not None else self.revision
        return TranslationOutcome(target_language="French", config_revision=revision)


class BlockingTranslator:
    def __init__(self) -> None:
        self.started = threading.Event()

    def snapshot_configuration(self):
        return SimpleNamespace(config_revision="config-v1", payload=b"test-config")

    def translate(self, input_dir, output_dir, progress, cancelled, configuration=None):
        pages = sorted(Path(input_dir).glob("*.jpg"))
        self.started.set()
        while not cancelled.wait(0.01):
            pass
        raise TranslationCancelled()


def submit(
    client: TestClient,
    chapter_key: str,
    pages: tuple[bytes, ...] = (b"one", b"two"),
    *,
    force: bool = False,
):
    return client.post(
        "/v1/chapters",
        headers=auth_headers(),
        data={"chapter_key": chapter_key, "force": str(force).lower()},
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
    assert response.json() == {
        "api_version": 1,
        "status": "ok",
        "config_revision": "config-v1",
    }


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
        assert archive.namelist() == ["000000.jpg", "000001.jpg"]
        assert archive.read("000000.jpg") == b"translated:one"


def test_packages_manga_translator_output_names_as_canonical_pages(tmp_path: Path) -> None:
    app = create_app(tmp_path, TOKEN, MangaTranslatorNamingTranslator())
    with TestClient(app) as client:
        response = submit(client, "manga-translator-output-names")
        completed = wait_for_status(client, response.json()["job_id"], "complete")
        result = client.get(
            f"/v1/chapters/{completed['job_id']}/result",
            headers=auth_headers(),
        )

    assert result.status_code == 200
    with zipfile.ZipFile(io.BytesIO(result.content)) as archive:
        assert archive.namelist() == ["000000.png", "000001.png"]
        assert archive.read("000000.png") == b"translated:one"


def test_keeps_original_pages_when_manga_translator_omits_them(tmp_path: Path) -> None:
    app = create_app(tmp_path, TOKEN, PartialTranslationTranslator())
    with TestClient(app) as client:
        response = submit(client, "partial-manga-translator-output")
        completed = wait_for_status(client, response.json()["job_id"], "complete")
        result = client.get(
            f"/v1/chapters/{completed['job_id']}/result",
            headers=auth_headers(),
        )

    assert result.status_code == 200
    with zipfile.ZipFile(io.BytesIO(result.content)) as archive:
        assert archive.namelist() == ["000000.png", "000001.jpg"]
        assert archive.read("000000.png") == b"translated:one"
        assert archive.read("000001.jpg") == b"two"
    work_directory = tmp_path / "work" / completed["job_id"]
    assert not (work_directory / "input").exists()
    assert not (work_directory / "output").exists()
    assert (work_directory / "translated.zip").is_file()


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


def test_forced_chapter_submission_creates_a_new_job(tmp_path: Path) -> None:
    translator = RecordingTranslator()
    app = create_app(tmp_path, TOKEN, translator)
    with TestClient(app) as client:
        first = submit(client, "same-chapter").json()
        wait_for_status(client, first["job_id"], "complete")

        second_response = submit(client, "same-chapter", force=True)
        second = second_response.json()
        wait_for_status(client, second["job_id"], "complete")

    assert second_response.status_code == 202
    assert second["job_id"] != first["job_id"]
    assert second["cached"] is False
    assert translator.calls == 2


def test_changed_configuration_creates_a_new_job(tmp_path: Path) -> None:
    translator = RecordingTranslator()
    app = create_app(tmp_path, TOKEN, translator)
    with TestClient(app) as client:
        first = submit(client, "same-chapter").json()
        wait_for_status(client, first["job_id"], "complete")
        translator.revision = "config-v2"

        second_response = submit(client, "same-chapter")
        second = second_response.json()
        wait_for_status(client, second["job_id"], "complete")

    assert second_response.status_code == 202
    assert second["job_id"] != first["job_id"]
    assert second["config_revision"] == "config-v2"
    assert translator.calls == 2


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


def test_queued_job_keeps_the_configuration_snapshot_from_submission(tmp_path: Path) -> None:
    translator = RecordingTranslator(revision="config-v1")
    dormant = create_app(tmp_path, TOKEN, translator, start_workers=False)
    with TestClient(dormant) as client:
        queued = submit(client, "snapshot-on-submit").json()

    translator.revision = "config-v2"
    restarted = create_app(tmp_path, TOKEN, translator)
    with TestClient(restarted) as client:
        completed = wait_for_status(client, queued["job_id"], "complete")

    assert completed["config_revision"] == "config-v1"


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


def test_submit_rejects_upload_larger_than_limit(tmp_path: Path, monkeypatch) -> None:
    monkeypatch.setattr("nova_manga_translator.api.MAX_UPLOAD_BYTES", 16)
    monkeypatch.setattr("nova_manga_translator.api.MAX_MULTIPART_OVERHEAD_BYTES", 0)
    app = create_app(tmp_path, TOKEN, RecordingTranslator())
    with TestClient(app) as client:
        response = submit(client, "too-large", pages=(b"x" * 128,))

    assert response.status_code == 413
    assert not any((tmp_path / "incoming").glob("*.zip"))


def test_archive_is_fully_validated_before_any_page_is_extracted(tmp_path: Path) -> None:
    archive_path = tmp_path / "invalid.zip"
    destination = tmp_path / "extracted"
    with zipfile.ZipFile(archive_path, "w") as archive:
        archive.writestr("000000.jpg", b"valid-first-page")
        archive.writestr("../late-invalid.jpg", b"unsafe")

    from nova_manga_translator.jobs import InvalidChapterArchive, extract_chapter_archive

    try:
        extract_chapter_archive(archive_path, destination)
    except InvalidChapterArchive:
        pass
    else:
        raise AssertionError("invalid archive was accepted")

    assert not destination.exists()


def test_archive_rejects_page_larger_than_limit(tmp_path: Path, monkeypatch) -> None:
    monkeypatch.setattr("nova_manga_translator.jobs.MAX_PAGE_BYTES", 8)
    archive_path = tmp_path / "oversized-page.zip"
    with zipfile.ZipFile(archive_path, "w", zipfile.ZIP_DEFLATED) as archive:
        archive.writestr("000000.jpg", b"x" * 32)

    from nova_manga_translator.jobs import InvalidChapterArchive, extract_chapter_archive

    try:
        extract_chapter_archive(archive_path, tmp_path / "extracted")
    except InvalidChapterArchive:
        pass
    else:
        raise AssertionError("oversized page was accepted")
