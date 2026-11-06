# Compact Library and Manga UI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Compacter le carrousel, les controles de bibliotheque, le synopsis et l'en-tete des chapitres tout en adoptant le `LoadingIndicator` Material 3 Expressive.

**Architecture:** Conserver les donnees et callbacks existants, puis remplacer uniquement les compositions visuelles. Les decisions de taille et de libelle qui ont un comportement observable sont placees dans des fonctions pures testables; les composants Compose utilisent des primitives stables et adaptatives.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3 Expressive, Coil 3, JUnit 5, Kotest.

**Spec:** `docs/superpowers/specs/2026-11-06-compact-library-manga-design.md`

## Global Constraints

- Aucune nouvelle dependance.
- Zones tactiles d'au moins 48 dp.
- Aucun `ButtonGroup`, `ToggleButtonGroup` ou `HorizontalFloatingToolbar` dans les surfaces modifiees.
- Respect du reglage systeme de reduction des animations.
- Conserver les callbacks, filtres, telechargements et etats de lecture existants.
- Demander l'autorisation avant de construire un APK.

## Review Focus

- Largeur inferieure a 360 dp: aucun texte ou bouton ne doit se chevaucher.
- Grande taille de police: les actions restent tactiles et les libelles peuvent revenir a la ligne sans deformer la grille.
- Carrousel vide ou couverture absente: le fond et l'action Decouvrir restent lisibles.
- Light novel avec synopsis absent: la fiche ne reserve pas une grande zone vide.
- Actions optionnelles absentes: les menus de chapitres ne doivent contenir que les callbacks disponibles.

---

### Task 1: Stable UI component policy and M3 loading

**Files:**
- Create: `app/src/test/java/eu/kanade/presentation/theme/NovaUiComponentPolicyTest.kt`
- Modify: `presentation-core/src/main/java/tachiyomi/presentation/core/screens/LoadingScreen.kt`

**Interfaces:**
- Produces: un contrat source empechant le retour des composants de mesure fragiles et un `LoadingScreen` base sur `LoadingIndicator`.

- [ ] **Step 1: Write the failing component policy test**

Ajouter quatre assertions: `MoreScreen.kt` ne contient pas `ButtonGroup`, `NovaMangaScreen.kt` ne contient pas `HorizontalFloatingToolbar`, `LoadingScreen.kt` contient `LoadingIndicator(` et ne contient pas `CircularProgressIndicator`.

- [ ] **Step 2: Run the focused test and verify RED**

Run: `./gradlew :app:testDebugUnitTest --tests eu.kanade.presentation.theme.NovaUiComponentPolicyTest`

Expected: FAIL pour `HorizontalFloatingToolbar` et `CircularProgressIndicator`.

- [ ] **Step 3: Replace the shared indeterminate loader**

Dans `LoadingScreen.kt`, utiliser `androidx.compose.material3.LoadingIndicator` centre, avec `MaterialTheme.colorScheme.primary`. Ne pas modifier les indicateurs determines ailleurs.

- [ ] **Step 4: Keep the policy test red only for the manga toolbar**

Run: meme commande.

Expected: seul le contrat `HorizontalFloatingToolbar` reste en echec jusqu'a la Task 4.

### Task 2: Adaptive Material 3 multi-aspect carousel

**Files:**
- Modify: `app/src/main/java/eu/kanade/presentation/theme/NovaSpatialLayout.kt`
- Modify: `app/src/test/java/eu/kanade/presentation/theme/NovaSpatialLayoutTest.kt`
- Modify: `app/src/main/java/eu/kanade/presentation/library/components/NovaRevResumeCarousel.kt`

**Interfaces:**
- Produces: `fun novaSpatialCarouselHeightDp(widthDp: Int): Int`, `fun novaSpatialCarouselPreferredWidthDp(availableWidthDp: Int, itemCount: Int): Int` et un `HorizontalMultiBrowseCarousel` utilisant ces dimensions.

- [ ] **Step 1: Add failing height tests**

Verifier les hauteurs `320 -> 220`, `380 -> 232`, `700 -> 248`. Pour la largeur dynamique, verifier qu'elle reste dans la largeur utile, qu'un seul element utilise presque toute la largeur, que davantage d'espace revele progressivement plus de cartes et que les entrees nulles ou negatives produisent une valeur sure.

- [ ] **Step 2: Run the focused test and verify RED**

Run: `./gradlew :app:testDebugUnitTest --tests eu.kanade.presentation.theme.NovaSpatialLayoutTest`

Expected: FAIL car `novaSpatialCarouselHeightDp` n'existe pas.

- [ ] **Step 3: Implement the height policy and redesign the card**

Remplacer `nova.cardmotion.CardCarousel` par `androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel` avec `rememberCarouselState`, une `preferredItemWidth` recalculee depuis `BoxWithConstraints.maxWidth`, `itemSpacing`, `contentPadding` et `maskClip`. Utiliser une image plein cadre, un fond colore issu de `item.colors`, un degrade sombre vertical, les metadonnees et l'action de reprise en superposition. Les cartes grandes, moyennes et petites doivent rester lisibles sans texte superpose; conserver swipe, progression, qualite d'image et reduction des animations.

- [ ] **Step 4: Run the focused test and compile**

Run: `./gradlew :app:testDebugUnitTest --tests eu.kanade.presentation.theme.NovaSpatialLayoutTest :app:compileDebugKotlin`

Expected: PASS et `BUILD SUCCESSFUL`.

### Task 3: Material 3 library tabs

**Files:**
- Modify: `app/src/main/java/eu/kanade/presentation/library/NovaLibraryShelves.kt`
- Modify: `app/src/test/java/eu/kanade/presentation/library/NovaLibraryShelvesTest.kt`
- Modify: `app/src/main/java/eu/kanade/presentation/library/components/NovaVikiLibraryScreen.kt`

**Interfaces:**
- Produces: `fun novaLibraryShelfOptions(currentCount: Int, archiveCount: Int, categories: List<NovaLibraryCategory>, categoryCounts: Map<Long, Int>): List<NovaLibraryShelfOption>` utilise par les tabs secondaires.

- [ ] **Step 1: Add failing shelf option tests**

Verifier l'ordre En cours, Archives, puis categories utilisateur; les compteurs associes; l'absence de la categorie reservee Archives dans les categories; et la normalisation des compteurs negatifs a zero.

- [ ] **Step 2: Run the focused test and verify RED**

Run: `./gradlew :app:testDebugUnitTest --tests eu.kanade.presentation.library.NovaLibraryShelvesTest`

Expected: FAIL car le modele d'option n'existe pas.

- [ ] **Step 3: Implement the compact control band**

Remplacer les selecteurs personnalises par un `PrimaryTabRow` pour All Series / Light Novel et un `SecondaryScrollableTabRow` pour En cours / Archives / categories utilisateur. Selectionner une categorie filtre directement la grille sans passer par Collections. Placer le tri dans la zone d'actions sans creer de troisieme rangee; conserver la colonne laterale tablette.

- [ ] **Step 4: Run tests and compile**

Run: `./gradlew :app:testDebugUnitTest --tests eu.kanade.presentation.library.NovaLibraryShelvesTest :app:compileDebugKotlin`

Expected: PASS et `BUILD SUCCESSFUL`.

### Task 4: Compact manga synopsis and chapter header

**Files:**
- Modify: `app/src/main/java/eu/kanade/presentation/manga/NovaMangaModels.kt`
- Modify: `app/src/test/java/eu/kanade/presentation/manga/NovaMangaModelsTest.kt`
- Modify: `app/src/main/java/eu/kanade/presentation/manga/NovaMangaScreen.kt`

**Interfaces:**
- Produces: `fun novaChapterHeaderSummary(chapterCount: Int, visibleChapterCount: Int, unreadCount: Int, filterActive: Boolean): String`.

- [ ] **Step 1: Add failing chapter summary tests**

Verifier les textes avec filtre actif, sans filtre, zero chapitre et compteurs negatifs normalises.

- [ ] **Step 2: Run the focused test and verify RED**

Run: `./gradlew :app:testDebugUnitTest --tests eu.kanade.presentation.manga.NovaMangaModelsTest`

Expected: FAIL car `novaChapterHeaderSummary` n'existe pas.

- [ ] **Step 3: Compact the hero and synopsis**

Reduire la hauteur fixe du hero, retirer le synopsis du panneau en verre, puis afficher sous le hero une zone a deux lignes avec action Plus / Moins et `animateContentSize` base sur le motion scheme. Ne pas afficher cette zone quand le synopsis est vide.

- [ ] **Step 4: Replace the chapter floating toolbar**

Construire une seule `Row`: titre et resume a gauche; Telecharger, Filtrer et Plus a droite. Placer les actions globales lu/non lu dans Plus et conserver les menus/callbacks existants.

- [ ] **Step 5: Run the policy and manga tests**

Run: `./gradlew :app:testDebugUnitTest --tests eu.kanade.presentation.theme.NovaUiComponentPolicyTest --tests eu.kanade.presentation.manga.NovaMangaModelsTest`

Expected: PASS.

### Task 5: Material 3 tabs for Moi

**Files:**
- Modify: `app/src/main/java/eu/kanade/presentation/more/MoreScreen.kt`
- Modify: `app/src/test/java/eu/kanade/presentation/home/NovaDestinationContentTest.kt`

**Interfaces:**
- Produces: `enum class NovaMeCategory` et `fun novaMeCategories(): List<NovaMeCategory>`.

- [ ] **Step 1: Add failing category tests**

Verifier l'ordre Apercu, Lecture, Bibliotheque, Application et la conservation de Bibliotheque meme quand les outils avances sont absents.

- [ ] **Step 2: Run the focused test and verify RED**

Run: `./gradlew :app:testDebugUnitTest --tests eu.kanade.presentation.home.NovaDestinationContentTest`

Expected: FAIL car les categories Moi n'existent pas.

- [ ] **Step 3: Implement the categorized Moi screen**

Ajouter un `PrimaryScrollableTabRow` sous `MediumTopAppBar`, memoriser le tab avec `rememberSaveable`, et n'afficher dans la `LazyColumn` que les sections de la categorie active. Les acces Categories et Parametres restent disponibles dans leurs categories respectives.

- [ ] **Step 4: Run tests and compile**

Run: `./gradlew :app:testDebugUnitTest --tests eu.kanade.presentation.home.NovaDestinationContentTest :app:compileDebugKotlin`

Expected: PASS et `BUILD SUCCESSFUL`.

### Task 6: Directional wave navigation feedback

**Files:**
- Modify: `app/src/main/java/eu/kanade/presentation/home/NovaNavigation.kt`
- Modify: `app/src/main/java/eu/kanade/tachiyomi/ui/home/HomeScreen.kt`
- Modify: `app/src/test/java/eu/kanade/presentation/home/NovaNavigationTest.kt`

**Interfaces:**
- Produces: `fun resolveNovaNavigationDirection(previousIndex: Int, currentIndex: Int): Int` et un indicateur actif deformable partage par la barre inferieure et le rail.

- [ ] **Step 1: Verify direction in a focused unit test**

Verifier les transitions vers la gauche, la droite et la reselection sans mouvement.

- [ ] **Step 2: Animate only the active indicator**

Conserver les composants M3 `NavigationBarItem` et `NavigationRailItem`, rendre leur indicateur natif transparent, puis dessiner un indicateur local qui s'etire dans la direction du changement et revient a sa forme stable avec le motion scheme M3. L'icone et le texte ne se deplacent pas.

- [ ] **Step 3: Run navigation tests**

Run: `./gradlew :app:testDebugUnitTest --tests eu.kanade.presentation.home.NovaNavigationTest`

Expected: PASS.

### Task 7: Full verification

**Files:**
- Verify all modified files from Tasks 1-5.

**Interfaces:**
- Produces: un changement compilable et publiable, sans APK tant que l'utilisateur ne l'a pas autorise.

- [ ] **Step 1: Check forbidden components**

Run: `rg -n "ButtonGroup|ToggleButtonGroup|HorizontalFloatingToolbar" app/src/main presentation-core/src/main`

Expected: aucune occurrence dans les surfaces modifiees.

- [ ] **Step 2: Run formatting checks for touched files**

Run: `./gradlew :app:spotlessKotlinCheck --no-problems-report`

Expected: les fichiers touches ne figurent dans aucune erreur; documenter les erreurs preexistantes ailleurs.

- [ ] **Step 3: Run the complete unit suite**

Run: `./gradlew :app:testDebugUnitTest --no-problems-report`

Expected: `BUILD SUCCESSFUL`, aucun test en echec.

- [ ] **Step 4: Inspect the final diff**

Run: `git diff --check`

Expected: aucune erreur.

- [ ] **Step 5: Ask before APK build**

Ne lancer aucune tache `assemble` avant l'autorisation explicite de l'utilisateur.
