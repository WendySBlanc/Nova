# Nova Interface Redesign

**Date:** 2026-11-03  
**Status:** Approved by carte blanche  
**Form seed:** `seed:c7ce7aebf2f871`  
**Direction:** Nova Signal

## Intent

Replace Nova's inherited Komikku and Viki presentation layers with one native Android visual system built around two equally important jobs: resume reading immediately and manage a large personal library confidently.

The redesign changes presentation, navigation hierarchy, interaction, and motion. It preserves library data, downloads, backups, extensions, source migration, chapter state, trackers, reader behavior, and compatibility with existing local content.

## Direction Selection

Seven grounded worlds were considered: a Korean serial-release magazine, a library edge index, a comic-shop release wall, a personal reading ledger, a festival programme, a calibrated print-registration rail, and a long-strip webtoon rhythm. The assigned sixth direction became **Nova Signal**.

Nova Signal borrows the precision of print registration and the rhythm of serialized reading without making the app look like a tool or a themed artifact. Covers remain full-color content. The shell is quiet and tactile. Reading progress, unread state, downloads, and navigation align to a persistent off-centre signal rail that changes density and length across screen sizes.

Catalog challengers were weighed against audience identification and product clarity:

- **Airport wayfinding:** competitive on clarity, weaker on reader identity. Kept discipline: every screen exposes only the next useful decisions.
- **Acetate reference manual:** declined. Kept discipline: advanced actions use progressive disclosure and one clear top layer.
- **Character catalog:** declined. Kept discipline: collection cells reflow as whole units and completed content can desaturate without losing labels.
- **Labanotation score:** declined. Kept discipline: progress is spatially consistent and always moves in one direction.
- **Bitmap specimen:** declined. Kept discipline: typography uses intentional scale steps rather than arbitrary size changes.
- **Emission-line rail:** competitive on state legibility, weaker on warmth. Kept discipline: state never depends on hue alone.

## Product Principles

1. **Resume and organize are peers.** The first surface supports both without a separate marketing-style home.
2. **State before action.** Read, unread, in-progress, downloaded, downloading, failed, and new states are visible before a tap.
3. **One action, one home.** Duplicate actions are consolidated; secondary actions live in one contextual menu or sheet.
4. **Offline is first-class.** Local chapters and restored downloads render immediately, before network refreshes.
5. **Content leads.** Covers, titles, chapters, and text occupy the visual hierarchy; chrome stays calm.
6. **Motion explains.** Animation connects source and destination, reveals hierarchy, and confirms state. It never delays reading.

## Information Architecture

### Compact Windows

Use four bottom destinations with 48 dp minimum targets:

- **Bibliotheque:** reading hub, collection, categories, filters, and sort.
- **Decouvrir:** global search, source browsing, extensions, and source web views.
- **Activite:** library updates, download queue, history, and failures.
- **Moi:** backups, storage, appearance, accessibility, reader preferences, and advanced settings.

### Medium And Expanded Windows

Move the same destinations to a navigation rail. The first redesign release keeps title and source details full-screen so the existing Voyager stack remains predictable. List-detail panes become a separate Navigation 3 migration after full-screen parity is verified. The rail responds to current window size and folding features; it is not selected from device-name checks.

The existing navigation engine remains during the visual migration. A Navigation 3 migration is deferred until presentation parity and regression tests are stable.

## Visual System

### Palette

The base is neutral graphite and paper rather than blue-black. It supports dark, light, AMOLED, and optional system-derived themes.

- Primary signal: mineral teal, used for active route and primary reading actions.
- Secondary signal: vermilion, used sparingly for new releases and destructive confirmation.
- Tertiary signal: chartreuse, used for completed downloads and successful synchronization.
- Warning: amber.
- Error: red with icon and text.
- Covers and thumbnails remain unfiltered except where reading progress intentionally desaturates the completed portion.

No large decorative gradients, glass panels, nested cards, or one-hue screens. Color is attached to meaning and interaction.

### Type

Use the Android system type family to avoid loading delay and preserve language coverage. Titles use expressive Material 3 scale steps; dense lists use compact body and label styles. Letter spacing remains zero. Font scaling to 200 percent must preserve actions and reading state without overlap.

### Shape And Space

- 4 dp: progress marks, thumbnails, compact controls.
- 6 dp: repeated collection cells and chapter rows.
- 8 dp: buttons, sheets, menus, and grouped tools.
- 4 dp spacing base; 16 dp compact page margin; 24 dp expanded margin.
- 48 dp minimum touch targets, including icon buttons.

The signal rail is a 1 to 3 dp structural line used for selection, reading progress, and navigation continuity. It must never become decorative noise.

### Iconography

Use the existing Material icon library with filled icons for selected destinations and outlined icons for inactive destinations. Use familiar symbols for search, filter, sort, download, share, web view, and reader controls. Every icon-only control has a localized content description and tooltip where appropriate.

## Motion System

Motion follows the Android system duration scale and reduced-motion preference.

- 90 ms: press, selection, and check feedback.
- 160 ms: menus, compact controls, and chapter-state changes.
- 220 ms: destination fade-through and content replacement.
- Spring, medium-low bounce: signal rail landing and cover-to-detail continuity.
- Shared bounds: cover and title from library to detail where the navigation stack safely supports it.
- Predictive back: previews the library cell or previous pane instead of intercepting the root gesture.

Only transform, alpha, clipping, and color animate in scrolling surfaces. List insertion and sorting preserve item keys and animate placement without blocking input. Skeletons appear only when no cached or local data exists.

## Primary Surfaces

### Bibliotheque

The opening viewport contains:

1. A compact pinned app bar with Nova, global search, update status, and profile access.
2. A **Reprendre** strip showing the current title, next chapter, reading progress, and offline availability. It advances automatically only when idle and stops on touch or accessibility focus.
3. A segmented media switch for Manhwa and Light Novel.
4. Category chips generated from the user's categories, followed by filter and sort controls.
5. An adaptive cover grid. Phones target three columns where content remains legible; larger screens use `GridCells.Adaptive` with stable cover proportions.

Each cell exposes cover, title, remaining chapter count, latest meaningful state, and download availability. Read covers are desaturated. In-progress desaturation follows completed page percentage, while a compact text or icon state remains available for accessibility.

Pull-to-refresh stays available. Cached and local data remains visible during refresh. Refresh never replaces the whole screen with loading UI.

### Title Detail

A collapsing cover header connects the library cell to the title. It contains cover, title, source, concise metadata, and one primary action:

- **Lire** starts the first unread chapter.
- **Reprendre** resumes current progress.
- **Relire** appears when all chapters are read.

Source and title remain searchable/clickable. Chapters lead the page immediately below the header. A sticky chapter toolbar contains search, filter, sort, and one overflow menu. Web view, share, tracking, migration, download all, mark read, mark unread, and related actions appear exactly once.

Rows show thumbnail when available, chapter title, date/scanlator, reading progress, downloaded state, and failure state. Long press opens contextual multi-selection with mark read, mark previous read, mark unread, download, delete download, and share when supported.

Local chapter state renders first. Network updates merge through the existing chapter and download recovery contracts so restored downloads remain recognized. Cross-source URL aliasing is domain work and must be designed and tested separately rather than hidden inside this presentation migration.

### Decouvrir

One persistent search field covers sources and installed extensions. The screen has tabs for catalogues, extensions, and repositories, with filters scoped to the active tab. Installed, update available, disabled, and incompatible states use explicit labels and icons.

Bulk install and uninstall use selection mode with a persistent action bar and confirmation summary. Source web view remains available from catalogue and title surfaces.

### Activite

Updates, downloads, and history share one destination with clear sub-tabs. The download queue shows chapter, title, source, progress, speed, retry reason, pause/resume, reorder, and bulk controls. Failed items stay actionable and never disappear behind a generic snackbar.

### Moi

Use grouped full-width settings rows instead of nested cards. Lead with identity-neutral local status: storage folder, last backup, synchronization/update status, and app version. Appearance, readers, downloads, library, extensions, backup, accessibility, privacy, and advanced settings each have one route and no duplicate entry points.

## Readers

### Manhwa Reader

Reading content stays edge-to-edge. A single tap toggles controls. The top overlay contains back, title/chapter, and overflow. The bottom overlay contains previous, chapter selector, next, page/progress slider, orientation/reading mode, and settings.

Only one visible scrollbar exists. For vertical reading it is horizontal in the control overlay and reflects the active chapter's true page/scroll progress. Crossing the final threshold reveals a deliberate next-chapter affordance; it never silently jumps two chapters or restores a stale position in the next chapter.

Chapter transition preloads adjacent metadata and the first pages, preserves exact history, and uses a short directional fade-through. Controls remain hidden when the reader is opened unless accessibility services require focusable chrome.

### Light Novel Reader

The text reader keeps the LNReader-derived capabilities and presents them through three layers:

- Tap overlay: back, chapter, previous/next, progress, play/pause TTS, and settings.
- Reading sheet: typography, width, alignment, line height, paragraph spacing, theme, and brightness.
- Advanced sheet: custom colors, CSS, JavaScript, TTS queue/state, voice, rate, pitch, sleep timer, and background playback.

TTS uses Media3 notification controls with play/pause, stop, previous, next, replay, seek where meaningful, queue state, progress, and background resume. Durable background resume requires service-owned session state and is delivered as an isolated reader phase after the new text-reader controls are stable. Image-only controls are hidden for text chapters.

## Loading And Error States

- Render database and local filesystem data immediately.
- Refresh network data in the background and merge changes without resetting scroll or selection.
- Use inline retry for one failed section; reserve full-screen errors for screens with no usable local state.
- Parse source errors defensively and show a localized message rather than leaking JSON decoding exceptions.
- Distinguish empty library, empty filter, unavailable source, no local chapter, and network failure.
- Never show indefinite buffering without a reason, timeout, and action.

## Accessibility

- TalkBack order follows visual reading order and announces read/download progress.
- States use icon, text, semantics, or pattern in addition to color.
- All gestures have button or menu equivalents.
- Dynamic text, landscape, foldables, tablets, and display scaling are required test matrices.
- Motion responds to the system animator scale; automatic carousel movement pauses for touch, focus, and reduced motion.
- Contrast meets WCAG AA for text and essential controls.

## Performance Contract

- Show cached title and chapter data in under one second on a warm local database.
- Keep scrolling surfaces free of synchronous filesystem and network work.
- Use stable keys, immutable UI models, derived state, and content-type hints for lazy containers.
- Decode covers to requested display size and preserve higher-quality originals in disk cache.
- Preload only the next useful content: adjacent chapter metadata/pages and near-viewport covers.
- Avoid broad recomposition from download or progress updates; update the affected item only.

## Migration Plan

1. Add screenshot and Compose regression coverage for the current library, detail, navigation, and reader overlays.
2. Introduce Nova Signal tokens, shared components, semantics, and motion primitives without changing data flows.
3. Replace the main navigation shell and add the Activity destination.
4. Replace the library and title detail screens, then remove their bypassed Viki duplicates.
5. Unify Discover, extension management, and source web view presentation.
6. Replace Moi while preserving every preference route.
7. Replace manhwa and light-novel reader chrome without replacing proven reader engines.
8. Validate phone, landscape phone, foldable, and tablet captures; profile startup, library scroll, and chapter transition.
9. Remove dead presentation code only after parity tests pass.

## Non-Goals

- No source, database, download-format, backup-format, or extension-API migration is part of the first visual pass.
- No proprietary Viki or third-party assets are copied.
- Navigation 3 is not introduced in the same change as the visual shell.
- Reader engines are not rewritten when only their controls and interaction hierarchy need replacement.

## Acceptance Criteria

- Every existing user capability is reachable exactly once from a logical surface.
- Library state is understandable without opening a title.
- Local and restored downloads appear before network refresh and retain their indicators.
- Both readers expose their complete controls without duplicate overlays.
- Navigation adapts between bottom bar and rail without losing destination state.
- No clipped text or overlapping controls at supported window sizes and font scales.
- Motion remains responsive and can be reduced through Android system settings.
- Phone, landscape, foldable, and tablet screenshots pass visual and accessibility review.
