package eu.kanade.presentation.library

import eu.kanade.domain.ui.model.NovaLibraryPresentation
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class NovaLibraryCatalogTest {
    @Test
    fun `phone gallery uses three compact columns`() {
        val gallery = novaCatalogMetrics(416, NovaLibraryPresentation.Grid)

        gallery.columns shouldBe 3
        gallery.sidePadding shouldBe 16
        gallery.columnGap shouldBe 10
        gallery.rowGap shouldBe 10
    }

    @Test
    fun `list and large cards form single columns on a phone`() {
        novaCatalogMetrics(432, NovaLibraryPresentation.List).columns shouldBe 1
        novaCatalogMetrics(432, NovaLibraryPresentation.Cards).columns shouldBe 1
    }

    @Test
    fun `gallery remains three across on tablets`() {
        val gallery = novaCatalogMetrics(920, NovaLibraryPresentation.Grid)

        gallery.columns shouldBe 3
        gallery.sidePadding shouldBe 20
        novaCatalogMetrics(920, NovaLibraryPresentation.Cards).columns shouldBe 2
        novaCatalogMetrics(920, NovaLibraryPresentation.List).columns shouldBe 1
    }

    @Test
    fun `compact split screen still leaves room for covers`() {
        val gallery = novaCatalogMetrics(270, NovaLibraryPresentation.Grid)

        gallery.columns shouldBe 3
        gallery.sidePadding shouldBe 12
        gallery.columnGap shouldBe 7
    }
}
