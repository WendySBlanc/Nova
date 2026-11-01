package eu.kanade.presentation.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import coil3.request.ImageRequest
import coil3.request.allowRgb565
import coil3.size.Precision

@Composable
fun rememberHighQualityImageRequest(
    data: Any?,
    widthPx: Int? = null,
    heightPx: Int? = null,
    exactSize: Boolean = false,
): ImageRequest {
    val context = LocalContext.current
    return remember(context, data, widthPx, heightPx, exactSize) {
        val builder = if (data is ImageRequest) {
            data.newBuilder(context)
        } else {
            ImageRequest.Builder(context).data(data)
        }

        builder.allowRgb565(false)
        if (widthPx != null && heightPx != null) {
            builder.size(widthPx, heightPx)
            if (exactSize) {
                builder.precision(Precision.EXACT)
            }
        }
        builder.build()
    }
}
