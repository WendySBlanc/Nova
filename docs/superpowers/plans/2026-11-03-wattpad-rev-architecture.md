# Nova Wattpad-Inspired Rev Architecture Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace Nova's four-destination shell with a five-destination Wattpad-inspired reading architecture and apply Rev card motion to the local-first Home resume experience without regressing library, download, extension, backup, or reader behavior.

**Architecture:** Keep the existing Voyager navigation and domain/data flows. Add an independent `card-motion` Compose module, reactivate `NovaHomeTab`, introduce pure UI derivation helpers for Home and Library shelves, and adapt the existing screens incrementally so each task leaves a runnable application.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, Voyager tabs, Coil 3, SQLDelight-backed existing domain flows, JUnit 5/Kotest, Gradle convention plugins.

**Spec:** `docs/superpowers/specs/2026-11-03-wattpad-rev-architecture-design.md`

## Global Constraints

- Do not copy Wattpad code, resources, copy, icons, or protected visual identity.
- Preserve existing database, extension API, download, backup, source migration, and reader contracts.
- Use five destinations in order: Accueil, Decouvrir, Bibliotheque, Activite, Moi.
- Use Rev only for the Home resume carousel and an optional featured discovery carousel.
- Keep every essential gesture available through a visible control or menu.
- Respect 48 dp touch targets, adaptive bottom-bar/rail navigation, TalkBack, font scaling, and reduced motion.
- Keep warm local Home and Library usable in under one second and never block them on network refresh.
- Do not push until targeted tests, complete affected module tests, formatting checks for changed files, and the preview build pass.

## Review Focus

- An empty or uninitialized library must show a usable Home and Library without crashing or producing invalid pager keys; covered in Task 4 tests.
- A missing, duplicate, or differently cased Archives category must never hide a title unexpectedly; covered in Task 5 tests.
- Rotation or bottom-bar-to-rail changes must preserve the selected primary destination and nested tab state; covered in Task 3 tests.
- Reduced motion must avoid blur and spatial expansion while preserving card activation and focus restoration; covered in Task 2 tests.
- A partial library refresh must retain successful new-chapter items and cached content instead of clearing Home; covered in Task 4 tests.

---

### Task 1: Baseline Checkpoint And Navigation Contracts

**Files:**
- Modify: `app/src/test/java/eu/kanade/presentation/home/NovaNavigationTest.kt`
- Modify: `docs/superpowers/specs/2026-11-03-wattpad-rev-architecture-design.md`
- Create: `docs/superpowers/plans/2026-11-03-wattpad-rev-architecture.md`
- Preserve and include the already-tested motion cleanup files currently modified in the worktree.

**Interfaces:**
- Produces: an explicit five-destination test contract and a restorable Git checkpoint before architecture code changes.

- [x] **Step 1: Verify the existing motion and navigation baseline, then commit the approved design, plan, and prior motion cleanup as the restore point**

Run: `./gradlew.bat :app:testDebugUnitTest :presentation-core:testDebugUnitTest :domain:testDebugUnitTest --no-problems-report`

Expected: BUILD SUCCESSFUL.

Local note: the Kotlin compilation completed on 2026-11-03. The combined test run could not start its forked test workers because Windows reported an insufficient paging file; the same suites had passed before this design checkpoint. Targeted tests are run one class at a time below.

Commit message: `docs: define Wattpad-inspired Nova architecture`

- [x] **Step 2: Change the destination contract test to expect `Home`, `Discover`, `Library`, `Activity`, and `Me` in that order**

The test must fail because `NovaDestination.Home` does not yet exist.

- [x] **Step 3: Run the focused navigation test and verify it fails**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "eu.kanade.presentation.home.NovaNavigationTest" --no-problems-report`

Expected: FAIL with an unresolved or mismatched `Home` destination.

### Task 2: Independent Rev Card Motion Module

**Files:**
- Modify: `settings.gradle.kts`
- Modify: `app/build.gradle.kts`
- Create: `card-motion/build.gradle.kts`
- Create: `card-motion/src/main/AndroidManifest.xml`
- Create: `card-motion/src/main/kotlin/nova/cardmotion/CardCarousel.kt`
- Create: `card-motion/src/test/kotlin/nova/cardmotion/CardMotionSpecTest.kt`

**Interfaces:**
- Produces: `CardMotionSpec`, `CardMotionMode`, and generic `CardCarousel<T>` with stable keys, optional `onItemExpanded: (T) -> Unit`, explicit dismiss behavior, and reduced-motion mode.
- Consumes later: Home passes stable manga IDs and `CardMotionMode.Spatial` or `Reduced`.

- [x] **Step 1: Write failing `CardMotionSpecTest` cases**

Assert that invalid negative duration/blur values throw, reduced mode resolves duration to zero and blur to zero, and spatial mode retains the supplied 260 ms duration and 18 dp blur.

- [x] **Step 2: Run the module test and verify the module is unresolved**

Run: `./gradlew.bat :card-motion:testDebugUnitTest --no-problems-report`

Expected: FAIL because `:card-motion` is not included.

- [x] **Step 3: Add `:card-motion` using the repository's `mihon.library` and `mihon.library.compose` plugins**

Use namespace `nova.cardmotion`, the host SDK/JVM conventions, Compose foundation/activity/animation dependencies, and the repository test bundle. Add `implementation(projects.cardMotion)` to `:app`.

- [x] **Step 4: Implement the generic Rev carousel**

Keep the bundled skill implementation as the baseline, then add:

- `enum class CardMotionMode { Spatial, Reduced }`
- `CardMotionSpec.resolved(mode: CardMotionMode): CardMotionSpec`
- `onItemExpanded: ((T) -> Unit)?`
- one-shot activation after expansion, with internal selection reset so returning from Voyager does not reopen the card
- stable unique key validation and a safe empty-items state
- backdrop semantics hidden while expanded and an explicit dismiss callback for hosted detail

- [x] **Step 5: Run module tests and compile the host**

Run: `./gradlew.bat :card-motion:testDebugUnitTest :app:compileDebugKotlin --no-problems-report`

Expected: BUILD SUCCESSFUL.

- [x] **Step 6: Commit**

Commit message: `feat: add adaptive Rev card motion module`

### Task 3: Five-Destination Adaptive Shell

**Files:**
- Modify: `app/src/main/java/eu/kanade/presentation/home/NovaNavigation.kt`
- Modify: `app/src/main/java/eu/kanade/tachiyomi/ui/home/HomeScreen.kt`
- Modify: `app/src/main/java/eu/kanade/tachiyomi/ui/home/NovaHomeTab.kt`
- Modify: `app/src/main/java/eu/kanade/tachiyomi/ui/home/NovaLibraryTab.kt`
- Modify: `app/src/main/java/eu/kanade/tachiyomi/ui/browse/BrowseTab.kt`
- Modify: `app/src/main/java/eu/kanade/tachiyomi/ui/activity/NovaActivityTab.kt`
- Test: `app/src/test/java/eu/kanade/presentation/home/NovaNavigationTest.kt`

**Interfaces:**
- Produces: `NovaDestination.Home`, stable destination ordering, Home as initial tab, and adaptive bottom bar/rail with the same route set.
- Consumes: existing Voyager `Tab` objects and `resolveNovaNavigationLayout`.

- [x] **Step 1: Complete the failing destination test with compact and expanded layout assertions**

Also assert a pure `defaultNovaDestination()` or equivalent returns `Home`, so process recreation and layout changes share one start contract.

- [x] **Step 2: Implement the five-destination enum and initial-route helper**

Set `NovaDestination.entries` to `Home, Discover, Library, Activity, Me`.

- [x] **Step 3: Change `HomeScreen.TABS` and `TabNavigator`**

Use `NovaHomeTab, BrowseTab, NovaLibraryTab, NovaActivityTab, MoreTab`; initialize with `NovaHomeTab`; root Back returns to Home. Reselect Home must scroll/reset Home rather than open search.

- [x] **Step 4: Align tab indices and route mappings**

Use indices Home 0, Discover 1, Library 2, Activity 3, Me 4. Preserve `HomeScreen.openTab` compatibility for existing callers and map legacy `Tab.Library` directly to `NovaLibraryTab`.

- [x] **Step 5: Run focused and complete app unit tests**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "eu.kanade.presentation.home.NovaNavigationTest" --no-problems-report`

Then: `./gradlew.bat :app:testDebugUnitTest --no-problems-report`

Expected: BUILD SUCCESSFUL.

- [x] **Step 6: Commit**

Commit message: `feat: add five-destination Nova shell`

### Task 4: Local-First Rev Home

**Files:**
- Create: `app/src/main/java/eu/kanade/presentation/home/NovaHomeModels.kt`
- Create: `app/src/test/java/eu/kanade/presentation/home/NovaHomeModelsTest.kt`
- Create: `app/src/main/java/eu/kanade/presentation/library/components/NovaRevResumeCarousel.kt`
- Modify: `app/src/main/java/eu/kanade/tachiyomi/ui/home/NovaHomeTab.kt`
- Modify: `app/src/main/java/eu/kanade/presentation/library/components/NovaVikiHomeScreen.kt`

**Interfaces:**
- Produces: `NovaHomeSections`, `deriveNovaHomeSections(items, maxResume, maxRecent)`, and `NovaRevResumeCarousel`.
- Consumes: `NovaLibraryEntry`, `CardCarousel`, existing Coil requests, `onOpenManga`, and `onPlay`.

- [x] **Step 1: Write failing Home derivation tests**

Cover empty/uninitialized input, started titles ordered by `lastRead`, unread recent titles ordered by `latestUpload`, recently added ordering, stable deduplication, and partial refresh data that retains successful items.

- [ ] **Step 2: Run the focused test and verify it fails**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "eu.kanade.presentation.home.NovaHomeModelsTest" --no-problems-report`

Expected: FAIL because `deriveNovaHomeSections` is missing.

- [x] **Step 3: Implement immutable Home section derivation**

Do not perform chapter lookup, filesystem work, or network work in the helper. Use existing `lastRead`, `remainingChapters`, `latestUpload`, and `dateAdded` fields.

- [x] **Step 4: Build `NovaRevResumeCarousel`**

Render landscape reading cards containing cover, title, next chapter subtitle, progress, unread count, and local/offline indicator when available. Use manga ID as the stable key, 260 ms spatial motion, no auto-advance, and reduced mode when the system animation scale is disabled. `onItemExpanded` opens the canonical `MangaScreen` without a second tap.

- [x] **Step 5: Replace the old auto-advancing Home hero**

Keep the pinned search header and pull-to-refresh. Order sections as Reprendre, Nouveaux chapitres, Ajoutes recemment. Keep cached content visible while refresh runs and preserve section list position with saveable state.

- [x] **Step 6: Run Home tests and compile**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "eu.kanade.presentation.home.NovaHomeModelsTest" :app:compileDebugKotlin --no-problems-report`

Expected: BUILD SUCCESSFUL.

- [x] **Step 7: Commit**

Commit message: `feat: add Rev-powered local reading home`

### Task 5: Current, Archive, And Collections Library

**Files:**
- Create: `app/src/main/java/eu/kanade/presentation/library/NovaLibraryShelves.kt`
- Create: `app/src/test/java/eu/kanade/presentation/library/NovaLibraryShelvesTest.kt`
- Modify: `app/src/main/java/eu/kanade/tachiyomi/ui/library/LibraryScreenModel.kt`
- Modify: `app/src/main/java/eu/kanade/tachiyomi/ui/home/NovaLibraryTab.kt`
- Modify: `app/src/main/java/eu/kanade/presentation/library/NovaLibraryScreen.kt`
- Modify: `app/src/main/java/eu/kanade/presentation/library/components/NovaVikiLibraryScreen.kt`

**Interfaces:**
- Produces: `NovaLibraryShelf { Current, Archive, Collections }`, `findNovaArchiveCategory`, `filterNovaLibraryShelf`, and `LibraryScreenModel.setNovaArchived(mangaId, archived)`.
- Consumes: existing categories and `NovaLibraryEntry.categoryIds`; archive metadata is an ordinary backed-up category named `Archives`, excluded from collection chips.

- [x] **Step 1: Write failing shelf tests**

Assert case-insensitive archive-category matching, Current excluding archived titles, Archive containing only explicit archive members, Collections excluding the archive category, missing archive category leaving all titles Current, and duplicate archive names choosing the lowest stable category ID without hiding unrelated titles.

- [ ] **Step 2: Run the focused test and verify it fails**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "eu.kanade.presentation.library.NovaLibraryShelvesTest" --no-problems-report`

Expected: FAIL because shelf helpers are missing.

- [x] **Step 3: Implement pure shelf derivation**

Use the localized canonical name `Archives` with case-insensitive matching. Keep the helper independent from Compose and repositories.

- [x] **Step 4: Implement `LibraryScreenModel.setNovaArchived`**

Create the ordinary Archives category on first archive, resolve its inserted ID by re-querying categories, preserve every non-archive category, and add/remove only the archive category. No source/tracker status may call this method automatically.

- [x] **Step 5: Replace the library hero with shelf controls**

Add En cours, Archives, Collections controls above the existing Manhwa/Light Novel and sort controls. Show collection chips only in Collections. Preserve the adaptive grid and scroll state per shelf. Add a visible archive/unarchive control to each card and keep long press free for future selection mode.

- [x] **Step 6: Run shelf, backup, restore, and app tests**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "eu.kanade.presentation.library.NovaLibraryShelvesTest" --tests "eu.kanade.tachiyomi.data.backup.*" --no-problems-report`

Then: `./gradlew.bat :app:testDebugUnitTest --no-problems-report`

Expected: BUILD SUCCESSFUL.

- [x] **Step 7: Commit**

Commit message: `feat: add current archive and collection shelves`

### Task 6: Discover, Activity, And Me Alignment

**Files:**
- Modify: `app/src/main/java/eu/kanade/tachiyomi/ui/browse/BrowseTab.kt`
- Modify: `app/src/main/java/eu/kanade/presentation/components/TabbedScreen.kt`
- Modify: `app/src/main/java/eu/kanade/tachiyomi/ui/activity/NovaActivityTab.kt`
- Modify: `app/src/main/java/eu/kanade/presentation/more/MoreScreen.kt`
- Create: `app/src/test/java/eu/kanade/presentation/home/NovaDestinationContentTest.kt`

**Interfaces:**
- Produces: stable Discover subsection order, `NovaActivitySection.Problems`, and one canonical Me route per settings group.
- Consumes: existing source, extension, migration, download, history, update-error, and settings screens.

- [x] **Step 1: Write failing destination-content tests**

Assert Discover starts with sources/catalogues and contains extensions; Activity exposes Updates, Downloads, History, Problems in that order; Me quick routes contain no duplicate Activity destinations already present in the shell.

- [x] **Step 2: Implement stable Discover hierarchy**

Keep source/catalogue, feed when enabled, extensions, and migration functionality. Ensure the top toolbar exposes global search consistently and switching extension tab still resolves the correct index from the actual tab list rather than a hard-coded page.

- [x] **Step 3: Add Activity Problems**

Use a scrollable four-tab row. Problems is a live hub linking to current library update errors and other existing actionable error surfaces; it must not create a persistent incident database.

- [x] **Step 4: Remove duplicate Activity shortcuts from Me**

Keep downloads, categories, statistics, storage/backup/settings, readers, appearance, accessibility, and advanced routes. Activity, Updates, and History remain canonical in Activity.

- [x] **Step 5: Run focused and app tests**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "eu.kanade.presentation.home.NovaDestinationContentTest" --no-problems-report`

Then: `./gradlew.bat :app:testDebugUnitTest --no-problems-report`

Expected: BUILD SUCCESSFUL.

- [x] **Step 6: Commit**

Commit message: `feat: align discover activity and me destinations`

### Task 7: Verification, Preview Build, And Push

**Files:**
- Update checkbox state in this plan as tasks complete.
- No unrelated source edits.

**Interfaces:**
- Consumes: all earlier tasks.
- Produces: verified commit history pushed to the current Nova branch, triggering the existing preview update workflow.

- [x] **Step 1: Run targeted formatting on changed Kotlin files**

Use Spotless IDE-hook paths so pre-existing unrelated formatting violations are not rewritten.

- [x] **Step 2: Run complete affected test suites**

Run: `./gradlew.bat :card-motion:testDebugUnitTest :app:testDebugUnitTest :presentation-core:testDebugUnitTest :domain:testDebugUnitTest --no-problems-report`

Expected: BUILD SUCCESSFUL.

- [x] **Step 3: Run static and release-oriented checks**

Run: `git diff --check`

Run: `./gradlew.bat :app:lintDebug --no-problems-report`

Expected: no new errors attributable to changed files.

- [x] **Step 4: Build the preview APK**

Run the repository's existing preview variant command used by the GitHub release workflow. Verify APK existence, size, package ID, version code, and signing mode without installing because no ADB device is connected.

- [x] **Step 5: Review the final diff and commit any verification-only fixes**

Commit message when needed: `fix: finalize Wattpad-inspired Nova shell`

- [ ] **Step 6: Push the current branch**

Push only after verifying the remote/branch and confirming no secrets or local artifacts are tracked. Report the commit SHA, remote branch, workflow URL, build result available at push time, and any device-only checks still pending.
