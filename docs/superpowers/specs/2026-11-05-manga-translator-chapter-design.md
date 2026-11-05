# MangaTranslator Chapter Translation

## Intent

Nova can send the current image chapter to MangaTranslator running on the user's own PC or server. The reader exposes one translation command, `Traduire le chapitre`, and stays responsive while the server processes every page.

## User flow

1. The user configures the private MangaTranslator server URL and access token in Reader settings.
2. The reader overflow menu exposes `Traduire le chapitre` for image chapters.
3. Nova uploads the chapter once as an ordered ZIP archive and receives a persistent job identifier.
4. The server processes the complete chapter asynchronously and reports page progress.
5. Nova downloads the translated archive, validates it, stores it in the Nova data directory, and refreshes the current chapter in place.
6. Once translated, the same menu entry becomes `Afficher l'original`; switching back changes it to `Afficher la traduction`.

No page-level translation command is added. Light-novel chapters keep their existing reader and are not sent to MangaTranslator.

## Server companion

The companion service lives in `tools/manga-translator-server/` and wraps an existing MangaTranslator checkout rather than copying its models into Nova. It loads MangaTranslator's saved `config.json`, so provider credentials, target language, OCR, cleaning, inpainting, and rendering remain configured only on the PC.

The authenticated API contract is:

- `GET /v1/health`
- `POST /v1/chapters` with an ordered ZIP and a deterministic chapter key
- `GET /v1/chapters/{jobId}` for queued, running, complete, failed, or cancelled state
- `GET /v1/chapters/{jobId}/result` for the translated ZIP
- `DELETE /v1/chapters/{jobId}` to cancel work

Jobs and completed archives survive a service restart. A repeated chapter key returns the existing job or cached result instead of translating twice.

## Private connectivity

MangaTranslator listens only on `127.0.0.1`. Tailscale Serve exposes it through a private HTTPS address available to the user's tailnet. Nova never requires a publicly reachable port. The PC must be online for new translations; cached translated chapters remain readable offline.

## Android architecture

- `MangaTranslationPreferences` stores the server URL and token in Nova's preference store.
- `MangaTranslationClient` owns the small OkHttp API contract and does not use source interceptors or source cookies.
- `MangaTranslationArchive` creates ordered uploads, rejects unsafe ZIP paths, validates that every expected page exists, and atomically promotes complete results into the chapter cache.
- `MangaTranslationRepository` deduplicates requests by chapter key and exposes a `StateFlow` for reader progress.
- `ReaderViewModel` starts or resumes translation and swaps each `ReaderPage.stream` only after the complete archive is validated. Original stream providers remain available for instant switching.

The cache key includes the source ID, manga ID, chapter ID, page count, target language reported by the server, and a server configuration revision. A configuration change therefore cannot silently reuse stale output.

## UI and accessibility

- The action is placed in the existing reader overflow menu; no extra toolbar icon is added.
- While active, the menu row reports `Traduction 4/20` and can cancel the job.
- A compact progress surface appears only while reader controls are visible.
- Success, failure, cancellation, and server-unreachable states have spoken semantics and concise French messages.
- The reader remains scrollable and chapter navigation remains available during translation.

## Failure behavior

- A network interruption preserves the server job ID and resumes polling when connectivity returns.
- Closing Nova never cancels server work.
- A partial, corrupt, mismatched, or path-traversing ZIP is discarded without replacing reader pages.
- If any page fails on the server, Nova keeps the original chapter and displays the failed page count.
- Server tokens and MangaTranslator provider keys are never written to logs.

## Non-goals

- No public cloud hosting.
- No translation model inside the APK.
- No automatic translation without an explicit user action.
- No page-by-page overlay or live translation during scroll.
- No modification of downloaded original chapter files.

## Approaches considered

- A public GPU service would work everywhere but violates the free, self-hosted requirement.
- Calling the Gradio UI directly is fragile because its generated callback contract changes with MangaTranslator's interface.
- A small versioned API around MangaTranslator's batch pipeline is selected because it is private, resumable, testable, and independent of the Gradio interface.
