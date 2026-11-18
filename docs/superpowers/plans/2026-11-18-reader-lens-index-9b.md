# Reader Lens Index 9B Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace Reader Lens' per-bubble ML Kit translation with contextual, offline Index-Translate 9B inference and natural French overlays.

**Architecture:** Keep ML Kit only for OCR and language detection. Submit every page as one ordered structured request to an isolated translator, download the pinned GGUF model through a resumable verified model store, and run it through a prebuilt llama.cpp Android AAR. Keep the current queue, overlay, and cache, but version cache entries by model and prompt schema.

**Tech Stack:** Kotlin, coroutines/StateFlow, OkHttp, kotlinx.serialization, ML Kit OCR, llama.cpp Android AAR, JUnit/Kotest.

**Spec:** `docs/superpowers/specs/2026-11-18-reader-lens-index-translate-design.md`

## Global Constraints

- Translation is fully local after one model download; no cloud service or API key.
- ML Kit remains OCR/language detection only and is never a translation fallback.
- Model: `Index-Translate-9B.Q3_K_M.gguf`, 4,737,609,152 bytes, SHA-256 `7144b4aeb76febd2d6295bbade0f8fa5eb047bb20c11f94bc03ad5fa9a5071c0`.
- Supported runtime: `arm64-v8a`; incompatible devices keep the reader functional and report Lens as unavailable.
- Never bundle the model in Git, CI artifacts, or the APK.
- Do not build an APK until the user explicitly approves the build.

## Review Focus

- A malformed or partial model response must never map text to the wrong bubble; Task 1 tests missing, duplicate, unknown, and reordered IDs.
- An interrupted 4.41 GiB download must resume without duplicating bytes; Task 2 tests HTTP 206 and HTTP 200 restart behavior.
- A checksum or disk-space failure must leave the old/original reader usable; Task 2 tests failed validation and unchanged final file state.
- Native loading on non-arm64 devices must not trigger an `UnsatisfiedLinkError`; Task 3 tests compatibility before runtime construction.
- Cancellation during visible-page changes must not publish stale overlays; Task 4 retains queue cancellation tests and tests one page-level call.

---

### Task 1: Contextual Translation Protocol

**Files:**
- Create: `app/src/main/java/eu/kanade/tachiyomi/data/lens/ReaderLensTranslator.kt`
- Create: `app/src/test/java/eu/kanade/tachiyomi/data/lens/ReaderLensTranslatorTest.kt`

**Interfaces:**
- Produces: `ReaderLensTranslator.translatePage(blocks, sourceLanguage, previousContext)`, `buildIndexTranslationPrompt(...)`, and `parseIndexTranslation(...)`.

- [x] Write failing tests for ordered IDs, natural-style examples, strict structured parsing, and rejection of missing, duplicate, unknown, or blank results.
- [x] Run `./gradlew :app:testDebugUnitTest --tests eu.kanade.tachiyomi.data.lens.ReaderLensTranslatorTest` and confirm RED.
- [x] Implement the translator protocol and parser with a serialized list of `{id,text}` objects.
- [x] Run the focused test and confirm GREEN.

### Task 2: Resumable Verified Model Store

**Files:**
- Create: `app/src/main/java/eu/kanade/tachiyomi/data/lens/ReaderLensModelStore.kt`
- Create: `app/src/test/java/eu/kanade/tachiyomi/data/lens/ReaderLensModelStoreTest.kt`

**Interfaces:**
- Produces: `ReaderLensModelState`, `ReaderLensModelStore.state`, `ensureModel(): File`, and pinned model metadata constants.

- [ ] Write failing MockWebServer tests for fresh download, resume, server range refusal, cancellation-preserved partial file, disk-space failure, checksum rejection, and atomic promotion.
- [ ] Run the focused model-store test and confirm RED.
- [ ] Implement an unlimited-call-time OkHttp download, progress StateFlow, size/hash validation, and same-directory promotion.
- [ ] Run the focused test and confirm GREEN.

### Task 3: llama.cpp Runtime Adapter

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`
- Create: `app/src/main/java/eu/kanade/tachiyomi/data/lens/ReaderLensInferenceRuntime.kt`
- Create: `app/src/test/java/eu/kanade/tachiyomi/data/lens/ReaderLensInferenceRuntimeTest.kt`

**Interfaces:**
- Consumes: model file from Task 2.
- Produces: `ReaderLensInferenceRuntime.complete(prompt)`, deterministic `LlamaReaderLensInferenceRuntime`, and `isReaderLensRuntimeSupported(abis)`.

- [ ] Write failing tests proving arm64-only gating, deterministic configuration, lazy single load, serialized inference, and release.
- [ ] Run the focused runtime test and confirm RED.
- [ ] Add `dev.ffmpegkit-maintained:llama-android:0.1.1` and implement the isolated adapter with context 2048, temperature 0, CPU-only inference, and bounded threads.
- [ ] Run the focused test and confirm GREEN.

### Task 4: Lens Engine and Cache Integration

**Files:**
- Modify: `app/src/main/java/eu/kanade/tachiyomi/data/lens/ReaderLensEngine.kt`
- Modify: `app/src/main/java/eu/kanade/tachiyomi/data/lens/ReaderLensCache.kt`
- Modify: `app/src/main/java/eu/kanade/tachiyomi/di/AppModule.kt`
- Modify: `app/src/test/java/eu/kanade/tachiyomi/data/lens/ReaderLensCacheTest.kt`
- Create: `app/src/test/java/eu/kanade/tachiyomi/data/lens/ReaderLensEngineTranslationTest.kt`

**Interfaces:**
- Consumes: translator/runtime/store from Tasks 1-3.
- Produces: one contextual translation call per page and model-aware cache identities.

- [ ] Write failing tests proving one call per page, exact coordinate mapping, French-page bypass, malformed-output errors, and model/prompt cache invalidation.
- [ ] Run focused Lens tests and confirm RED.
- [ ] Remove ML Kit Translation dependencies and per-bubble translation, inject the Index translator, and version the cache with model digest plus prompt schema.
- [ ] Run all Lens tests and confirm GREEN.

### Task 5: Reader Model Status and Accessibility

**Files:**
- Modify: `app/src/main/java/eu/kanade/tachiyomi/ui/reader/ReaderViewModel.kt`
- Modify: `app/src/main/java/eu/kanade/tachiyomi/ui/reader/ReaderActivity.kt`
- Modify: `app/src/main/java/eu/kanade/presentation/reader/appbars/ReaderAppBars.kt`
- Modify: `app/src/main/java/eu/kanade/presentation/reader/appbars/ReaderBottomBar.kt`
- Modify: `i18n/src/commonMain/moko-resources/base/strings.xml`
- Modify: `i18n/src/commonMain/moko-resources/fr/strings.xml`

**Interfaces:**
- Consumes: `ReaderLensModelStore.state`.
- Produces: visible download progress and accessible ready/downloading/failed/incompatible state on the existing Lens control.

- [ ] Write or extend UI policy tests for one Lens control, progress semantics, and unsupported-device behavior.
- [ ] Run the focused UI tests and confirm RED.
- [ ] Collect model state in the reader and update the existing control without adding a duplicate translation option.
- [ ] Run focused UI tests and confirm GREEN.

### Task 6: Verification and Documentation

**Files:**
- Modify: `docs/superpowers/specs/2026-11-18-reader-lens-index-translate-design.md`

- [ ] Run all Lens and reader unit tests.
- [ ] Run `./gradlew :app:testDebugUnitTest` and report any pre-existing failures by name.
- [ ] Run formatting/static checks available for touched Kotlin files.
- [ ] Review the final diff for model binaries, secrets, duplicate UI options, and remaining ML Kit Translation calls.
- [ ] Ask the user before any APK build.
