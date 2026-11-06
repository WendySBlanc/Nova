package eu.kanade.presentation.home

import eu.kanade.tachiyomi.ui.activity.NovaActivitySection
import io.kotest.matchers.collections.shouldContainExactly
import org.junit.jupiter.api.Test

class NovaDestinationContentTest {

    @Test
    fun `discover keeps sources extensions and migration in stable order`() {
        novaDiscoverSections(feedEnabled = false, feedFirst = false) shouldContainExactly listOf(
            NovaDiscoverSection.Sources,
            NovaDiscoverSection.Extensions,
            NovaDiscoverSection.Migration,
        )
        novaDiscoverSections(feedEnabled = true, feedFirst = false) shouldContainExactly listOf(
            NovaDiscoverSection.Sources,
            NovaDiscoverSection.Feed,
            NovaDiscoverSection.Extensions,
            NovaDiscoverSection.Migration,
        )
    }

    @Test
    fun `activity owns all reading activity destinations`() {
        NovaActivitySection.entries shouldContainExactly listOf(
            NovaActivitySection.Updates,
            NovaActivitySection.Downloads,
            NovaActivitySection.History,
            NovaActivitySection.Problems,
        )
    }

    @Test
    fun `me quick routes do not duplicate updates or history`() {
        novaMeQuickRoutes() shouldContainExactly listOf(
            NovaMeQuickRoute.Downloads,
            NovaMeQuickRoute.Categories,
            NovaMeQuickRoute.Statistics,
            NovaMeQuickRoute.Settings,
        )
    }

    @Test
    fun `me settings are grouped into four stable categories`() {
        novaMeCategories() shouldContainExactly listOf(
            NovaMeCategory.Overview,
            NovaMeCategory.Reading,
            NovaMeCategory.Library,
            NovaMeCategory.Application,
        )
    }
}
