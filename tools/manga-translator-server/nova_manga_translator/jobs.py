from __future__ import annotations

import json
import os
import re
import shutil
import threading
import uuid
import zipfile
from concurrent.futures import Future, ThreadPoolExecutor
from dataclasses import asdict, dataclass
from datetime import datetime, timezone
from pathlib import Path, PurePosixPath

from .translator import ChapterTranslator, TranslationCancelled, TranslationConfigSnapshot


PAGE_NAME = re.compile(r"^(\d{6})\.(?:jpe?g|png|webp)$", re.IGNORECASE)
TRANSLATED_PAGE_NAME = re.compile(
    r"^(\d{6})(?:_translated)?\.(?:jpe?g|png|webp)$",
    re.IGNORECASE,
)
ACTIVE_STATUSES = {"queued", "running"}
MAX_PAGE_COUNT = 5_000
MAX_PAGE_BYTES = 100 * 1024 * 1024
MAX_ARCHIVE_BYTES = 2 * 1024 * 1024 * 1024
COPY_BUFFER_BYTES = 1024 * 1024


@dataclass
class JobRecord:
    job_id: str
    chapter_key: str
    status: str
    completed_pages: int
    total_pages: int
    error: str | None
    result_path: str | None
    input_dir: str
    output_dir: str
    created_at: str
    target_language: str | None = None
    config_revision: str | None = None
    config_snapshot_path: str | None = None

    def public_dict(self, *, cached: bool = False) -> dict:
        return {
            "job_id": self.job_id,
            "chapter_key": self.chapter_key,
            "status": self.status,
            "completed_pages": self.completed_pages,
            "total_pages": self.total_pages,
            "error": self.error,
            "target_language": self.target_language,
            "config_revision": self.config_revision,
            "cached": cached,
        }


class InvalidChapterArchive(ValueError):
    pass


class JobStore:
    def __init__(self, data_dir: Path) -> None:
        self.data_dir = Path(data_dir).resolve()
        self.jobs_dir = self.data_dir / "jobs"
        self.jobs_dir.mkdir(parents=True, exist_ok=True)
        self._lock = threading.RLock()

    def save(self, record: JobRecord) -> None:
        destination = self.jobs_dir / f"{record.job_id}.json"
        temporary = destination.with_suffix(".json.tmp")
        payload = json.dumps(asdict(record), ensure_ascii=False, indent=2)
        with self._lock:
            temporary.write_text(payload, encoding="utf-8")
            os.replace(temporary, destination)

    def get(self, job_id: str) -> JobRecord | None:
        path = self.jobs_dir / f"{job_id}.json"
        with self._lock:
            if not path.is_file():
                return None
            return JobRecord(**json.loads(path.read_text(encoding="utf-8")))

    def all(self) -> list[JobRecord]:
        with self._lock:
            records = []
            for path in sorted(self.jobs_dir.glob("*.json")):
                records.append(JobRecord(**json.loads(path.read_text(encoding="utf-8"))))
            return records

    def find_by_chapter(self, chapter_key: str, config_revision: str | None = None) -> JobRecord | None:
        matches = [
            record
            for record in self.all()
            if record.chapter_key == chapter_key
            and (config_revision is None or record.config_revision == config_revision)
        ]
        return max(matches, key=lambda item: item.created_at) if matches else None


def extract_chapter_archive(archive_path: Path, destination: Path) -> list[Path]:
    temporary = destination.with_name(f".{destination.name}.{uuid.uuid4().hex}.tmp")
    try:
        with zipfile.ZipFile(archive_path) as archive:
            entries = archive.infolist()
            if not entries or len(entries) > MAX_PAGE_COUNT:
                raise InvalidChapterArchive("Archive page count is outside the supported range")

            validated: list[tuple[zipfile.ZipInfo, str]] = []
            declared_total = 0
            for expected_index, entry in enumerate(entries):
                name = entry.filename.replace("\\", "/")
                pure_name = PurePosixPath(name)
                if entry.is_dir() or pure_name.name != name or entry.flag_bits & 0x1:
                    raise InvalidChapterArchive("Archive contains an unsafe or duplicate entry")
                match = PAGE_NAME.fullmatch(name)
                if match is None:
                    raise InvalidChapterArchive("Archive contains an invalid page name")
                if int(match.group(1)) != expected_index:
                    raise InvalidChapterArchive("Archive pages must be ordered and start at zero")
                if entry.file_size <= 0 or entry.file_size > MAX_PAGE_BYTES:
                    raise InvalidChapterArchive("Archive contains an oversized or empty page")
                declared_total += entry.file_size
                if declared_total > MAX_ARCHIVE_BYTES:
                    raise InvalidChapterArchive("Archive is too large after extraction")
                validated.append((entry, name))

            temporary.mkdir(parents=True, exist_ok=False)
            actual_total = 0
            for entry, name in validated:
                target = temporary / name
                page_bytes = 0
                with archive.open(entry) as source, target.open("wb") as output:
                    while chunk := source.read(COPY_BUFFER_BYTES):
                        page_bytes += len(chunk)
                        actual_total += len(chunk)
                        if page_bytes > MAX_PAGE_BYTES or actual_total > MAX_ARCHIVE_BYTES:
                            raise InvalidChapterArchive("Archive is too large after extraction")
                        output.write(chunk)
                if page_bytes != entry.file_size:
                    raise InvalidChapterArchive("Archive page size does not match its metadata")
    except zipfile.BadZipFile as error:
        shutil.rmtree(temporary, ignore_errors=True)
        raise InvalidChapterArchive("Archive is not a valid ZIP file") from error
    except Exception:
        shutil.rmtree(temporary, ignore_errors=True)
        raise

    destination.parent.mkdir(parents=True, exist_ok=True)
    os.replace(temporary, destination)
    return [destination / name for _, name in validated]


class JobManager:
    def __init__(
        self,
        data_dir: Path,
        translator: ChapterTranslator,
        *,
        start_workers: bool = True,
    ) -> None:
        self.data_dir = Path(data_dir).resolve()
        self.data_dir.mkdir(parents=True, exist_ok=True)
        self.store = JobStore(self.data_dir)
        self.translator = translator
        self.start_workers = start_workers
        self.executor = ThreadPoolExecutor(max_workers=1, thread_name_prefix="nova-translation")
        self._lock = threading.RLock()
        self._futures: dict[str, Future] = {}
        self._cancelled: dict[str, threading.Event] = {}

    def snapshot_configuration(self) -> TranslationConfigSnapshot:
        return self.translator.snapshot_configuration()

    def find_by_chapter(self, chapter_key: str, config_revision: str) -> JobRecord | None:
        return self.store.find_by_chapter(chapter_key, config_revision)

    def submit(
        self,
        chapter_key: str,
        archive_path: Path,
        configuration: TranslationConfigSnapshot,
    ) -> tuple[JobRecord, bool]:
        with self._lock:
            existing = self.store.find_by_chapter(chapter_key, configuration.config_revision)
            if existing is not None and existing.status not in {"failed", "cancelled"}:
                archive_path.unlink(missing_ok=True)
                return existing, True

            job_id = uuid.uuid4().hex
            job_dir = self.data_dir / "work" / job_id
            input_dir = job_dir / "input"
            output_dir = job_dir / "output"
            config_snapshot_path = job_dir / "config.snapshot"
            try:
                pages = extract_chapter_archive(archive_path, input_dir)
                if configuration.payload is not None:
                    config_snapshot_path.write_bytes(configuration.payload)
            except Exception:
                shutil.rmtree(job_dir, ignore_errors=True)
                raise
            finally:
                archive_path.unlink(missing_ok=True)
            output_dir.mkdir(parents=True, exist_ok=True)
            record = JobRecord(
                job_id=job_id,
                chapter_key=chapter_key,
                status="queued",
                completed_pages=0,
                total_pages=len(pages),
                error=None,
                result_path=None,
                input_dir=str(input_dir),
                output_dir=str(output_dir),
                created_at=datetime.now(timezone.utc).isoformat(),
                config_revision=configuration.config_revision,
                config_snapshot_path=str(config_snapshot_path) if configuration.payload is not None else None,
            )
            self.store.save(record)
            if self.start_workers:
                self._schedule(record)
            return record, False

    def resume_pending(self) -> None:
        if not self.start_workers:
            return
        for record in self.store.all():
            if record.status in ACTIVE_STATUSES:
                record.status = "queued"
                record.error = None
                self.store.save(record)
                self._schedule(record)

    def get(self, job_id: str) -> JobRecord | None:
        return self.store.get(job_id)

    def cancel(self, job_id: str) -> JobRecord | None:
        with self._lock:
            record = self.store.get(job_id)
            if record is None:
                return None
            if record.status not in ACTIVE_STATUSES:
                return record
            event = self._cancelled.setdefault(job_id, threading.Event())
            event.set()
            future = self._futures.get(job_id)
            if record.status == "queued" and future is not None and future.cancel():
                record.status = "cancelled"
                record.error = None
                self.store.save(record)
            return self.store.get(job_id)

    def close(self) -> None:
        self.executor.shutdown(wait=True, cancel_futures=False)

    def _schedule(self, record: JobRecord) -> None:
        with self._lock:
            future = self._futures.get(record.job_id)
            if future is not None and not future.done():
                return
            event = self._cancelled.setdefault(record.job_id, threading.Event())
            self._futures[record.job_id] = self.executor.submit(self._run, record.job_id, event)

    def _run(self, job_id: str, cancelled: threading.Event) -> None:
        record = self.store.get(job_id)
        if record is None:
            return
        if cancelled.is_set():
            record.status = "cancelled"
            self.store.save(record)
            return

        record.status = "running"
        record.error = None
        self.store.save(record)

        def progress(completed: int, total: int) -> None:
            current = self.store.get(job_id)
            if current is None:
                return
            current.completed_pages = min(max(0, completed), current.total_pages)
            if total != current.total_pages:
                raise RuntimeError("MangaTranslator reported a mismatched page count")
            self.store.save(current)

        try:
            configuration = self._configuration_for(record)
            outcome = self.translator.translate(
                Path(record.input_dir),
                Path(record.output_dir),
                progress,
                cancelled,
                configuration,
            )
            if cancelled.is_set():
                raise TranslationCancelled()
            if outcome.config_revision != configuration.config_revision:
                raise RuntimeError("MangaTranslator used a different configuration revision")
            result_path = self._package_result(record)
            record = self.store.get(job_id) or record
            record.status = "complete"
            record.completed_pages = record.total_pages
            record.result_path = str(result_path)
            record.target_language = outcome.target_language
            record.config_revision = outcome.config_revision
            record.error = None
            self.store.save(record)
        except TranslationCancelled:
            record = self.store.get(job_id) or record
            record.status = "cancelled"
            record.error = None
            self.store.save(record)
        except Exception as error:
            record = self.store.get(job_id) or record
            record.status = "failed"
            record.error = str(error) or type(error).__name__
            self.store.save(record)

    def _package_result(self, record: JobRecord) -> Path:
        input_pages = sorted(path for path in Path(record.input_dir).iterdir() if path.is_file())
        output_by_stem: dict[str, Path] = {}
        for path in Path(record.output_dir).iterdir():
            match = TRANSLATED_PAGE_NAME.fullmatch(path.name) if path.is_file() else None
            if match is None:
                continue
            stem = match.group(1)
            if stem in output_by_stem:
                raise RuntimeError("MangaTranslator produced duplicate translated pages")
            output_by_stem[stem] = path

        expected_stems = [PAGE_NAME.fullmatch(path.name).group(1) for path in input_pages]
        if set(output_by_stem) != set(expected_stems):
            raise RuntimeError("Translated output does not match the input page set")

        destination = Path(record.output_dir).parent / "translated.zip"
        temporary = destination.with_suffix(".zip.tmp")
        with zipfile.ZipFile(temporary, "w", zipfile.ZIP_DEFLATED) as archive:
            for stem in expected_stems:
                page = output_by_stem[stem]
                archive.write(page, arcname=f"{stem}{page.suffix.lower()}")
        os.replace(temporary, destination)
        return destination

    def _configuration_for(self, record: JobRecord) -> TranslationConfigSnapshot:
        if record.config_revision is None:
            configuration = self.snapshot_configuration()
            record.config_revision = configuration.config_revision
            self.store.save(record)
            return configuration
        payload = None
        if record.config_snapshot_path is not None:
            snapshot_path = Path(record.config_snapshot_path)
            if not snapshot_path.is_file():
                raise RuntimeError("Translation configuration snapshot is missing")
            payload = snapshot_path.read_bytes()
        return TranslationConfigSnapshot(record.config_revision, payload)
