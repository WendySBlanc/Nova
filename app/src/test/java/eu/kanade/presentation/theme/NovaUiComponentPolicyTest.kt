package eu.kanade.presentation.theme

import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.junit.jupiter.api.Test
import java.io.File

class NovaUiComponentPolicyTest {

    @Test
    fun `compact surfaces avoid fragile measurement components`() {
        source("app/src/main/java/eu/kanade/presentation/more/MoreScreen.kt")
            .shouldNotContain("ButtonGroup(")
        source("app/src/main/java/eu/kanade/presentation/manga/NovaMangaScreen.kt")
            .shouldNotContain("HorizontalFloatingToolbar(")
    }

    @Test
    fun `shared indeterminate loading uses Material 3 expressive indicator`() {
        source("presentation-core/src/main/java/tachiyomi/presentation/core/screens/LoadingScreen.kt")
            .shouldContain("LoadingIndicator(")
        source("presentation-core/src/main/java/tachiyomi/presentation/core/screens/LoadingScreen.kt")
            .shouldNotContain("CircularProgressIndicator")
    }

    @Test
    fun `reader webview uses an isolated origin and non blocking image retries`() {
        val reader = source(
            "app/src/main/java/eu/kanade/tachiyomi/ui/reader/viewer/lnreader/LnReaderWebViewViewer.kt",
        )

        reader.shouldContain("allowFileAccess = false")
        reader.shouldContain("Content-Security-Policy")
        reader.shouldContain("https://nova.local/assets/")
        reader.shouldNotContain("allowUniversalAccessFromFileURLs = true")
        reader.shouldNotContain("file:///android_asset/")
        reader.shouldNotContain("Thread.sleep")
    }

    @Test
    fun `reader Lens is available early and placed in the bottom reading dock`() {
        val viewModel = source("app/src/main/java/eu/kanade/tachiyomi/ui/reader/ReaderViewModel.kt")
        val topBar = source("app/src/main/java/eu/kanade/presentation/reader/appbars/ReaderTopBar.kt")
        val bottomBar = source("app/src/main/java/eu/kanade/presentation/reader/appbars/ReaderBottomBar.kt")

        viewModel.shouldContain("shouldOfferReaderLens(readerLensPages(readerChapter).size)")
        topBar.shouldNotContain("readerLensAvailable")
        topBar.shouldNotContain("Icons.Outlined.Translate")
        bottomBar.shouldContain("readerLensAvailable")
        bottomBar.shouldContain("Icons.Outlined.Translate")
    }

    @Test
    fun `reader Lens replaces dialogue without floating cards`() {
        val reader = source(
            "app/src/main/java/eu/kanade/tachiyomi/ui/reader/viewer/lnreader/LnReaderWebViewViewer.kt",
        )

        reader.shouldContain("background: rgb(248, 248, 246)")
        reader.shouldNotContain("box-shadow: 0 1px 5px rgba(0, 0, 0, 0.24)")
    }

    @Test
    fun `me exposes settings directly instead of opening a settings hub`() {
        val moreScreen = source("app/src/main/java/eu/kanade/presentation/more/MoreScreen.kt")
        val moreTab = source("app/src/main/java/eu/kanade/tachiyomi/ui/more/MoreTab.kt")

        moreScreen.shouldContain("NovaMeSettingDestination.Appearance")
        moreScreen.shouldContain("NovaMeSettingDestination.Reader")
        moreScreen.shouldContain("NovaMeSettingDestination.Library")
        moreScreen.shouldContain("NovaMeSettingDestination.Downloads")
        moreScreen.shouldContain("NovaMeSettingDestination.DataAndStorage")
        moreScreen.shouldContain("NovaMeSettingDestination.Advanced")
        moreTab.shouldContain("SettingsScreen(destination.toSettingsDestination())")
        moreTab.shouldNotContain("navigator.push(SettingsScreen())")
    }

    @Test
    fun `settings search keeps a visible route back to settings`() {
        val settingsSearch = source(
            "app/src/main/java/eu/kanade/presentation/more/settings/screen/SettingsSearchScreen.kt",
        )

        settingsSearch.shouldContain("val backPress = LocalBackPress.current")
        settingsSearch.shouldContain("backPress?.invoke()")
    }

    @Test
    fun `settings destination is named settings everywhere`() {
        val moreScreen = source("app/src/main/java/eu/kanade/presentation/more/MoreScreen.kt")
        val moreTab = source("app/src/main/java/eu/kanade/tachiyomi/ui/more/MoreTab.kt")

        moreScreen.shouldContain("text = \"Param\\u00e8tres\"")
        moreScreen.shouldNotContain("text = \"Moi\"")
        moreTab.shouldContain("title = \"Param\\u00e8tres\"")
        moreTab.shouldNotContain("title = \"Moi\"")
    }

    @Test
    fun `library counts use the shared animated number primitive`() {
        source("app/src/main/java/eu/kanade/presentation/library/components/NovaVikiLibraryScreen.kt")
            .shouldContain("NovaCountBadge(")
    }

    @Test
    fun `nova manga messages use the shared notification host`() {
        val mangaScreen = source("app/src/main/java/eu/kanade/presentation/manga/NovaMangaScreen.kt")

        mangaScreen.shouldContain("NovaSnackbarHost(")
        mangaScreen.shouldContain("NovaCountBadge(")
        mangaScreen.shouldContain("NovaMenuItem(")
        mangaScreen.shouldContain("NovaChapterSummary(")
    }

    @Test
    fun `source search is backed by the shared command field`() {
        source("app/src/main/java/eu/kanade/presentation/components/SourceSearchBox.kt")
            .shouldContain("NovaCommandField(")
    }

    @Test
    fun `global search shares bounded IO workers instead of leaking a thread pool`() {
        val searchModel = source(
            "app/src/main/java/eu/kanade/tachiyomi/ui/browse/source/globalsearch/SearchScreenModel.kt",
        )

        searchModel.shouldContain("Dispatchers.IO.limitedParallelism(SEARCH_PARALLELISM)")
        searchModel.shouldContain("SEARCH_PARALLELISM = 16")
        searchModel.shouldNotContain("Executors.newFixedThreadPool")
    }

    @Test
    fun `global search merges concurrent source results atomically`() {
        val searchModel = source(
            "app/src/main/java/eu/kanade/tachiyomi/ui/browse/source/globalsearch/SearchScreenModel.kt",
        )

        searchModel.shouldContain("mutableState.update { currentState ->")
        searchModel.shouldContain("currentState.items.mutate")
        searchModel.shouldNotContain("val newItems = state.value.items.mutate")
    }

    private fun source(relativePath: String): String = File(repositoryRoot(), relativePath).readText()

    private fun repositoryRoot(): File {
        val workingDirectory = requireNotNull(System.getProperty("user.dir"))
        var candidate = File(workingDirectory).canonicalFile
        while (!File(candidate, "settings.gradle.kts").isFile) {
            candidate = candidate.parentFile
                ?: error("Unable to locate repository root from $workingDirectory")
        }
        return candidate
    }
}
