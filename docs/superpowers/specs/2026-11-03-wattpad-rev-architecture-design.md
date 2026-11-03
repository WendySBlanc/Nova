# Nova Wattpad-Inspired Rev Architecture

**Date:** 2026-11-03  
**Status:** Approved on 2026-11-03  
**Chosen approach:** Wattpad-inspired information architecture adapted to Nova  
**Visual and motion system:** Nova Signal tokens with the Rev Card Motion skill

## Intent

Reorganize the complete Nova interface around the clear reading journeys used by
modern story applications: discover something, save it, resume it, manage it,
and see new activity without mixing those jobs on one screen.

Wattpad is a reference for information architecture and interaction hierarchy,
not a source of proprietary code, graphics, copy, or brand assets. Nova remains
a native Kotlin and Jetpack Compose application for manhwa and light novels.

This design supersedes the four-destination information architecture and the
"no separate home" decision in
`2026-11-03-nova-interface-redesign-design.md`. The Nova Signal color,
typography, spacing, accessibility, local-first, and reader principles remain
in force unless this document replaces them explicitly.

## User Decision

The user selected a full application reorganization rather than limiting the
change to the library, title detail, or reader flow. The selected approach is a
functional adaptation: Wattpad-like routes and hierarchy with Nova-specific
jobs in every destination.

## Goals

- Give Home, discovery, library management, activity, and personal settings
  separate, predictable destinations.
- Put resume reading and recent releases on Home without turning it into a
  marketing page.
- Make the library a durable collection manager with Current, Archive, and
  Collections views.
- Preserve every existing Nova capability and expose it once from a logical
  location.
- Use Rev motion for high-value cover interactions while keeping lists, grids,
  readers, and frequent controls fast.
- Preserve existing library data, downloads, backup compatibility, extensions,
  source migration, chapter state, and reader engines.

## Non-Goals

- Reproducing Wattpad writing, publishing, messaging, voting, comments,
  subscriptions, advertising, or social profiles.
- Copying Wattpad resources, text, icons, code, layouts pixel for pixel, or
  protected visual identity.
- Replacing Voyager, the database, extension APIs, download formats, backup
  formats, or reader engines during the first architecture migration.
- Adding network recommendations that require a Nova server. Home is generated
  from local library state and installed sources.
- Applying blur, card expansion, parallax, or swipe effects to every surface.

## Anti-Goals

- A decorative feed that delays access to the user's own library.
- Multiple routes to the same setting or action with inconsistent state.
- Motion that blocks input, changes scroll position, or hides downloaded and
  read state.
- A phone-only layout with fixed dimensions.
- Recreating unsupported Wattpad social functions with empty placeholders.

## Approaches Considered

### 1. Wattpad-Inspired Architecture Adapted To Nova - Selected

Use five primary destinations and map each Wattpad-style job to a real Nova
capability. Reuse current domain and screen models, introduce a new shell and
presentation models, and reactivate the existing `NovaHomeTab` as a distinct
destination.

This gives the clearest hierarchy while preserving Nova's identity and avoids
inventing fake writing or social features.

### 2. Literal Spatial Clone

Recreate the same visible route order and screen composition, then place Nova
features in the nearest available locations. This was rejected because writing
and social destinations do not match Nova, important extension and download
features would become hard to find, and a close visual clone would create
unnecessary proprietary-design risk.

### 3. Four-Tab Visual Reskin

Keep the current Library, Discover, Activity, and Me shell and apply Rev motion
inside it. This was rejected because it does not satisfy the request to adopt
the complete architecture and cannot distinguish a personalized Home from
collection management.

## Primary Navigation

### Compact Windows

Use a five-item bottom navigation bar in this order:

1. **Accueil** - personalized local reading dashboard.
2. **Decouvrir** - search, catalogues, sources, extensions, and repositories.
3. **Bibliotheque** - current collection, archive, and user collections.
4. **Activite** - updates, downloads, history, and actionable failures.
5. **Moi** - storage, backups, appearance, reader preferences, and settings.

The bar uses stable height, navigation-bar insets, 48 dp minimum targets,
filled selected icons, outlined inactive icons, and a compact non-moving
selection indicator. Destination labels remain visible when accessibility or
the existing label preference requires them.

### Medium And Expanded Windows

Use the same destinations in a navigation rail. On large expanded widths, the
rail may show labels persistently. The destination state and nested tab state
must survive bottom-bar-to-rail changes.

### Navigation Behavior

- Launch into Accueil after migration.
- Back from a secondary primary destination returns to Accueil.
- Reselecting Accueil scrolls to the top; it does not open search.
- Reselecting Bibliotheque scrolls the active collection to the top.
- Search is available from Accueil and Decouvrir, but both launch the same
  global search route.
- Reader and title-detail screens remain above the shell in the existing
  navigator stack.

## Destination Design

### Accueil

Home is local-first and contains only actionable reading content:

1. A pinned compact header with Nova, global search, refresh status, and profile
   access.
2. A Rev **Reprendre** carousel generated from recently read titles. The active
   card shows cover, next chapter, progress, unread count, and offline state.
3. **Nouveaux chapitres** from the latest successful library update.
4. **Ajoutes recemment** from local library date-added state.
5. Optional source recommendations only when an installed source already
   provides them without blocking local content.

The first frame uses cached database state. Background refresh updates sections
in place and never replaces the entire screen with a spinner.

### Decouvrir

The destination opens with a prominent global search field and three stable
subsections:

- **Catalogues:** installed sources, pinned sources, source categories, and web
  view access.
- **Extensions:** installed, available, update, incompatible, and disabled
  extensions with selection-based bulk actions.
- **Depots:** repository management and update status.

Search results use ordinary adaptive grids and lists. Rev expansion is allowed
only for a featured result carousel, never for every search result.

### Bibliotheque

Use a pinned header with search, filter, sort, view mode, and refresh. Below it,
provide three top-level views:

- **En cours:** saved titles that are unread, started, or currently followed.
- **Archives:** titles intentionally archived or completed and hidden from the
  active collection.
- **Collections:** the user's existing categories represented as reading lists.

Archive membership is exclusively user-controlled. Source status, tracker
status, or reaching the final known chapter never moves a title automatically.
A completed title remains in En cours until the user archives it, preventing a
remote metadata change from making content disappear.

Within each view, retain the Manhwa and Light Novel media filter. Use an
adaptive cover grid, preserving three compact columns only when text and state
remain legible. Each cell shows title, unread count, progress, download state,
and a non-color read-state indicator.

Archiving is presentation and library metadata work, not deletion. Add durable
archive metadata that is included in new backups. Restoring an older backup in
the new app defaults missing archive metadata to active. A new backup restored
by an older Nova build may ignore archive metadata but must retain the title as
a favorite; backward interpretation of the archive view itself is not required.

### Activite

Use Updates, Downloads, History, and Problems as sub-tabs:

- Updates groups releases by date and preserves unread/downloaded state.
- Downloads exposes queue progress, pause/resume, retry, reorder, delete, and
  bulk actions.
- History exposes resume, previous/next chapter context, and clear-history
  actions.
- Problems consolidates library update, source, extension, and download errors
  with a useful recovery action.

Problems is a live view of current actionable conditions, not a persistent
incident inbox. Resolved download failures, completed extension updates, and
successful source/library retries disappear automatically. Historical events
remain in Updates or History where appropriate.

The destination icon may show a bounded badge for actionable updates or
failures. Animations must not replay continuously when counts change.

### Moi

Use a profile-like hierarchy without pretending Nova has a social identity:

1. Local status header: storage folder, last backup, app version, and update
   status.
2. Reading summary: current titles, chapters read, offline titles, and recent
   reading time when already available locally.
3. Grouped full-width routes for Appearance, Readers, Library, Downloads,
   Sources and Extensions, Backup and Restore, Accessibility, Privacy, and
   Advanced.

Do not use nested cards. Each setting has one canonical route.

## Title Detail

Opening a title from Home, Discover, or Library uses one shared destination.
The page contains:

- cover, title, source, media type, compact metadata, and reading progress;
- one primary Lire, Reprendre, or Relire action;
- one save/archive control and one overflow menu;
- a chapter toolbar followed immediately by chapter rows;
- local and restored download state before any network refresh.

Rev cover-to-detail motion connects a selected Home carousel card directly to
the canonical title detail; it does not insert a preview or require a second
tap. The title-detail presentation is one reusable composable hosted by the Rev
expanded surface for carousel entry and by the existing Voyager route for deep
links, notifications, and ordinary grid entry. Both hosts use the same screen
model contract and actions. Ordinary library and search grids use the lighter
existing shared-cover transition so opening several titles remains fast.

## Reader Placement

The reader engines and their complete controls remain unchanged by the shell
migration. The architecture change only updates entry and exit continuity:

- opening a chapter transitions quickly from title detail;
- returning restores the exact title and chapter-list position;
- completing a chapter updates Home, Library, and Activity from the same domain
  state;
- reader chrome never uses backdrop blur or card expansion.

## Rev Visual And Motion System

Integrate the Rev Card Motion implementation as an independent Compose module,
aligned with the host Android, Kotlin, Compose, JVM, and lint conventions.
App-specific models and navigation remain outside the module.

### Rev Is Used For

- the Accueil resume carousel;
- one optional featured discovery carousel;
- card-to-canonical-detail expansion from those carousels;
- dismiss by explicit close action, Android Back, or backdrop tap.

### Rev Is Not Used For

- library grids;
- chapter rows;
- download queues and updates;
- settings;
- reader controls;
- automatic page-to-page navigation.

### Motion Contract

- Carousel paging follows the user's horizontal gesture and snaps to one card.
- Open and close are reciprocal transformations from the selected card bounds.
- The selected card remains sharp. Only the backdrop blurs on Android 12 and
  newer; older versions use a scrim.
- Default open/close duration is 220 to 300 ms after device testing, not the
  module demo's 360 ms if that feels slow in Nova.
- System animator scale and reduced-motion settings replace expansion and blur
  with a short fade-through.
- Auto-advance is disabled for the resume carousel. Reader progress must never
  move without an explicit reading action.
- Only transform and alpha update per frame; image loading, database access,
  and chapter lookup never run inside animation frames.

## State And Data Flow

- Continue using `LibraryScreenModel` and existing domain flows as the initial
  source of library state.
- Introduce immutable destination UI models so Home and Library do not repeat
  chapter lookups or independently derive conflicting status.
- Compute next unread chapter through one cached use case keyed by manga and
  chapter-state revision.
- Share refresh state across Home, Library, and Activity. Starting refresh from
  one destination updates the others without launching duplicate jobs.
- Define Nouveaux chapitres from current durable library state rather than one
  all-or-nothing update run: show titles with unread chapters ordered by the
  latest successfully inserted or refreshed chapter timestamp. Successful
  per-title results remain visible when another source fails, and manual,
  category, and automatic refreshes feed the same state.
- Persist Home scroll/carousel position and each Library subsection's grid
  position independently.
- Use stable manga IDs and chapter IDs for lazy keys and Rev item keys.
- Archive and Collections must use durable library metadata that survives
  backup and restore.

## Loading And Errors

- Show local data immediately.
- Keep stale usable content visible while refreshing.
- Use section-level placeholders only when that section has no local content.
- Show inline retry at the failed section or Activity problem row.
- Distinguish empty Home, empty collection, unavailable source, and refresh
  failure.
- Never expose JSON parser or extension implementation errors directly to the
  user.

## Adaptive Layout

- Compact: bottom navigation, horizontal carousels, adaptive three-column
  library where legible.
- Medium: navigation rail, wider carousel cards, adaptive grid.
- Expanded: navigation rail with labels and centered bounded content; Home may
  place resume and updates in separate columns when both remain independently
  scrollable and readable.
- Fold posture and window size drive layout. Device model names do not.
- Card Motion always receives bounded dimensions and never assumes a fixed
  screen size.

## Accessibility

- Every gesture has a visible button or menu equivalent.
- Rev detail exposes an accessible close action and hides the blurred backdrop
  from TalkBack while open.
- Focus entering detail moves to the title; closing returns focus to the source
  card.
- Carousels announce item position and do not auto-advance.
- Read, unread, progress, archive, and download state use semantics and text or
  icons in addition to color.
- Touch targets are at least 48 dp and layouts tolerate 200 percent font scale.
- Reduced motion, high contrast, landscape, tablet, and foldable widths are
  required verification cases.

## Performance Contract

- Warm local Home and Library content becomes usable in under one second.
- Primary destination switching does not trigger a library reload.
- Rev cards decode images to their displayed dimensions and reuse Coil cache.
- Home limits eager next-chapter resolution to visible and near-visible items.
- No blur is applied to reader content, large lazy lists, or offscreen screens.
- Stable models and content types prevent whole-grid recomposition from one
  progress or download update.
- Baseline profiles and macrobenchmarks cover cold launch, Home first content,
  tab switching, library scroll, title opening, and reader launch.

## Migration Sequence

1. Add navigation, Home-model, and archive/collection contract tests around the
   current behavior.
2. Add and align the independent Rev Card Motion module with reduced-motion and
   accessibility hooks.
3. Replace the shell with five destinations while retaining current destination
   content behind each tab.
4. Rebuild Accueil from the existing `NovaHomeTab` data flow and the Rev resume
   carousel.
5. Split Bibliotheque into En cours, Archives, and Collections without changing
   stored favorites or categories unexpectedly.
6. Reorganize Decouvrir, Activite, and Moi using existing screens and routes.
7. Apply the shared title-detail entry rules and remove bypassed duplicate Home
   and library presentation code.
8. Verify reader entry/exit state, backup/restore, downloads, source migration,
   extension management, and background refresh.
9. Run screenshot, accessibility, unit, integration, and macrobenchmark checks
   on compact, medium, expanded, landscape, and reduced-motion configurations.

Each migration step must compile and keep every primary capability reachable.
The old shell is removed only after parity tests pass.

## Verification Strategy

- Unit tests for destination order, back/reselect behavior, Home section
  derivation, archive mapping, and shared refresh state.
- Compose tests for navigation semantics, selected destination, tab state,
  carousel focus, close behavior, and reduced motion.
- Screenshot tests for Home, Discover, all Library views, Activity, Me, title
  detail, empty/error states, and adaptive widths.
- Existing download, restore, reader progress, extension, and source recovery
  regression suites remain mandatory.
- Device checks for swipe, card open/close, Android Back, system gesture back,
  rotation, process recreation, and font scaling.

## Acceptance Criteria

- Accueil, Decouvrir, Bibliotheque, Activite, and Moi are distinct and stable
  destinations on compact and expanded layouts.
- Every existing Nova feature is reachable once from a logical destination.
- Home resumes the correct next chapter and never hides local content behind a
  network request.
- Library exposes En cours, Archives, Collections, Manhwa, and Light Novel
  without losing categories or backup compatibility.
- Rev motion is present on the intended carousels and nowhere it would slow
  frequent workflows.
- Opening and closing a Rev card preserves focus, state, and source position.
- Readers open quickly and return to the exact chapter-list position.
- Downloaded and restored chapters retain their visible state throughout the
  new architecture.
- No proprietary Wattpad code or assets are added to the repository.
- All affected tests pass, and device verification reports compile and runtime
  checks separately.

## Evidence And Open Constraint

The user supplied Wattpad Android XAPK version 11.42.0 as the architecture
reference. At design time Windows allowed metadata access but denied complete
reads while the file timestamp continued changing, so exact static package
inspection remains pending. Public current product captures corroborate the
separation of Home, Search/Discover, Library, creation/community, and activity
jobs. Nova deliberately replaces unsupported creation/community jobs with its
real Activity and Me capabilities.

Before implementation reaches visual parity review, inspect the stable XAPK if
it becomes readable and record only structural observations. Any new finding
that would change primary destinations requires a design amendment rather than
silent scope expansion.
