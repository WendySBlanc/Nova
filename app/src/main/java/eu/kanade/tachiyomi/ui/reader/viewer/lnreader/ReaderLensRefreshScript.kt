package eu.kanade.tachiyomi.ui.reader.viewer.lnreader

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal data class ReaderLensPageSource(
    val url: String,
    val translated: Boolean,
)

internal fun buildReaderLensRefreshScript(pageSources: Map<Int, ReaderLensPageSource>): String {
    val sources = buildJsonObject {
        pageSources.forEach { (pageIndex, source) ->
            put(
                pageIndex.toString(),
                buildJsonObject {
                    put("url", source.url)
                    put("translated", source.translated)
                },
            )
        }
    }

    return """
        (function(sources) {
          var figures = Array.prototype.slice.call(document.querySelectorAll('.nova-reader-page'));
          var anchor = figures.find(function(figure) {
            return figure.getBoundingClientRect().bottom > 0;
          });
          var anchorPageIndex = anchor ? anchor.dataset.pageIndex : null;
          var anchorOffset = anchor ? anchor.getBoundingClientRect().top : 0;

          Object.keys(sources).forEach(function(pageIndex) {
            var source = sources[pageIndex];
            var figure = document.querySelector('.nova-reader-page[data-page-index="' + pageIndex + '"]');
            var baseImage = figure && figure.querySelector('img:not(.nova-lens-overlay)');
            if (!figure || !baseImage) return;

            var overlay = figure.querySelector('.nova-lens-overlay');
            if (!source.translated) {
              if (overlay) overlay.remove();
              baseImage.dataset.originalSrc = source.url;
              if (baseImage.src !== source.url) baseImage.src = source.url;
              return;
            }

            if (!overlay) {
              overlay = document.createElement('img');
              overlay.className = 'nova-lens-overlay';
              overlay.alt = '';
              overlay.decoding = 'async';
              overlay.loading = 'eager';
              overlay.setAttribute('aria-hidden', 'true');
              overlay.addEventListener('load', function() {
                overlay.classList.add('nova-lens-visible');
                figure.classList.remove('nova-lens-pending', 'nova-image-error');
              }, { passive: true });
              overlay.addEventListener('error', function() {
                overlay.remove();
                figure.classList.remove('nova-lens-pending');
              }, { passive: true });
              figure.appendChild(overlay);
            }

            figure.classList.add('nova-lens-pending');
            overlay.classList.remove('nova-lens-visible');
            overlay.dataset.originalSrc = source.url;
            if (overlay.src === source.url && overlay.complete && overlay.naturalWidth > 0) {
              overlay.classList.add('nova-lens-visible');
              figure.classList.remove('nova-lens-pending', 'nova-image-error');
            } else {
              overlay.src = source.url;
            }
          });

          requestAnimationFrame(function() {
            if (anchorPageIndex === null) return;
            var updatedAnchor = document.querySelector(
              '.nova-reader-page[data-page-index="' + anchorPageIndex + '"]'
            );
            if (!updatedAnchor) return;
            window.scrollBy(0, updatedAnchor.getBoundingClientRect().top - anchorOffset);
          });
        })($sources);
    """.trimIndent()
}
