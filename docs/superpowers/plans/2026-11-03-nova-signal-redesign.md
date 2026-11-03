# Nova Signal Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace Nova's inherited Viki/Komikku presentation with the adaptive Nova Signal interface while preserving data, downloads, extensions, backups, and both reader engines.

**Architecture:** Introduce a presentation-only Nova Signal design system and migrate one complete user path at a time. Existing ScreenModels, repositories, and Voyager navigation remain authoritative; composables consume stable UI models so visual replacement does not alter persistence or source behavior.

**Tech Stack:** Kotlin, Jetpack Compose Material 3 Expressive, Voyager, Android adaptive window APIs, JUnit 6, existing macrobenchmark module.

**Spec:** `docs/superpowers/specs/2026-11-03-nova-interface-redesign-design.md`

## Global Constraints

- Preserve library data, downloads, backups, extensions, source migration, chapter state, trackers, and reader behavior.
- Keep Voyager during this redesign; Navigation 3 is a later isolated migration.
- Use 48 dp minimum touch targets, 4/6/8 dp shape scale, zero letter spacing, and localized strings.
- Support compact, medium, and expanded windows, edge-to-edge insets, font scale up to 200 percent, and reduced motion.
- Render cached and local data before network refresh; never replace usable content with a loading screen.
- Do not copy proprietary Viki assets or introduce a second reader engine.
- Do not run a Gradle build until the user explicitly approves it.

## Review Focus

- Restored local downloads must remain visibly downloaded before and after a refresh; pinned by Task 4 merge/state tests.
- Empty filters must not be confused with an empty library or source failure; pinned by Task 3 library-state tests.
- Reader progress must not jump into the end of chapter N+1 or skip to N+2; pinned by Task 7 reader-window tests.
- A 200 percent font scale must keep navigation and primary actions reachable; pinned by Task 2 semantics/layout tests and device capture.
- Extension errors and malformed JSON must render a localized inline error instead of crashing; pinned by Task 5 error-mapping tests.

---

### Task 0: Characterization And Capability Inventory

**Files:**
- Create: `docs/nova-ui-capability-matrix.md`
- Create: `app/src/test/java/eu/kanade/presentation/NovaCapabilityRoutesTest.kt`
- Modify: `app/build.gradle.kts`

**Interfaces:**
- Produces: an explicit capability-to-route inventory and the Compose UI test foundation used to reject missing or duplicated actions in later tasks.

- [ ] **Step 1: Inventory every current route and action**

Map library, updates, history, downloads, browsing, extensions, repositories, title actions, chapter selection, backup, settings, manhwa controls, and light-novel controls to their current Screen/ScreenModel callback and proposed Nova Signal destination.

- [ ] **Step 2: Write the failing route uniqueness test**

Assert every required capability has one owner destination, one production callback, and no missing replacement route.

- [ ] **Step 3: Add only the Compose test dependencies needed by the characterization harness**

Reuse the repository's existing Android test/version catalog conventions. Do not add a screenshot library until emulator capture requirements prove it necessary.

- [ ] **Step 4: Verify the focused test fails on known duplicate/missing routes**

Run: `./gradlew :app:testDebugUnitTest --tests '*NovaCapabilityRoutesTest'`

Expected: FAIL on the documented duplicate or missing routes that Tasks 2 through 7 will close.

- [ ] **Step 5: Commit**

```bash
git add docs/nova-ui-capability-matrix.md app/build.gradle.kts app/src/test/java/eu/kanade/presentation/NovaCapabilityRoutesTest.kt
git commit -m "test(ui): characterize Nova presentation capabilities"
```

### Task 1: Nova Signal Design Foundation

**Files:**
- Create: `app/src/main/java/eu/kanade/presentation/theme/NovaSignalTheme.kt`
- Create: `app/src/main/java/eu/kanade/presentation/components/NovaSignalComponents.kt`
- Create: `app/src/main/java/eu/kanade/presentation/components/NovaMotion.kt`
- Modify: `app/src/main/java/eu/kanade/presentation/theme/TachiyomiTheme.kt`
- Modify: `app/src/main/java/eu/kanade/presentation/theme/colorscheme/TachiyomiColorScheme.kt`
- Test: `app/src/test/java/eu/kanade/presentation/theme/NovaSignalThemeTest.kt`

**Interfaces:**
- Produces: `NovaSignalTokens`, `NovaSignalMotion`, `NovaSignalStatus`, `NovaSignalRail`, and `rememberNovaMotionSpec()` for all later tasks.

- [ ] **Step 1: Write failing token and status tests**

Assert 4/6/8 dp shapes, 48 dp minimum target, 90/160/220 ms motion values, distinct light/dark semantic colors, and non-color status icons for every `NovaSignalStatus`.

- [ ] **Step 2: Verify the tests fail**

Run: `./gradlew :app:testDebugUnitTest --tests '*NovaSignalThemeTest'`

Expected: FAIL because the Nova Signal types do not exist.

- [ ] **Step 3: Implement the token, status, rail, and motion primitives**

Keep the existing theme preference and dynamic-color plumbing. Replace hard-coded Viki colors at the theme boundary with semantic Material color roles and expose reduced-motion-safe specs.

- [ ] **Step 4: Verify the focused tests pass**

Run: `./gradlew :app:testDebugUnitTest --tests '*NovaSignalThemeTest'`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/eu/kanade/presentation/theme app/src/main/java/eu/kanade/presentation/components app/src/test/java/eu/kanade/presentation/theme
git commit -m "feat(ui): add Nova Signal design foundation"
```

### Task 2: Adaptive Navigation Shell

**Files:**
- Create: `app/src/main/java/eu/kanade/tachiyomi/ui/activity/NovaActivityTab.kt`
- Create: `app/src/main/java/eu/kanade/presentation/home/NovaNavigation.kt`
- Modify: `app/src/main/java/eu/kanade/tachiyomi/ui/home/HomeScreen.kt`
- Modify: `app/src/main/java/eu/kanade/tachiyomi/ui/home/NovaLibraryTab.kt`
- Modify: `app/src/main/java/eu/kanade/tachiyomi/ui/browse/BrowseTab.kt`
- Modify: `app/src/main/java/eu/kanade/tachiyomi/ui/more/MoreTab.kt`
- Modify: `i18n/src/commonMain/moko-resources/strings.xml`
- Test: `app/src/test/java/eu/kanade/presentation/home/NovaNavigationTest.kt`

**Interfaces:**
- Consumes: `NovaSignalTokens`, `NovaSignalMotion`, `NovaSignalRail` from Task 1.
- Produces: `NovaDestination`, `NovaNavigationLayout`, and `resolveNovaNavigationLayout(windowWidthDp: Int)`.

- [ ] **Step 1: Write failing destination and width-policy tests**

Assert four unique destinations, bottom navigation below 600 dp, rail at 600 dp and above, stable destination identity, and Activity badge aggregation.

- [ ] **Step 2: Verify the tests fail**

Run: `./gradlew :app:testDebugUnitTest --tests '*NovaNavigationTest'`

Expected: FAIL because the destination model does not exist.

- [ ] **Step 3: Implement the four-destination shell**

Map Bibliotheque, Decouvrir, Activite, and Moi onto existing Voyager tabs. Activity hosts updates, downloads, and history as sub-tabs. Preserve selected-tab state and current `HomeScreen.openTab()` compatibility.

- [ ] **Step 4: Add semantics and predictive-back-safe behavior**

Remove root back interception when already on Bibliotheque. Expose selected state and badges to TalkBack and keep labels visible at large font scale.

- [ ] **Step 5: Verify focused tests pass**

Run: `./gradlew :app:testDebugUnitTest --tests '*NovaNavigationTest'`

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/eu/kanade/tachiyomi/ui app/src/main/java/eu/kanade/presentation/home i18n/src/commonMain/moko-resources/strings.xml app/src/test/java/eu/kanade/presentation/home
git commit -m "feat(ui): add adaptive Nova navigation"
```

### Task 3: Reading-First Library

**Files:**
- Create: `app/src/main/java/eu/kanade/presentation/library/NovaLibraryScreen.kt`
- Create: `app/src/main/java/eu/kanade/presentation/library/NovaLibraryModels.kt`
- Create: `app/src/main/java/eu/kanade/presentation/library/components/NovaResumeStrip.kt`
- Create: `app/src/main/java/eu/kanade/presentation/library/components/NovaLibraryGrid.kt`
- Modify: `app/src/main/java/eu/kanade/tachiyomi/ui/home/NovaLibraryTab.kt`
- Modify: `i18n/src/commonMain/moko-resources/strings.xml`
- Test: `app/src/test/java/eu/kanade/presentation/library/NovaLibraryModelsTest.kt`

**Interfaces:**
- Consumes: Task 1 components and Task 2 navigation shell.
- Produces: `NovaLibraryEntry`, `NovaResumeEntry`, `NovaLibraryUiState`, `buildNovaLibraryUiState()`, and `coverCompletionFraction()`.

- [ ] **Step 1: Write failing state-mapping tests**

Cover empty library, empty filtered result, Manhwa/Light Novel segmentation, user categories, unread count, next readable chapter, local-download state, full-read grayscale, and partial-read completion fraction.

- [ ] **Step 2: Verify the tests fail**

Run: `./gradlew :app:testDebugUnitTest --tests '*NovaLibraryModelsTest'`

Expected: FAIL because the Nova library model does not exist.

- [ ] **Step 3: Implement stable presentation models**

Move sorting, filtering, resume selection, and status derivation out of the large composable. Keep stable manga IDs and content types for lazy-grid keys.

- [ ] **Step 4: Implement the pinned header, resume strip, category controls, and adaptive grid**

Use three compact columns only when the calculated cell width stays legible; otherwise use `GridCells.Adaptive`. Preserve visible cached items during pull-to-refresh and pause resume-strip auto-advance on touch, focus, or reduced motion.

- [ ] **Step 5: Verify focused tests pass**

Run: `./gradlew :app:testDebugUnitTest --tests '*NovaLibraryModelsTest'`

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/eu/kanade/presentation/library app/src/main/java/eu/kanade/tachiyomi/ui/home/NovaLibraryTab.kt i18n/src/commonMain/moko-resources/strings.xml app/src/test/java/eu/kanade/presentation/library
git commit -m "feat(ui): replace library with Nova Signal hub"
```

### Task 4: Title Detail And Chapter State

**Files:**
- Create: `app/src/main/java/eu/kanade/presentation/manga/NovaMangaScreen.kt`
- Create: `app/src/main/java/eu/kanade/presentation/manga/NovaMangaModels.kt`
- Create: `app/src/main/java/eu/kanade/presentation/manga/components/NovaMangaHeader.kt`
- Create: `app/src/main/java/eu/kanade/presentation/manga/components/NovaChapterList.kt`
- Modify: `app/src/main/java/eu/kanade/presentation/manga/MangaScreen.kt`
- Modify: `app/src/main/java/eu/kanade/tachiyomi/ui/manga/MangaScreenModel.kt`
- Modify: `i18n/src/commonMain/moko-resources/strings.xml`
- Test: `app/src/test/java/eu/kanade/presentation/manga/NovaMangaModelsTest.kt`
- Test: `app/src/test/java/eu/kanade/tachiyomi/data/backup/restore/restorers/RestoredChapterMergeTest.kt`

**Interfaces:**
- Consumes: Task 1 components and existing `MangaScreenModel.State.Success`.
- Produces: `NovaMangaUiState`, `NovaPrimaryReadAction`, `NovaChapterUiModel`, and `resolveDownloadedState()`.

- [ ] **Step 1: Write failing read-action and chapter-state tests**

Assert Lire/Reprendre/Relire selection, restored-download recognition without exact source identity, read/in-progress/unread presentation, local-first ordering, and distinct loading/error/empty sections.

- [ ] **Step 2: Verify the tests fail**

Run: `./gradlew :app:testDebugUnitTest --tests '*NovaMangaModelsTest' --tests '*RestoredChapterMergeTest'`

Expected: FAIL on the new presentation contracts.

- [ ] **Step 3: Implement the local-first presentation mapper**

Derive UI state from the existing ScreenModel and download cache. Do not start network work in composables. Use the existing chapter and download recovery contracts. Any cross-source URL aliasing requires a separate domain design and is not added in this presentation task.

- [ ] **Step 4: Implement collapsing detail and single chapter toolbar**

Provide one primary read action and one instance of search, filter, sort, web view, share, tracking, migration, download-all, mark-read, and mark-unread actions. Preserve long-press selection actions.

- [ ] **Step 5: Verify focused tests pass**

Run: `./gradlew :app:testDebugUnitTest --tests '*NovaMangaModelsTest' --tests '*RestoredChapterMergeTest'`

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/eu/kanade/presentation/manga app/src/main/java/eu/kanade/tachiyomi/ui/manga i18n/src/commonMain/moko-resources/strings.xml app/src/test/java/eu/kanade/presentation/manga app/src/test/java/eu/kanade/tachiyomi/data/backup/restore/restorers/RestoredChapterMergeTest.kt
git commit -m "feat(ui): rebuild title and chapter experience"
```

### Task 5: Discover, Extensions, And Error Presentation

**Files:**
- Create: `app/src/main/java/eu/kanade/presentation/browse/NovaDiscoverScreen.kt`
- Create: `app/src/main/java/eu/kanade/presentation/browse/NovaDiscoverModels.kt`
- Modify: `app/src/main/java/eu/kanade/tachiyomi/ui/browse/BrowseTab.kt`
- Modify: `app/src/main/java/eu/kanade/presentation/browse/BrowseSourceScreen.kt`
- Modify: `app/src/main/java/eu/kanade/presentation/browse/components/BrowseSourceDialogs.kt`
- Modify: `i18n/src/commonMain/moko-resources/strings.xml`
- Test: `app/src/test/java/eu/kanade/presentation/browse/NovaDiscoverModelsTest.kt`

**Interfaces:**
- Consumes: Task 1 components and existing source/extension managers.
- Produces: `NovaDiscoverTab`, `NovaExtensionState`, `NovaSourceError`, and `toNovaSourceError(Throwable)`.

- [ ] **Step 1: Write failing tab, bulk-selection, and error-mapping tests**

Cover installed/update/disabled/incompatible extension states, selected counts, malformed JSON, HTTP denial, attestation failure, unsupported signed reader, and empty search results.

- [ ] **Step 2: Verify the tests fail**

Run: `./gradlew :app:testDebugUnitTest --tests '*NovaDiscoverModelsTest'`

Expected: FAIL because the models and mapper do not exist.

- [ ] **Step 3: Implement unified Discover models and defensive error mapping**

Keep raw exceptions in logs only. Present localized source-specific recovery actions such as retry, open web view, update extension, or sign in.

- [ ] **Step 4: Implement catalogues, extensions, repositories, and bulk selection**

Use one search field and active-tab-scoped filters. Keep source web view reachable from search results and title details.

- [ ] **Step 5: Verify focused tests pass**

Run: `./gradlew :app:testDebugUnitTest --tests '*NovaDiscoverModelsTest'`

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/eu/kanade/presentation/browse app/src/main/java/eu/kanade/tachiyomi/ui/browse i18n/src/commonMain/moko-resources/strings.xml app/src/test/java/eu/kanade/presentation/browse
git commit -m "feat(ui): unify discovery and extensions"
```

### Task 6: Activity And Moi

**Files:**
- Create: `app/src/main/java/eu/kanade/presentation/activity/NovaActivityScreen.kt`
- Create: `app/src/main/java/eu/kanade/presentation/activity/NovaActivityModels.kt`
- Create: `app/src/main/java/eu/kanade/presentation/more/NovaMoreScreen.kt`
- Modify: `app/src/main/java/eu/kanade/presentation/more/MoreScreen.kt`
- Modify: `app/src/main/java/eu/kanade/tachiyomi/ui/activity/NovaActivityTab.kt`
- Modify: `i18n/src/commonMain/moko-resources/strings.xml`
- Test: `app/src/test/java/eu/kanade/presentation/activity/NovaActivityModelsTest.kt`

**Interfaces:**
- Consumes: Task 1 components, updates/download/history presenters, and current settings routes.
- Produces: `NovaActivitySection`, `NovaDownloadQueueItem`, and `NovaSettingsSection`.

- [ ] **Step 1: Write failing activity aggregation tests**

Assert update/download/history counts, queued/running/paused/failed states, retry action visibility, and stable queue ordering.

- [ ] **Step 2: Verify the tests fail**

Run: `./gradlew :app:testDebugUnitTest --tests '*NovaActivityModelsTest'`

Expected: FAIL because the activity models do not exist.

- [ ] **Step 3: Implement Activity with actionable download failures**

Reuse existing operations and presenters. Keep queue progress updates item-scoped and expose pause/resume/retry/reorder/bulk controls.

- [ ] **Step 4: Replace Moi with single-entry grouped settings**

Keep every existing route, remove duplicate entries, and lead with storage folder, backup status, update status, and version. Use full-width rows and no nested cards.

- [ ] **Step 5: Verify focused tests pass**

Run: `./gradlew :app:testDebugUnitTest --tests '*NovaActivityModelsTest'`

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/eu/kanade/presentation/activity app/src/main/java/eu/kanade/presentation/more app/src/main/java/eu/kanade/tachiyomi/ui/activity i18n/src/commonMain/moko-resources/strings.xml app/src/test/java/eu/kanade/presentation/activity
git commit -m "feat(ui): add activity hub and rebuild Moi"
```

### Task 7: Reader Chrome, Adaptive Verification, And Cleanup

**Files:**
- Create: `app/src/main/java/eu/kanade/presentation/reader/NovaReaderControls.kt`
- Create: `app/src/main/java/eu/kanade/presentation/reader/NovaReaderProgress.kt`
- Modify: `app/src/main/java/eu/kanade/presentation/reader/appbars/ReaderAppBars.kt`
- Modify: `app/src/main/java/eu/kanade/presentation/reader/appbars/ReaderTopBar.kt`
- Modify: `app/src/main/java/eu/kanade/presentation/reader/appbars/ReaderBottomBar.kt`
- Modify: `app/src/main/java/eu/kanade/presentation/reader/settings/ReaderSettingsDialog.kt`
- Modify: `app/src/main/java/eu/kanade/presentation/reader/settings/LnReaderSettingsPage.kt`
- Modify: `app/src/main/java/eu/kanade/tachiyomi/ui/reader/ReaderChapterWindow.kt`
- Modify: `macrobenchmark/src/main/java/tachiyomi/macrobenchmark/StartupBenchmark.kt`
- Test: `app/src/test/java/eu/kanade/tachiyomi/ui/reader/ReaderChapterWindowTest.kt`
- Test: `app/src/test/java/eu/kanade/presentation/reader/NovaReaderProgressTest.kt`

**Interfaces:**
- Consumes: Task 1 components and existing visual/text reader controllers.
- Produces: `NovaReaderOverlayState`, `NovaReaderProgress`, and `resolveNextChapterTransition()`.

- [ ] **Step 1: Write failing progress and chapter-window tests**

Assert exact active-chapter progress, one scrollbar, previous/next navigation, no N+2 skip, no stale restoration in N+1, controls visible after tap, and reduced-motion transition selection.

- [ ] **Step 2: Verify the tests fail**

Run: `./gradlew :app:testDebugUnitTest --tests '*ReaderChapterWindowTest' --tests '*NovaReaderProgressTest'`

Expected: FAIL on the new progress and transition contracts.

- [ ] **Step 3: Replace manhwa reader chrome**

Keep one top overlay and one bottom overlay, one horizontal progress control, deliberate end-of-chapter continuation, and adjacent-page preloading without changing the page loader.

- [ ] **Step 4: Reorganize light-novel controls without removing LNReader features**

Separate quick reading, typography, and advanced CSS/JS/TTS controls. Retain Media3 notification behavior and hide image-only options for text chapters.

- [ ] **Step 5: Remove bypassed Viki presentation files after parity verification**

Delete `NovaVikiVisuals.kt`, `NovaVikiLibraryScreen.kt`, `NovaVikiHomeScreen.kt`, and `NovaVikiMangaScreen.kt` only when no production references remain. Keep data mappers that are still shared until their callers migrate.

- [ ] **Step 6: Run the complete verification matrix after explicit build approval**

Run unit tests, `:app:assembleDebug`, baseline startup benchmark, and emulator captures for compact portrait, compact landscape, foldable/medium, and expanded tablet at normal and 200 percent font scale.

Expected: all tests pass; no clipped text, overlapping controls, duplicate actions, blank reader, or lost local-download state.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/eu/kanade/presentation app/src/main/java/eu/kanade/tachiyomi/ui/reader app/src/test/java/eu/kanade/presentation/reader app/src/test/java/eu/kanade/tachiyomi/ui/reader macrobenchmark
git commit -m "feat(ui): complete Nova Signal readers and cleanup"
```
