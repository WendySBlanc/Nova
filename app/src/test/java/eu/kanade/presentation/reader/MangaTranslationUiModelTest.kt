package eu.kanade.presentation.reader

import eu.kanade.tachiyomi.data.translation.MangaTranslationState
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class MangaTranslationUiModelTest {

    @Test
    fun `text chapters expose no translation action`() {
        mangaTranslationUiModel(
            available = false,
            configured = true,
            state = MangaTranslationState.Idle,
            hasTranslation = false,
            showingTranslation = false,
        ) shouldBe null
    }

    @Test
    fun `idle image chapter exposes translate action and description`() {
        val model = model(state = MangaTranslationState.Idle)

        model.action shouldBe MangaTranslationAction.Translate
        model.enabled shouldBe true
        model.label shouldBe MangaTranslationUiText.TranslateChapter
        model.contentDescription shouldBe MangaTranslationUiText.TranslateChapterDescription
    }

    @Test
    fun `translation action is disabled when server configuration is incomplete`() {
        val model = model(
            state = MangaTranslationState.Idle,
            configured = false,
        )

        model.action shouldBe MangaTranslationAction.Translate
        model.enabled shouldBe false
        model.contentDescription shouldBe MangaTranslationUiText.ConfigureServerDescription
    }

    @Test
    fun `preparing chapter exposes cancellable indeterminate progress`() {
        val model = model(state = MangaTranslationState.Preparing)

        model.action shouldBe MangaTranslationAction.Cancel
        model.label shouldBe MangaTranslationUiText.TranslationInProgress
        model.contentDescription shouldBe MangaTranslationUiText.CancelTranslationDescription
        model.completedPages shouldBe null
        model.totalPages shouldBe null
    }

    @Test
    fun `running chapter exposes cancellable page progress`() {
        val model = model(state = MangaTranslationState.Translating(4, 20))

        model.action shouldBe MangaTranslationAction.Cancel
        model.label shouldBe MangaTranslationUiText.TranslationProgress
        model.contentDescription shouldBe MangaTranslationUiText.CancelTranslationProgressDescription
        model.completedPages shouldBe 4
        model.totalPages shouldBe 20
    }

    @Test
    fun `cached translation can be shown`() {
        val model = model(
            state = MangaTranslationState.Idle,
            hasTranslation = true,
            showingTranslation = false,
            configured = false,
        )

        model.action shouldBe MangaTranslationAction.ShowTranslation
        model.enabled shouldBe true
        model.label shouldBe MangaTranslationUiText.ShowTranslation
        model.contentDescription shouldBe MangaTranslationUiText.ShowTranslationDescription
    }

    @Test
    fun `visible translation can switch back to original`() {
        val model = model(
            state = MangaTranslationState.Idle,
            hasTranslation = true,
            showingTranslation = true,
        )

        model.action shouldBe MangaTranslationAction.ShowOriginal
        model.label shouldBe MangaTranslationUiText.ShowOriginal
        model.contentDescription shouldBe MangaTranslationUiText.ShowOriginalDescription
    }

    @Test
    fun `failed translation can be started again`() {
        val model = model(
            state = MangaTranslationState.Failed("offline", canRetry = true),
        )

        model.action shouldBe MangaTranslationAction.Translate
        model.label shouldBe MangaTranslationUiText.TranslateChapter
    }

    private fun model(
        state: MangaTranslationState,
        hasTranslation: Boolean = false,
        showingTranslation: Boolean = false,
        configured: Boolean = true,
    ) = mangaTranslationUiModel(
        available = true,
        configured = configured,
        state = state,
        hasTranslation = hasTranslation,
        showingTranslation = showingTranslation,
    )!!
}
