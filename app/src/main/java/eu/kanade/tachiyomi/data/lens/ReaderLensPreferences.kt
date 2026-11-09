package eu.kanade.tachiyomi.data.lens

import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore

class ReaderLensPreferences(
    preferenceStore: PreferenceStore,
) {
    val enabled = preferenceStore.getBoolean("reader_lens_enabled", true)

    init {
        preferenceStore.getString("manga_translator_server_url", "").delete()
        preferenceStore.getString(Preference.privateKey("manga_translator_access_token"), "").delete()
        preferenceStore.getString(Preference.appStateKey("manga_translator_pending_jobs"), "").delete()
    }
}
