package eu.kanade.tachiyomi.ui.reader.viewer.lnreader

import eu.kanade.tachiyomi.data.lens.ReaderLensRegion
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal fun buildReaderLensOverlayScript(
    pageIndex: Int,
    regions: List<ReaderLensRegion>,
): String {
    val encodedRegions = Json.encodeToString(regions)
        .replace("<", "\\u003c")
        .replace(">", "\\u003e")
        .replace("&", "\\u0026")
    return """
        (function(regions) {
          var figure = document.querySelector('.nova-reader-page[data-page-index="$pageIndex"]');
          if (!figure) return;
          var previous = figure.querySelector('.nova-lens-text-layer');
          if (previous) previous.remove();
          var layer = document.createElement('div');
          layer.className = 'nova-lens-text-layer';
          layer.setAttribute('aria-label', 'Traduction Lens');
          regions.forEach(function(region) {
            var block = document.createElement('span');
            block.className = 'nova-lens-text';
            block.textContent = region.text;
            block.style.left = (region.left * 100) + '%';
            block.style.top = (region.top * 100) + '%';
            block.style.width = (region.width * 100) + '%';
            block.style.height = (region.height * 100) + '%';
            layer.appendChild(block);
          });
          figure.appendChild(layer);
          requestAnimationFrame(function() { layer.classList.add('nova-lens-visible'); });
        })($encodedRegions);
    """.trimIndent()
}

internal const val CLEAR_READER_LENS_OVERLAYS_SCRIPT =
    "document.querySelectorAll('.nova-lens-text-layer').forEach(function(layer) { layer.remove(); });"
