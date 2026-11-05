# MangaTranslator Chapter Translation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a single `Traduire le chapitre` reader action that sends an image chapter to a private MangaTranslator server and caches the validated translated result.

**Architecture:** A Python companion service wraps MangaTranslator's stable batch pipeline behind an authenticated asynchronous API. Nova packages page streams, tracks the persistent job through OkHttp, atomically caches the result, and switches the existing reader streams only after complete validation.

**Tech Stack:** Kotlin, Coroutines/StateFlow, OkHttp 5, Java ZIP APIs, Jetpack Compose, Python 3.11, FastAPI, pytest, MangaTranslator batch pipeline, Tailscale Serve.

**Spec:** `docs/superpowers/specs/2026-11-05-manga-translator-chapter-design.md`

## Global Constraints

- Expose only one reader translation command at a time.
- Never store provider API keys in Nova.
- Never modify or replace original downloaded chapter files.
- Keep reader scrolling and chapter navigation responsive during server work.
- Do not build or publish the APK without asking the user first.

## Review Focus

- A chapter containing missing or non-ready pages must not begin upload.
- A malicious ZIP entry such as `../page.jpg` must be rejected before extraction.
- A server restart must preserve queued, running, completed, and failed job metadata.
- A network interruption must preserve the job ID and allow polling to resume.
- A result with missing, duplicate, reordered, or extra pages must not replace reader streams.

---

### Task 1: Versioned MangaTranslator server contract

**Files:**
- Create: `tools/manga-translator-server/nova_manga_translator/api.py`
- Create: `tools/manga-translator-server/nova_manga_translator/jobs.py`
- Create: `tools/manga-translator-server/nova_manga_translator/translator.py`
- Create: `tools/manga-translator-server/tests/test_api.py`
- Create: `tools/manga-translator-server/pyproject.toml`

**Interfaces:**
- Produces: authenticated `/v1/health` and `/v1/chapters` job endpoints.
- Produces: persistent `JobRecord` values with `job_id`, `chapter_key`, `status`, `completed_pages`, `total_pages`, `error`, and `result_path`.

- [ ] Write failing API tests for authentication, chapter-key deduplication, restart recovery, cancellation, progress, and result download.
- [ ] Run `python -m pytest tools/manga-translator-server/tests/test_api.py -q` and verify failures are caused by the missing service.
- [ ] Implement the FastAPI contract and a filesystem-backed job store.
- [ ] Adapt MangaTranslator's `UIConfigState.from_dict`, `map_ui_to_backend_config`, and `batch_translate_images` without depending on Gradio callbacks.
- [ ] Re-run the focused server tests and commit the passing task.

### Task 2: Android preferences and API client

**Files:**
- Create: `app/src/main/java/eu/kanade/tachiyomi/data/translation/MangaTranslationPreferences.kt`
- Create: `app/src/main/java/eu/kanade/tachiyomi/data/translation/MangaTranslationClient.kt`
- Create: `app/src/main/java/eu/kanade/tachiyomi/data/translation/MangaTranslationModels.kt`
- Test: `app/src/test/java/eu/kanade/tachiyomi/data/translation/MangaTranslationClientTest.kt`
- Modify: `app/src/main/java/eu/kanade/tachiyomi/di/AppModule.kt`

**Interfaces:**
- Produces: `suspend fun submitChapter(chapterKey: String, archive: File): TranslationJob`.
- Produces: `suspend fun getJob(jobId: String): TranslationJob`.
- Produces: `suspend fun downloadResult(jobId: String, destination: File): TranslationResultMetadata`.
- Produces: `suspend fun cancelJob(jobId: String)`.

- [ ] Write failing tests for URL normalization, bearer authentication, response parsing, non-JSON errors, timeouts, and token redaction.
- [ ] Run the focused Android unit test and verify the expected failures.
- [ ] Implement the preferences, immutable API models, OkHttp client, and dependency injection.
- [ ] Re-run the focused tests and commit the passing task.

### Task 3: Ordered archive and atomic translated cache

**Files:**
- Create: `app/src/main/java/eu/kanade/tachiyomi/data/translation/MangaTranslationArchive.kt`
- Create: `app/src/main/java/eu/kanade/tachiyomi/data/translation/MangaTranslationCache.kt`
- Test: `app/src/test/java/eu/kanade/tachiyomi/data/translation/MangaTranslationArchiveTest.kt`

**Interfaces:**
- Produces: `suspend fun createUpload(chapterKey: String, pages: List<ReaderPage>): File`.
- Produces: `fun installResult(metadata: TranslationResultMetadata, archive: File): InstalledTranslation`.
- Produces: `fun translatedPage(chapterKey: String, pageIndex: Int): File?`.

- [ ] Write failing tests for deterministic ordering, unreadable pages, ZIP traversal, duplicate entries, page-count mismatch, atomic installation, and stale temporary cleanup.
- [ ] Run the focused archive tests and verify the expected failures.
- [ ] Implement streaming ZIP creation, strict extraction validation, and atomic directory promotion.
- [ ] Re-run the focused tests and commit the passing task.

### Task 4: Resumable chapter translation repository

**Files:**
- Create: `app/src/main/java/eu/kanade/tachiyomi/data/translation/MangaTranslationRepository.kt`
- Create: `app/src/main/java/eu/kanade/tachiyomi/data/translation/MangaTranslationState.kt`
- Test: `app/src/test/java/eu/kanade/tachiyomi/data/translation/MangaTranslationRepositoryTest.kt`

**Interfaces:**
- Produces: `fun observe(chapterKey: String): StateFlow<MangaTranslationState>`.
- Produces: `suspend fun translate(chapterKey: String, pages: List<ReaderPage>)`.
- Produces: `suspend fun resume(chapterKey: String)` and `suspend fun cancel(chapterKey: String)`.

- [ ] Write failing state-machine tests for submit, deduplication, progress, network retry, process recreation, completion, server failure, cancellation, and invalid result.
- [ ] Run the focused repository tests and verify the expected failures.
- [ ] Implement the state machine with persisted job IDs and bounded polling.
- [ ] Re-run the focused tests and commit the passing task.

### Task 5: Reader integration and single command

**Files:**
- Modify: `app/src/main/java/eu/kanade/tachiyomi/ui/reader/ReaderViewModel.kt`
- Modify: `app/src/main/java/eu/kanade/tachiyomi/ui/reader/ReaderActivity.kt`
- Modify: `app/src/main/java/eu/kanade/presentation/reader/appbars/ReaderTopBar.kt`
- Modify: `app/src/main/java/eu/kanade/presentation/reader/appbars/ReaderAppBars.kt`
- Modify: `app/src/main/java/eu/kanade/tachiyomi/ui/reader/model/ReaderPage.kt`
- Create: `app/src/main/java/eu/kanade/presentation/reader/MangaTranslationProgress.kt`
- Test: `app/src/test/java/eu/kanade/presentation/reader/MangaTranslationUiModelTest.kt`
- Modify: `i18n/src/commonMain/moko-resources/base/strings.xml`
- Modify: `i18n/src/commonMain/moko-resources/fr/strings.xml`

**Interfaces:**
- Consumes: `MangaTranslationRepository.observe`, `translate`, and `cancel`.
- Produces: one adaptive overflow action: translate, progress/cancel, show original, or show translation.

- [ ] Write failing pure-model tests for every visible action and content description.
- [ ] Run the focused UI-model test and verify the expected failures.
- [ ] Add reader state, preserve original streams, and swap all page streams only after a validated complete result.
- [ ] Add the single overflow action and accessible progress surface without changing reader gestures or navigation.
- [ ] Re-run the focused tests and commit the passing task.

### Task 6: Configuration UI and operating guide

**Files:**
- Modify: `app/src/main/java/eu/kanade/presentation/more/settings/screen/SettingsReaderScreen.kt`
- Create: `tools/manga-translator-server/README.md`
- Create: `tools/manga-translator-server/start-nova-server.ps1`

**Interfaces:**
- Consumes: `MangaTranslationPreferences`.
- Produces: server URL and token settings plus a connection test.

- [ ] Add a focused test for URL validation and disabled reader action when configuration is incomplete.
- [ ] Add Reader settings with masked token input and a connection test.
- [ ] Document MangaTranslator checkout, Python environment, server startup, Windows firewall behavior, and `tailscale serve --bg 8765`.
- [ ] Run server tests, focused Android tests, Kotlin formatting checks for changed files, and the full available unit-test suite.
- [ ] Review the final diff for secrets, logs, source behavior, original file mutation, reader scroll, and chapter navigation regressions.
- [ ] Ask the user before triggering the APK build and release workflow.
