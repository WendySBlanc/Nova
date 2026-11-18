# Reader Lens: contextual English-to-French translation

## Intent

Replace Reader Lens' final per-bubble ML Kit translation with a local, contextual English-to-French translator. The overlay must remain usable while scrolling, preserve every bubble position, work offline after one model download, and avoid re-translating pages that have already been processed.

## Current state

`ReaderLensEngine` detects text with ML Kit OCR, groups nearby lines into bubbles, then translates each bubble independently with ML Kit Translation. This loses dialogue context and produces literal or inconsistent French. The reader already queues the visible and next pages, caches regions by image fingerprint, and renders translated text without modifying the source image.

## Approaches

1. **TranslatePsy-EuroNano Base (42 MB):** fastest and smallest, but its published English-to-European COMET score is below Firefox/Bergamot and NLLB. Rejected because quality is the main requirement.
2. **TranslateGemma 4B:** strong contextual translation, but its multi-gigabyte footprint and slower mobile inference make continuous reader prefetch less reliable.
3. **Index-Translate-2B Q4_K_M:** compact, but local tests exposed grammatical errors in French dialogue, including incorrect object-pronoun placement. Rejected because natural French is the primary requirement.
4. **Index-Translate-9B Q3_K_M (selected):** 4.41 GiB GGUF, Apache-2.0, designed for contextual, stylistic, structured and community-language translation. Local deterministic tests preserved every bubble identifier and produced the most natural French of the tested models. The measured desktop peak working set was 5.42 GiB.

## Design

### Translation boundary

Introduce a `ReaderLensTranslator` interface so OCR, model management and translation can be tested independently. `ReaderLensEngine` will continue producing ordered `ReaderLensTextBlock` instances with image coordinates. It will submit all bubbles from a page as one structured request, with stable identifiers such as `b0`, `b1` and `b2`.

The Index prompt will use the model's constrained-translation format with deterministic decoding (`temperature = 0`, thinking disabled). It will request natural French dialogue while preserving names, punctuation intent, bubble identifiers and output order. Short style examples will teach idiomatic restructuring of passive English narration without a second inference pass. The response parser must reject missing, duplicated or unknown identifiers instead of placing text in the wrong bubble. A failed structured response leaves the original page visible and exposes a retryable Lens error; it never invokes another translation engine.

Previous-page source text may be supplied as read-only background context when it is already cached. Its absence must never delay the visible page.

The Android runtime supplies a dedicated system prompt that keeps reasoning enabled but asks for a very brief trace and JSON-only output. A real Index-Translate 9B Q3_K_M smoke test on the supplied training-room bubble returned a complete JSON translation in about eight seconds on the development PC. Prompts are capped at 2,500 UTF-8 bytes, include at most 300 bytes of rolling context, and split abnormal OCR blocks into Unicode-safe 1,000-byte segments before reassembling them under the original bubble identifier.

### Runtime and compatibility

Add a small Android JNI module based on a pinned `llama.cpp` revision and the official Android binding patterns. Only one model instance and one inference operation may be active at a time. Native initialization and generation run outside the main thread and preserve the existing cancellation gate.

The local Index engine is enabled on supported `arm64-v8a` devices with Android 9 or newer and sufficient available memory. On older or incompatible devices, Lens translation is unavailable and explains the compatibility requirement without affecting the reader. Nova's general minimum Android version does not change.

### Model lifecycle

The 4.41 GiB `Index-Translate-9B.Q3_K_M.gguf` file is not bundled in the APK. The first Lens activation starts an explicit, cancellable download into app-managed external model storage. Downloading is resumable, uses a temporary file, verifies the exact 4,737,609,152-byte size and SHA-256 digest `7144b4aeb76febd2d6295bbade0f8fa5eb047bb20c11f94bc03ad5fa9a5071c0`, and promotes the file atomically only after validation.

Reader Lens exposes model states (`not installed`, `downloading`, `ready`, `failed`) through its existing control instead of adding a second translation feature. A failed or interrupted download never prevents the reader from opening.

### Cache and scrolling

Keep the visible-page-first queue and next-page prefetch. Cache entries will include the image fingerprint, target language, model identifier, model digest and prompt schema version. Increment the cache version so existing ML Kit results cannot be mistaken for Index results.

Cached overlays render immediately. New translations are computed once, stored atomically, and reused across reader sessions. Model loading remains warm while Lens is enabled and is released when the reader closes or Android reports critical memory pressure.

### OCR scope

This migration keeps the existing ML Kit OCR and coordinates. Replacing OCR with PP-OCRv6 is a separate change because OCR affects tiling, bounding boxes and script detection, while the reported problem is translation quality. The translator boundary will allow a later OCR replacement without changing Index integration.

## Reliability and tests

- Unit tests for ordered page requests, strict identifier parsing, retryable malformed-output errors and natural-style prompt construction.
- Unit tests for model state transitions, resumable download metadata, checksum failure and atomic promotion.
- Cache tests covering model and prompt version invalidation.
- Reader queue tests proving visible-page priority, next-page prefetch and cancellation.
- Existing Lens overlay and reader tests remain mandatory.
- An optional device smoke test uses a separately supplied GGUF file; the 4.41 GiB model is never stored in Git or CI artifacts.

## Success criteria

- One contextual translation request for a normal page instead of one request per bubble. Oversized pages are split into bounded batches with global bubble identifiers and a short rolling context.
- Every returned translation is mapped to the correct original bubble.
- Cached pages display without network access or repeated inference.
- Reader opening, scrolling and chapter navigation remain available while the model downloads or fails.
- English manhwa dialogue reads naturally in French and keeps terminology consistent across the page.

## Non-goals

- No cloud translation service or personal API key.
- No translated image copies and no replacement of downloaded chapter files.
- No OCR engine migration in this change.
- No automatic APK build or release as part of the migration itself.
