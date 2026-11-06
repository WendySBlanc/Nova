package eu.kanade.tachiyomi.data.translation

import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.core.common.preference.Preference

class MangaTranslationJobStoreTest {

    @Test
    fun `legacy job ids remain readable after the structured store migration`() {
        val store = storeWith("""{"chapter":"legacy-job"}""")

        store.get("chapter") shouldBe PendingMangaTranslationJob(jobId = "legacy-job")
    }

    @Test
    fun `page replacement index survives persistence`() {
        val preferenceStore = InMemoryPreferenceStore(
            sequenceOf(pendingJobsPreference("")),
        )
        val preferences = MangaTranslationPreferences(preferenceStore)
        val first = PreferenceMangaTranslationJobStore(preferences, Json)
        val pending = PendingMangaTranslationJob(jobId = "page-job", replacementPageIndex = 7)

        first.put("chapter", pending)

        first.get("chapter") shouldBe pending
    }

    private fun storeWith(value: String): PreferenceMangaTranslationJobStore {
        val preferenceStore = InMemoryPreferenceStore(
            sequenceOf(pendingJobsPreference(value)),
        )
        return PreferenceMangaTranslationJobStore(MangaTranslationPreferences(preferenceStore), Json)
    }

    private fun pendingJobsPreference(value: String) = InMemoryPreferenceStore.InMemoryPreference(
        key = Preference.appStateKey("manga_translator_pending_jobs"),
        data = value,
        defaultValue = "",
    )
}
