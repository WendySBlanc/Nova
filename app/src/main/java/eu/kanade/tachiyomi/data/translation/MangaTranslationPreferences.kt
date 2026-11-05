package eu.kanade.tachiyomi.data.translation

import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore

class MangaTranslationPreferences(
    private val preferenceStore: PreferenceStore,
) {
    fun serverUrl() = preferenceStore.getString(SERVER_URL_KEY, "")

    fun accessToken() = preferenceStore.getString(Preference.privateKey(ACCESS_TOKEN_KEY), "")

    fun connection(): MangaTranslationConnection = MangaTranslationConnection(
        baseUrl = serverUrl().get(),
        accessToken = accessToken().get(),
    )

    companion object {
        private const val SERVER_URL_KEY = "manga_translator_server_url"
        private const val ACCESS_TOKEN_KEY = "manga_translator_access_token"
    }
}
