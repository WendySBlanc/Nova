package eu.kanade.tachiyomi.ui.reader

import org.jsoup.Jsoup

private const val BLOCKED_READER_ELEMENTS =
    "script, iframe, frame, frameset, object, embed, form, input, button, textarea, select, option, " +
        "link, meta, base, applet, svg, math"

private val readerUrlAttributes = setOf(
    "href",
    "src",
    "action",
    "formaction",
    "poster",
    "background",
    "xlink:href",
)

internal fun String.sanitizeReaderContentHtml(): String {
    val body = Jsoup.parseBodyFragment(this).body()
    body.select(BLOCKED_READER_ELEMENTS).remove()

    body.allElements.forEach { element ->
        element.attributes().asList().map { it.key }.forEach { attributeName ->
            val normalizedName = attributeName.lowercase()
            val shouldRemove = when {
                normalizedName.startsWith("on") -> true
                normalizedName == "style" || normalizedName == "srcdoc" -> true
                normalizedName in readerUrlAttributes -> {
                    element.attr(attributeName).isUnsafeReaderUrl(normalizedName)
                }
                else -> false
            }
            if (shouldRemove) {
                element.removeAttr(attributeName)
            }
        }
    }

    return body.html()
}

private fun String.isUnsafeReaderUrl(attributeName: String): Boolean {
    val normalizedUrl = trim()
        .lowercase()
        .filterNot { it <= ' ' }
    if (normalizedUrl.startsWith("data:")) {
        return attributeName != "src" || SAFE_READER_DATA_IMAGE_REGEX.find(normalizedUrl) == null
    }
    return UNSAFE_READER_URL_SCHEMES.any(normalizedUrl::startsWith)
}

private val UNSAFE_READER_URL_SCHEMES = listOf(
    "javascript:",
    "vbscript:",
    "file:",
    "content:",
    "intent:",
)

private val SAFE_READER_DATA_IMAGE_REGEX =
    Regex("^data:image/(png|jpe?g|gif|webp|avif);base64,")
