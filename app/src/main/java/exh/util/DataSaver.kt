package exh.util

import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.domain.source.service.SourcePreferences.DataSaver.BANDWIDTH_HERO
import eu.kanade.domain.source.service.SourcePreferences.DataSaver.NONE
import eu.kanade.domain.source.service.SourcePreferences.DataSaver.WSRV_NL
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.online.HttpSource
import kotlinx.coroutines.CancellationException
import okhttp3.Response
import tachiyomi.core.common.preference.Preference

interface DataSaver {

    fun compress(imageUrl: String): String

    companion object {
        val NoOp = object : DataSaver {
            override fun compress(imageUrl: String): String {
                return imageUrl
            }
        }

        suspend fun HttpSource.getImage(page: Page, dataSaver: DataSaver, existingSize: Long = 0L): Response {
            val imageUrl = page.imageUrl ?: return getImage(page, existingSize)
            if (imageUrl.shouldBypassDataSaver()) {
                return getImage(page, existingSize)
            }
            val compressedImageUrl = dataSaver.compress(imageUrl)
            if (compressedImageUrl == imageUrl) {
                return getImage(page, existingSize)
            }
            page.imageUrl = compressedImageUrl
            return try {
                // A proxy can produce different bytes than the original image. Start proxy
                // downloads from zero so falling back to the source can never mix both payloads.
                getImage(page, 0L)
            } catch (e: Throwable) {
                if (e is CancellationException) {
                    throw e
                }
                page.imageUrl = imageUrl
                getImage(page, 0L)
            } finally {
                page.imageUrl = imageUrl
            }
        }

        private fun String.shouldBypassDataSaver(): Boolean {
            return startsWith("file:", ignoreCase = true) ||
                startsWith("content:", ignoreCase = true) ||
                startsWith("data:", ignoreCase = true) ||
                contains("127.0.0.1", ignoreCase = true) ||
                contains("localhost", ignoreCase = true) ||
                contains("/novel-france-rendered-page.png", ignoreCase = true)
        }
    }
}

fun DataSaver(source: Source, preferences: SourcePreferences): DataSaver {
    val dataSaver = preferences.dataSaver().get()
    if (dataSaver != NONE && source.id.toString() in preferences.dataSaverExcludedSources().get()) {
        return DataSaver.NoOp
    }
    return when (dataSaver) {
        NONE -> DataSaver.NoOp
        BANDWIDTH_HERO -> BandwidthHeroDataSaver(preferences)
        WSRV_NL -> WsrvNlDataSaver(preferences)
    }
}

private class BandwidthHeroDataSaver(preferences: SourcePreferences) : DataSaver {
    private val dataSavedServer = preferences.dataSaverServer().get().trimEnd('/')

    private val ignoreJpg = preferences.dataSaverIgnoreJpeg().get()
    private val ignoreGif = preferences.dataSaverIgnoreGif().get()

    private val format = preferences.dataSaverImageFormatJpeg().toIntRepresentation()
    private val quality = preferences.dataSaverImageQuality().get()
    private val colorBW = preferences.dataSaverColorBW().toIntRepresentation()

    override fun compress(imageUrl: String): String {
        return if (dataSavedServer.isNotBlank() && !imageUrl.contains(dataSavedServer)) {
            when {
                imageUrl.contains(".jpeg", true) || imageUrl.contains(".jpg", true) -> if (ignoreJpg) imageUrl else getUrl(imageUrl)
                imageUrl.contains(".gif", true) -> if (ignoreGif) imageUrl else getUrl(imageUrl)
                else -> getUrl(imageUrl)
            }
        } else {
            imageUrl
        }
    }

    private fun getUrl(imageUrl: String): String {
        // Network Request sent for the Bandwidth Hero Proxy server
        return "$dataSavedServer/?jpg=$format&l=$quality&bw=$colorBW&url=$imageUrl"
    }

    private fun Preference<Boolean>.toIntRepresentation() = if (get()) "1" else "0"
}

private class WsrvNlDataSaver(preferences: SourcePreferences) : DataSaver {
    private val ignoreJpg = preferences.dataSaverIgnoreJpeg().get()
    private val ignoreGif = preferences.dataSaverIgnoreGif().get()

    private val format = preferences.dataSaverImageFormatJpeg().get()
    private val quality = preferences.dataSaverImageQuality().get()

    override fun compress(imageUrl: String): String {
        return when {
            imageUrl.contains(".jpeg", true) || imageUrl.contains(".jpg", true) -> if (ignoreJpg) imageUrl else getUrl(imageUrl)
            imageUrl.contains(".gif", true) -> if (ignoreGif) imageUrl else getUrl(imageUrl)
            else -> getUrl(imageUrl)
        }
    }

    private fun getUrl(imageUrl: String): String {
        // Network Request sent to wsrv
        return "https://wsrv.nl/?url=$imageUrl" +
            if (imageUrl.contains(".webp", true) || imageUrl.contains(".gif", true)) {
                if (!format) {
                    // Preserve output image extension for animated images(.webp and .gif)
                    "&q=$quality&n=-1"
                } else {
                    // Do not preserve output Extension if User asked to convert into Jpeg
                    "&output=jpg&q=$quality&n=-1"
                }
            } else {
                if (format) {
                    "&output=jpg&q=$quality"
                } else {
                    "&output=webp&q=$quality"
                }
            }
    }
}
