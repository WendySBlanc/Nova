package eu.kanade.tachiyomi.ui.reader.viewer.lnreader

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.view.GestureDetector
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.ui.reader.loader.HttpPageLoader
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ViewerChapters
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import eu.kanade.tachiyomi.ui.reader.viewer.Viewer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import logcat.LogPriority
import org.json.JSONArray
import org.json.JSONObject
import tachiyomi.core.common.util.system.logcat
import tachiyomi.decoder.ImageDecoder
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.Locale

class LnReaderWebViewViewer(
    private val activity: ReaderActivity,
    private val readerPreferences: ReaderPreferences = Injekt.get(),
) : Viewer {

    private val scope = MainScope()
    private var viewerChapters: ViewerChapters? = null
    private var currentChapter: ReaderChapter? = null
    private var autoStartTtsOnNextLoad = false
    private var lastSettingsPayload: String? = null
    private var ttsQueue = emptyList<String>()
    private var ttsQueueIndex = 0
    private var ttsQueueTotal = 0
    private var isTtsReading = false
    private var currentChapterUsesImages = false
    val isImageChapter: Boolean
        get() = currentChapterUsesImages
    private var ttsReady = false
    private var chapterLoadJob: Job? = null
    private var chapterLoadGeneration = 0L
    private var chapterNavigationInFlight = false
    private var resetProgressOnNextChapterLoad = false
    private var resetScrollAfterPageLoad = false
    private var hasShownImageReaderChrome = false
    private val preloadedChapterHtml = linkedMapOf<String, String>()
    private val preloadingChapterKeys = mutableSetOf<String>()
    private lateinit var tts: TextToSpeech

    private val imageReaderGestureDetector = GestureDetector(
        activity,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(event: MotionEvent): Boolean = true

            override fun onSingleTapConfirmed(event: MotionEvent): Boolean {
                if (!currentChapterUsesImages) return false
                activity.toggleMenu()
                return true
            }
        },
    )

    private val webView = object : WebView(activity) {
        override fun dispatchTouchEvent(event: MotionEvent): Boolean {
            if (currentChapterUsesImages) {
                imageReaderGestureDetector.onTouchEvent(event)
            }
            return super.dispatchTouchEvent(event)
        }
    }.apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )
        isFocusable = true
        isFocusableInTouchMode = true
        isVerticalScrollBarEnabled = false
        isHorizontalScrollBarEnabled = false
    }

    private val ttsMediaController = LnReaderTtsMediaController(
        context = activity.applicationContext,
        callbacks = object : LnReaderTtsMediaController.Callbacks {
            override fun onPlay() {
                injectTtsScript(
                    """
                        if (!tts.reading) { tts.resume(); }
                        var controller = document.getElementById('TTS-Controller');
                        if (controller && controller.firstElementChild) {
                          controller.firstElementChild.innerHTML = pauseIcon;
                        }
                    """.trimIndent(),
                )
            }

            override fun onPause() {
                injectTtsScript(
                    """
                        if (tts.reading) { tts.pause(); }
                        var controller = document.getElementById('TTS-Controller');
                        if (controller && controller.firstElementChild) {
                          controller.firstElementChild.innerHTML = resumeIcon;
                        }
                    """.trimIndent(),
                )
            }

            override fun onStop() {
                injectTtsScript("tts.stop();")
            }

            override fun onPrevious() {
                injectTtsScript(
                    "if (reader.prevChapter) { reader.post({ type: 'prev', autoStartTTS: true }); }",
                )
            }

            override fun onNext() {
                injectTtsScript(
                    "if (reader.nextChapter) { reader.post({ type: 'next', autoStartTTS: true }); }",
                )
            }

            override fun onRewind() {
                injectTtsScript("tts.rewind();")
            }

            override fun onSeekTo(index: Int) {
                injectTtsScript("tts.seekTo(${index.coerceAtLeast(0)});")
            }
        },
    )

    init {
        tts = TextToSpeech(activity.applicationContext) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
            if (ttsReady && ::tts.isInitialized) {
                tts.language = Locale.getDefault()
            }
        }.apply {
            setOnUtteranceProgressListener(
                object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        isTtsReading = true
                        ttsMediaController.updatePlaybackState(true)
                    }

                    override fun onDone(utteranceId: String?) {
                        onTtsUtteranceFinished()
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        onTtsUtteranceFinished()
                    }
                },
            )
        }
        setupWebView()
        startAutoScrollLoop()
        startSettingsSyncLoop()
        webView.loadDataWithBaseURL(
            ASSET_BASE_URL,
            loadingHtml(),
            "text/html",
            "UTF-8",
            null,
        )
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(false)
        }

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            setSupportZoom(false)
            builtInZoomControls = true
            displayZoomControls = false
            loadWithOverviewMode = true
            useWideViewPort = true
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                allowFileAccessFromFileURLs = true
                allowUniversalAccessFromFileURLs = true
            }
        }
        webView.addJavascriptInterface(Bridge(), "AndroidReader")
        webView.webChromeClient = WebChromeClient()
        webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                return request?.url?.let(::readerImageResponse)
                    ?: super.shouldInterceptRequest(view, request)
            }

            @Deprecated("Deprecated in Java")
            override fun shouldInterceptRequest(view: WebView?, url: String?): WebResourceResponse? {
                return url
                    ?.let { runCatching { Uri.parse(it) }.getOrNull() }
                    ?.let(::readerImageResponse)
                    ?: super.shouldInterceptRequest(view, url)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                chapterNavigationInFlight = false
                setChromeVisible(activity.viewModel.state.value.menuVisible)
                if (resetScrollAfterPageLoad) {
                    resetScrollAfterPageLoad = false
                    view?.evaluateJavascript("window.scrollTo({ top: 0, behavior: 'auto' });", null)
                    view?.postDelayed(
                        {
                            view.evaluateJavascript("window.scrollTo({ top: 0, behavior: 'auto' });", null)
                        },
                        120,
                    )
                }
                if (autoStartTtsOnNextLoad) {
                    autoStartTtsOnNextLoad = false
                    view?.postDelayed(
                        {
                            view.evaluateJavascript(
                                "if (window.tts && reader.generalSettings.val.TTSEnable) { tts.start(); }",
                                null,
                            )
                        },
                        400,
                    )
                }
            }
        }
    }

    private fun readerImageResponse(uri: Uri): WebResourceResponse? {
        if (uri.scheme != "https" || uri.host != NOVA_READER_IMAGE_HOST) return null
        val segments = uri.pathSegments.takeIf { it.firstOrNull() == NOVA_READER_IMAGE_PATH }
            ?: return null
        val chapterKey = segments.getOrNull(1)
            ?: return emptyReaderImageResponse(400)
        if (chapterKey != currentChapter?.imageUrlChapterKey()) {
            return emptyReaderImageResponse(404)
        }
        val pageIndex = segments.getOrNull(2)?.toIntOrNull()
            ?: segments.getOrNull(1)?.toIntOrNull()
            ?: return emptyReaderImageResponse(400)

        val page = currentChapter
            ?.pages
            ?.firstOrNull { page -> page.index == pageIndex }
            ?: return emptyReaderImageResponse(404)
        val streamProvider = page.stream ?: page.awaitStreamForWebView()
            ?: return emptyReaderImageResponse(404)

        return runCatching {
            streamProvider.toWebResourceResponse()
        }.getOrElse { error ->
            logcat(LogPriority.ERROR, error) { "Unable to serve local reader image: $uri" }
            emptyReaderImageResponse(500)
        }
    }

    private fun ReaderPage.awaitStreamForWebView(): (() -> InputStream)? {
        if (status is Page.State.Error) {
            chapter.pageLoader?.retryPage(this)
        }
        (chapter.pageLoader as? HttpPageLoader)?.boostPage(this)

        repeat(IMAGE_STREAM_WAIT_ATTEMPTS) {
            stream?.let { return it }
            try {
                Thread.sleep(IMAGE_STREAM_WAIT_STEP_MS)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                return null
            }
        }
        return stream
    }

    private fun (() -> InputStream).toWebResourceResponse(): WebResourceResponse {
        val (mimeType, stream) = invoke().withDetectedImageMime()
        if (!mimeType.needsWebViewTranscode()) {
            return webResourceResponse(mimeType, stream)
        }

        stream.close()
        return transcodeToJpegResponse(this) ?: run {
            val (fallbackMimeType, fallbackStream) = invoke().withDetectedImageMime()
            webResourceResponse(fallbackMimeType, fallbackStream)
        }
    }

    private fun InputStream.withDetectedImageMime(): Pair<String, InputStream> {
        val buffered = if (markSupported()) this else BufferedInputStream(this)
        buffered.mark(IMAGE_HEADER_READ_LIMIT)
        val header = ByteArray(IMAGE_HEADER_READ_LIMIT)
        val read = buffered.read(header)
        buffered.reset()
        return header.copyOf(read.coerceAtLeast(0)).detectedImageMimeType() to buffered
    }

    private fun transcodeToJpegResponse(streamProvider: () -> InputStream): WebResourceResponse? {
        val bitmap = runCatching {
            streamProvider().use { ImageDecoder.newInstance(it)?.decode() }
        }.getOrNull() ?: return null

        val output = ByteArrayOutputStream()
        val hasAlpha = bitmap.hasAlpha()
        val format = if (hasAlpha) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
        val mimeType = if (hasAlpha) "image/png" else "image/jpeg"
        return try {
            if (!bitmap.compress(format, IMAGE_TRANSCODE_QUALITY, output)) return null
            webResourceResponse(mimeType, ByteArrayInputStream(output.toByteArray()))
        } finally {
            bitmap.recycle()
        }
    }

    private fun webResourceResponse(mimeType: String, stream: InputStream): WebResourceResponse {
        return WebResourceResponse(mimeType, null, stream).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                responseHeaders = mapOf("Cache-Control" to "private, max-age=$READER_IMAGE_CACHE_SECONDS")
            }
        }
    }

    private fun emptyReaderImageResponse(statusCode: Int): WebResourceResponse {
        val stream = ByteArrayInputStream(ByteArray(0))
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            WebResourceResponse(
                "text/plain",
                "UTF-8",
                statusCode,
                "Reader image unavailable",
                mapOf("Cache-Control" to "no-store"),
                stream,
            )
        } else {
            WebResourceResponse("text/plain", "UTF-8", stream)
        }
    }

    override fun getView(): View = webView

    override fun destroy() {
        tts.stop()
        tts.shutdown()
        ttsMediaController.dismiss()
        scope.cancel()
        webView.destroy()
    }

    override fun setChapters(chapters: ViewerChapters) {
        viewerChapters = chapters
        preloadAdjacentChapters(chapters)
        loadChapter(chapters.currChapter)
    }

    fun onAdjacentChaptersPreloaded(chapters: ViewerChapters) {
        viewerChapters = chapters
        preloadAdjacentChapterContent(chapters.prevChapter)
        preloadAdjacentChapterContent(chapters.nextChapter)
    }

    fun beginChapterNavigation(): Boolean {
        if (chapterNavigationInFlight) return false
        chapterNavigationInFlight = true
        resetProgressOnNextChapterLoad = true
        return true
    }

    fun cancelChapterNavigation() {
        chapterNavigationInFlight = false
        resetProgressOnNextChapterLoad = false
    }

    override fun moveToPage(page: ReaderPage) {
        webView.evaluateJavascript(
            "window.scrollTo({ top: 0, behavior: 'auto' }); if (window.pageReader) { pageReader.movePage(0); }",
            null,
        )
    }

    fun seekToProgress(progressPercent: Int) {
        val progress = progressPercent.coerceIn(0, 100)
        webView.evaluateJavascript(
            """
            (function() {
              var progress = $progress / 100;
              if (window.pageReader && pageReader.totalPages && pageReader.totalPages.val > 0) {
                pageReader.movePage(Math.max(0, Math.min(pageReader.totalPages.val - 1,
                  Math.round(progress * (pageReader.totalPages.val - 1)))));
                return;
              }
              var maxScroll = Math.max(0, document.documentElement.scrollHeight - window.innerHeight);
              window.scrollTo({ top: Math.round(maxScroll * progress), behavior: 'auto' });
            })();
            """.trimIndent(),
            null,
        )
    }

    fun setChromeVisible(visible: Boolean) {
        webView.evaluateJavascript(
            "if (window.reader) { reader.hidden.val = ${(!visible).toString()}; }",
            null,
        )
    }

    override fun handleKeyEvent(event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_UP) return false
        return when (event.keyCode) {
            KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_VOLUME_UP,
            -> {
                if (!readerPreferences.readWithVolumeKeys().get()) {
                    false
                } else {
                    val inverted = readerPreferences.readWithVolumeKeysInverted().get()
                    val pageDown = (event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) xor inverted
                    if (pageDown) webView.pageDown(false) else webView.pageUp(false)
                    true
                }
            }
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_PAGE_DOWN,
            -> {
                webView.pageDown(false)
                true
            }
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_PAGE_UP,
            -> {
                webView.pageUp(false)
                true
            }
            KeyEvent.KEYCODE_MENU -> {
                activity.toggleMenu()
                true
            }
            else -> false
        }
    }

    override fun handleGenericMotionEvent(event: MotionEvent): Boolean = false

    private fun loadChapter(chapter: ReaderChapter) {
        val previousChapterId = currentChapter?.chapter?.id
        val chapterId = chapter.chapter.id
        val resetInitialProgress = resetProgressOnNextChapterLoad &&
            previousChapterId != null &&
            previousChapterId != chapterId
        if (resetInitialProgress) {
            resetProgressOnNextChapterLoad = false
        }

        currentChapter = chapter
        lastSettingsPayload = null
        resetTtsQueue()
        if (!autoStartTtsOnNextLoad) {
            ttsMediaController.dismiss()
        }

        chapterLoadJob?.cancel()
        val generation = ++chapterLoadGeneration
        chapterLoadJob = scope.launch {
            val chapterKey = chapter.contentCacheKey()
            val html = preloadedChapterHtml[chapterKey] ?: try {
                activity.viewModel.getLightNovelChapterHtml(chapter)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                logcat(LogPriority.ERROR, error) {
                    "Unable to prepare LNReader content for ${chapter.chapter.name}"
                }
                null
            }
            if (generation != chapterLoadGeneration || currentChapter !== chapter) return@launch

            currentChapterUsesImages = html?.contains("data-nova-reader-type=\"image\"") == true
            applyImageZoomPreference()
            if (currentChapterUsesImages && !hasShownImageReaderChrome) {
                hasShownImageReaderChrome = true
                activity.showMenu()
            }
            val initialProgress = if (resetInitialProgress) {
                0
            } else {
                chapter.chapter.last_page_read.coerceIn(0, 100)
            }
            val document = if (html.isNullOrBlank()) {
                errorHtml(chapter, initialProgress)
            } else {
                readerHtml(
                    chapterHtml = html,
                    chapter = chapter,
                    chapters = viewerChapters,
                    imageReader = currentChapterUsesImages,
                    initialProgress = initialProgress,
                )
            }
            webView.stopLoading()
            if (resetInitialProgress) {
                webView.scrollTo(0, 0)
                resetScrollAfterPageLoad = true
            }
            webView.loadDataWithBaseURL(ASSET_BASE_URL, document, "text/html", "UTF-8", null)
            activity.viewModel.onTextReaderProgress(chapter, chapter.chapter.last_page_read.coerceIn(0, 100))
        }
    }

    private fun preloadAdjacentChapters(chapters: ViewerChapters) {
        listOfNotNull(chapters.prevChapter, chapters.nextChapter).forEach { chapter ->
            preloadAdjacentChapterContent(chapter)
            activity.requestPreloadChapter(chapter)
        }
    }

    private fun preloadAdjacentChapterContent(chapter: ReaderChapter?) {
        chapter ?: return
        if (chapter.state !is ReaderChapter.State.Loaded) return

        val chapterKey = chapter.contentCacheKey()
        if (chapterKey in preloadedChapterHtml || !preloadingChapterKeys.add(chapterKey)) return

        scope.launch {
            try {
                val html = activity.viewModel.getLightNovelChapterHtml(chapter)
                if (!html.isNullOrBlank()) {
                    preloadedChapterHtml[chapterKey] = html
                    while (preloadedChapterHtml.size > MAX_PRELOADED_CHAPTERS) {
                        preloadedChapterHtml.remove(preloadedChapterHtml.keys.first())
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                logcat(LogPriority.WARN, error) {
                    "Unable to preload LNReader content for ${chapter.chapter.name}"
                }
            } finally {
                preloadingChapterKeys.remove(chapterKey)
            }
        }
    }

    private fun ReaderChapter.contentCacheKey(): String {
        return chapter.id?.let { "id:$it" } ?: "url:${chapter.url}"
    }

    private fun ReaderChapter.imageUrlChapterKey(): String {
        return chapter.id?.toString() ?: chapter.url.hashCode().toString()
    }

    private fun handleMessage(message: String) {
        val event = runCatching { JSONObject(message) }
            .onFailure { logcat(LogPriority.ERROR, it) { "Invalid LNReader message: $message" } }
            .getOrNull()
            ?: return
        val eventChapterId = event.optLong("novaChapterId", Long.MIN_VALUE)
        val activeChapterId = currentChapter?.chapter?.id ?: 0L
        if (eventChapterId != Long.MIN_VALUE && eventChapterId != activeChapterId) {
            return
        }

        when (event.optString("type")) {
            "hide" -> activity.toggleMenu()
            "position" -> {
                val progress = event.optInt("data", -1)
                if (progress >= 0) {
                    activity.viewModel.onTextReaderPosition(progress)
                }
            }
            "save" -> {
                val progress = event.optInt("data", -1)
                if (progress >= 0) {
                    currentChapter?.let { activity.viewModel.onTextReaderProgress(it, progress) }
                }
            }
            "next" -> {
                if (!beginChapterNavigation()) return
                autoStartTtsOnNextLoad = event.optBoolean("autoStartTTS", false)
                scope.launch {
                    val originChapterId = activeChapterId
                    activity.viewModel.loadNextChapter()
                    if ((activity.viewModel.state.value.currentChapter?.chapter?.id ?: 0L) == originChapterId) {
                        cancelChapterNavigation()
                    }
                }
            }
            "prev" -> {
                if (!beginChapterNavigation()) return
                autoStartTtsOnNextLoad = event.optBoolean("autoStartTTS", false)
                scope.launch {
                    val originChapterId = activeChapterId
                    activity.viewModel.loadPreviousChapter()
                    if ((activity.viewModel.state.value.currentChapter?.chapter?.id ?: 0L) == originChapterId) {
                        cancelChapterNavigation()
                    }
                }
            }
            "retry-image" -> retryImagePage(event.optInt("pageIndex", -1))
            "tts-queue" -> handleTtsQueue(event)
            "tts-state" -> handleTtsState(event)
            "speak" -> {
                ttsQueueIndex = event.optInt("index", ttsQueueIndex).coerceAtLeast(0)
                ttsQueueTotal = event.optInt("total", ttsQueueTotal).coerceAtLeast(0)
                showTtsNotification(isPlaying = true)
                ttsMediaController.updateProgress(ttsQueueIndex, ttsQueueTotal)
                speak(event.optString("data"))
            }
            "pause-speak" -> {
                tts.stop()
                isTtsReading = false
                ttsMediaController.updatePlaybackState(false)
            }
            "stop-speak" -> {
                tts.stop()
                isTtsReading = false
                resetTtsQueue()
                ttsMediaController.dismiss()
            }
        }
    }

    private fun retryImagePage(pageIndex: Int) {
        val page = currentChapter
            ?.pages
            ?.firstOrNull { it.index == pageIndex }
            ?: return
        page.chapter.pageLoader?.retryPage(page)
    }

    private fun injectTtsScript(script: String) {
        activity.runOnUiThread {
            webView.evaluateJavascript(
                """
                    (function() {
                      if (!window.reader || !window.tts) return;
                      $script
                    })();
                """.trimIndent(),
                null,
            )
        }
    }

    private fun handleTtsQueue(event: JSONObject) {
        val data = event.optJSONObject("data")
        ttsQueue = data
            ?.optJSONArray("queue")
            ?.toStringList()
            .orEmpty()
        ttsQueueIndex = data?.optInt("startIndex", 0)?.coerceAtLeast(0) ?: 0
        ttsQueueTotal = ttsQueue.size
        ttsMediaController.updateProgress(ttsQueueIndex, ttsQueueTotal)
    }

    private fun handleTtsState(event: JSONObject) {
        val reading = event.optJSONObject("data")?.optBoolean("isReading", false) == true
        isTtsReading = reading
        if (reading) {
            showTtsNotification(isPlaying = true)
        }
        ttsMediaController.updatePlaybackState(reading)
    }

    private fun onTtsUtteranceFinished() {
        if (!isAppForeground()) {
            val nextIndex = ttsQueueIndex + 1
            if (nextIndex < ttsQueue.size) {
                ttsQueueIndex = nextIndex
                ttsMediaController.updateProgress(ttsQueueIndex, ttsQueueTotal)
                speak(ttsQueue[nextIndex], fromNativeQueue = true)
            } else {
                isTtsReading = false
                ttsMediaController.dismiss()
                injectTtsScript("tts.stop();")
            }
            return
        }

        activity.runOnUiThread {
            webView.evaluateJavascript("if (window.tts) { tts.next?.(); }", null)
        }
    }

    private fun showTtsNotification(isPlaying: Boolean) {
        val manga = activity.viewModel.manga
        ttsMediaController.show(
            title = manga?.title.orEmpty().ifBlank { "Nova" },
            subtitle = currentChapter?.chapter?.name.orEmpty().ifBlank { "Lecture" },
            coverUri = manga?.thumbnailUrl,
            isPlaying = isPlaying,
        )
    }

    private fun resetTtsQueue() {
        ttsQueue = emptyList()
        ttsQueueIndex = 0
        ttsQueueTotal = 0
        isTtsReading = false
    }

    private fun isAppForeground(): Boolean {
        return ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
    }

    private fun startSettingsSyncLoop() {
        scope.launch {
            while (true) {
                delay(500)
                if (currentChapter == null) continue

                syncReaderSettingsIfChanged()
            }
        }
    }

    private fun syncReaderSettingsIfChanged() {
        val readerSettings = readerSettingsJson()
        val generalSettings = chapterGeneralSettingsJson(currentChapterUsesImages)
        val payload = settingsPayloadString(readerSettings, generalSettings)
        if (payload == lastSettingsPayload) return

        lastSettingsPayload = payload
        applyImageZoomPreference()
        webView.evaluateJavascript(
            """
                (function() {
                  if (!window.reader) return;
                  var previousPageReader = reader.generalSettings.val.pageReader;
                  var progress = previousPageReader && window.pageReader && pageReader.totalPages.val
                    ? ((pageReader.page.val + 1) / pageReader.totalPages.val) * 100
                    : ((window.scrollY + reader.layoutHeight) / Math.max(1, reader.chapterHeight)) * 100;
                  reader.chapter.progress = Math.max(0, Math.min(100, parseInt(progress, 10) || reader.chapter.progress || 0));
                  reader.readerSettings.val = $readerSettings;
                  reader.generalSettings.val = $generalSettings;
                  var customCss = document.getElementById('lnreader-custom-css');
                  if (customCss) customCss.textContent = reader.readerSettings.val.customCSS || '';
                   if (reader.readerSettings.val.customJS && window.__novaLastCustomJS !== reader.readerSettings.val.customJS) {
                     window.__novaLastCustomJS = reader.readerSettings.val.customJS;
                     try { new Function(reader.readerSettings.val.customJS)(); } catch (e) {}
                  }
                   document.body.style.backgroundColor = reader.readerSettings.val.theme;
                  document.body.style.color = reader.readerSettings.val.textColor;
                  if (document.body.classList.contains('nova-image-reader')) {
                    document.documentElement.style.setProperty(
                      '--nova-image-side-padding',
                      (reader.generalSettings.val.imageSidePadding || 0) + '%'
                    );
                  }
                  reader.refresh && reader.refresh();
                  setTimeout(function() {
                    if (typeof calculatePages === 'function') calculatePages();
                  }, previousPageReader === reader.generalSettings.val.pageReader ? 120 : 220);
                })();
            """.trimIndent(),
            null,
        )
    }

    private fun applyImageZoomPreference() {
        webView.settings.setSupportZoom(
            currentChapterUsesImages && readerPreferences.webtoonPinchToZoomEnabled().get(),
        )
    }

    private fun speak(text: String, fromNativeQueue: Boolean = false) {
        val normalized = text.trim()
        if (normalized.isEmpty() || !ttsReady) {
            if (fromNativeQueue) {
                onTtsUtteranceFinished()
            } else {
                webView.evaluateJavascript("if (window.tts) { tts.next?.(); }", null)
            }
            return
        }

        if (fromNativeQueue) {
            showTtsNotification(isPlaying = true)
        }
        readerPreferences.lnReaderTtsVoiceName().get()
            .takeUnless { it == "system" }
            ?.let { selectedVoice ->
                tts.voices?.firstOrNull { it.name == selectedVoice }?.let(tts::setVoice)
            }
        tts.setSpeechRate(readerPreferences.lnReaderTtsRate().get().coerceIn(10, 500) / 100f)
        tts.setPitch(readerPreferences.lnReaderTtsPitch().get().coerceIn(10, 500) / 100f)
        val utteranceId = "lnreader-${System.nanoTime()}"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tts.speak(normalized, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        } else {
            @Suppress("DEPRECATION")
            tts.speak(normalized, TextToSpeech.QUEUE_FLUSH, hashMapOf(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID to utteranceId))
        }
    }

    private fun startAutoScrollLoop() {
        scope.launch {
            while (true) {
                val interval = readerPreferences.lnReaderAutoScrollInterval().get().coerceIn(1, 30)
                delay(interval * 1000L)
                if (!readerPreferences.lnReaderAutoScroll().get()) continue

                val offset = activity.resources.displayMetrics.heightPixels *
                    readerPreferences.lnReaderAutoScrollOffsetPercent().get().coerceIn(25, 100) / 100
                val behavior = if (readerPreferences.smoothAutoScroll().get()) "smooth" else "auto"
                webView.evaluateJavascript(
                    "window.scrollBy({ top: $offset, behavior: '$behavior' });",
                    null,
                )
            }
        }
    }

    private inner class Bridge {
        @JavascriptInterface
        fun postMessage(message: String) {
            activity.runOnUiThread { handleMessage(message) }
        }
    }

    private fun loadingHtml(): String {
        val theme = readerTheme()
        return """
            <!DOCTYPE html>
            <html>
              <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0">
                <style>
                  body {
                    margin: 0;
                    min-height: 100vh;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    background: ${theme.background};
                    color: ${theme.text};
                    font-family: system-ui, sans-serif;
                    font-size: 15px;
                  }
                </style>
              </head>
              <body>Chargement...</body>
            </html>
        """.trimIndent()
    }

    private fun errorHtml(chapter: ReaderChapter, initialProgress: Int): String {
        return readerHtml(
            """
                <h2>${chapter.chapter.name.escapeHtml()}</h2>
                <p>Ce chapitre ne contient pas encore de texte HTML lisible par le lecteur LNReader.</p>
                <p>Mets a jour l'extension light novel ou telecharge/restaure le chapitre avec un fichier index.html.</p>
            """.trimIndent(),
            chapter,
            viewerChapters,
            initialProgress = initialProgress,
        )
    }

    private fun readerHtml(
        chapterHtml: String,
        chapter: ReaderChapter,
        chapters: ViewerChapters?,
        imageReader: Boolean = false,
        initialProgress: Int = chapter.chapter.last_page_read.coerceIn(0, 100),
    ): String {
        val theme = readerTheme()
        val readerSettings = readerSettingsJson(theme)
        val generalSettings = chapterGeneralSettingsJson(imageReader)
        val fontFamily = readerSettings.optString("fontFamily")
        val viewport = if (imageReader) {
            "width=device-width, initial-scale=1.0, minimum-scale=1.0, maximum-scale=5.0, user-scalable=yes"
        } else {
            "width=device-width, initial-scale=1.0, maximum-scale=1.0"
        }
        lastSettingsPayload = settingsPayloadString(readerSettings, generalSettings)
        val chapterJson = JSONObject()
            .put("id", chapter.chapter.id ?: 0L)
            .put("name", chapter.chapter.name)
            .put("progress", initialProgress.coerceIn(0, 100))
        val initialReaderConfig = JSONObject()
            .put("readerSettings", readerSettings)
            .put("chapterGeneralSettings", generalSettings)
            .put("novel", JSONObject().put("name", activity.viewModel.manga?.title.orEmpty()))
            .put("chapter", chapterJson)
            .put("nextChapter", chapters?.nextChapter?.chapter?.name?.let { JSONObject().put("name", it) } ?: JSONObject.NULL)
            .put("prevChapter", chapters?.prevChapter?.chapter?.name?.let { JSONObject().put("name", it) } ?: JSONObject.NULL)
            .put("batteryLevel", 1)
            .put("autoSaveInterval", 2222)
            .put("DEBUG", false)
            .put(
                "strings",
                JSONObject()
                    .put("finished", "Termine : ${chapter.chapter.name}")
                    .put("nextChapter", chapters?.nextChapter?.chapter?.name?.let { "Chapitre suivant : $it" } ?: "")
                    .put("noNextChapter", "Aucun chapitre suivant"),
            )

        return """
            <!DOCTYPE html>
            <html dir="ltr">
              <head>
                <meta name="viewport" content="$viewport">
                <link rel="stylesheet" href="file:///android_asset/css/index.css">
                <link rel="stylesheet" href="file:///android_asset/css/pageReader.css">
                <link rel="stylesheet" href="file:///android_asset/css/toolWrapper.css">
                <link rel="stylesheet" href="file:///android_asset/css/tts.css">
                <style>
                  :root {
                    --StatusBar-currentHeight: 0px;
                    --readerSettings-theme: ${theme.background};
                    --readerSettings-padding: ${readerPreferences.lnReaderTextPadding().get().coerceIn(0, 64)}px;
                    --readerSettings-textSize: ${readerPreferences.lnReaderTextSize().get().coerceIn(12, 32)}px;
                    --readerSettings-textColor: ${theme.text};
                    --readerSettings-textAlign: ${readerPreferences.lnReaderTextAlign().get()};
                    --readerSettings-lineHeight: ${readerPreferences.lnReaderLineHeight().get().coerceIn(100, 240) / 100f};
                    --readerSettings-fontFamily: $fontFamily;
                    --theme-primary: #39b8ff;
                    --theme-onPrimary: #ffffff;
                    --theme-secondary: #ff7085;
                    --theme-tertiary: #71d6a0;
                    --theme-onTertiary: #08110d;
                    --theme-onSecondary: #ffffff;
                    --theme-surface: ${theme.background};
                    --theme-surface-0-9: ${theme.background};
                    --theme-onSurface: ${theme.text};
                    --theme-surfaceVariant: ${theme.surfaceVariant};
                    --theme-onSurfaceVariant: ${theme.text};
                    --theme-outline: ${theme.outline};
                    --theme-rippleColor: rgba(57, 184, 255, 0.18);
                    --nova-image-side-padding: ${readerPreferences.webtoonSidePadding().get().coerceIn(0, 25)}%;
                  }
                  html {
                    scroll-behavior: auto !important;
                    overscroll-behavior-y: contain;
                  }
                  body {
                    -webkit-overflow-scrolling: touch;
                  }
                  .transition-chapter {
                    display: none !important;
                    transition: none !important;
                    transform: none !important;
                  }
                  @font-face {
                    font-family: arbutus-slab;
                    src: url("file:///android_asset/fonts/arbutus-slab.ttf");
                  }
                  @font-face { font-family: domine; src: url("file:///android_asset/fonts/domine.ttf"); }
                  @font-face { font-family: lato; src: url("file:///android_asset/fonts/lato.ttf"); }
                  @font-face { font-family: lora; src: url("file:///android_asset/fonts/lora.ttf"); }
                  @font-face { font-family: noto-sans; src: url("file:///android_asset/fonts/noto-sans.ttf"); }
                  @font-face { font-family: nunito; src: url("file:///android_asset/fonts/nunito.ttf"); }
                  @font-face { font-family: open-sans; src: url("file:///android_asset/fonts/open-sans.ttf"); }
                  @font-face { font-family: OpenDyslexic3-Regular; src: url("file:///android_asset/fonts/OpenDyslexic3-Regular.ttf"); }
                  @font-face { font-family: pt-sans-bold; src: url("file:///android_asset/fonts/pt-sans-bold.ttf"); }
                  @font-face { font-family: pt-serif; src: url("file:///android_asset/fonts/pt-serif.ttf"); }
                  #reader-footer-wrapper,
                  #reader-footer {
                    display: none !important;
                  }
                  body.nova-image-reader {
                    overflow-x: hidden;
                    background: ${theme.background};
                  }
                  body.nova-image-reader #LNReader-chapter {
                    max-width: 100%;
                    padding: 0 !important;
                  }
                  body.nova-image-reader #Image-Modal {
                    display: none !important;
                    pointer-events: none !important;
                  }
                  body.nova-image-reader #TTS-Controller,
                  body.nova-image-reader .TTS-Controller,
                  body.nova-image-reader [id*="TTS"],
                  body.nova-image-reader [class*="tts"],
                  body.nova-image-reader [class*="TTS"] {
                    display: none !important;
                  }
                  .nova-image-chapter {
                    width: 100%;
                    box-sizing: border-box;
                    padding-inline: var(--nova-image-side-padding);
                    margin: 0;
                    touch-action: pan-y pinch-zoom;
                  }
                  .nova-image-chapter .nova-reader-page {
                    margin: 0;
                    padding: 0;
                    width: 100%;
                    background: ${theme.background};
                    touch-action: pan-y pinch-zoom;
                  }
                  .nova-image-chapter img {
                    display: block;
                    width: 100%;
                    max-width: 100%;
                    height: auto;
                    margin: 0 auto;
                    object-fit: contain;
                    touch-action: pan-y pinch-zoom;
                    user-select: none;
                    -webkit-user-drag: none;
                  }
                  .nova-reader-page {
                    position: relative;
                    content-visibility: auto;
                    contain-intrinsic-block-size: auto 180vh;
                  }
                  .nova-image-error-message {
                    display: none;
                    min-height: 180px;
                    align-items: center;
                    justify-content: center;
                    flex-direction: column;
                    gap: 10px;
                    color: ${theme.text};
                    font: 500 14px/1.4 system-ui, sans-serif;
                  }
                  .nova-reader-page.nova-image-error {
                    min-height: 180px;
                  }
                  .nova-reader-page.nova-image-error img {
                    display: none;
                  }
                  .nova-reader-page.nova-image-error .nova-image-error-message {
                    display: flex;
                  }
                  .nova-image-retry {
                    width: 48px;
                    height: 48px;
                    border: 1px solid ${theme.outline};
                    border-radius: 50%;
                    background: ${theme.surfaceVariant};
                    color: ${theme.text};
                    font: 600 26px/1 system-ui, sans-serif;
                  }
                  .nova-image-retry:active {
                    transform: scale(0.94);
                  }
                </style>
              </head>
              <body class="${bodyClasses(imageReader)}">
                <div class="transition-chapter">${chapter.chapter.name.escapeHtml()}</div>
                <div id="LNReader-chapter">
                  $chapterHtml
                </div>
                <div id="reader-ui"></div>
                <script>
                  window.ReactNativeWebView = {
                    postMessage: function(message) {
                      try {
                        var payload = JSON.parse(message);
                        payload.novaChapterId = initialReaderConfig.chapter.id || 0;
                        message = JSON.stringify(payload);
                      } catch (e) {}
                      AndroidReader.postMessage(message);
                    }
                  };
                  var initialPageReaderConfig = ${JSONObject().put("nextChapterScreenVisible", false)};
                  var initialReaderConfig = $initialReaderConfig;
                </script>
                <script src="file:///android_asset/js/polyfill-onscrollend.js"></script>
                <script src="file:///android_asset/js/icons.js"></script>
                <script src="file:///android_asset/js/van.js"></script>
                <script src="file:///android_asset/js/text-vibe.js"></script>
                <script src="file:///android_asset/js/core.js"></script>
                <script src="file:///android_asset/js/index.js"></script>
                ${if (imageReader) novaImageReaderScript() else ""}
                <script>
                  if (window.reader) { reader.hidden.val = ${(!activity.viewModel.state.value.menuVisible).toString()}; }
                </script>
                <style id="lnreader-custom-css">${readerPreferences.lnReaderCustomCss().get().escapeRawTextTag()}</style>
                <script>
                  window.__novaLastCustomJS = ${JSONObject.quote(readerPreferences.lnReaderCustomJs().get())};
                  ${readerPreferences.lnReaderCustomJs().get().escapeRawTextTag()}
                </script>
              </body>
            </html>
        """.trimIndent()
    }

    private fun readerSettingsJson(theme: ReaderTheme = readerTheme()): JSONObject {
        return JSONObject()
            .put("theme", theme.background)
            .put("textColor", theme.text)
            .put("textAlign", readerPreferences.lnReaderTextAlign().get())
            .put("lineHeight", readerPreferences.lnReaderLineHeight().get().coerceIn(100, 240) / 100f)
            .put("fontFamily", readerPreferences.lnReaderFontFamily().get().toCssFontFamily())
            .put("padding", readerPreferences.lnReaderTextPadding().get().coerceIn(0, 64))
            .put("textSize", readerPreferences.lnReaderTextSize().get().coerceIn(12, 32))
            .put("customCSS", readerPreferences.lnReaderCustomCss().get())
            .put("customJS", readerPreferences.lnReaderCustomJs().get())
            .put(
                "tts",
                JSONObject()
                    .put("autoPageAdvance", readerPreferences.lnReaderTtsAutoNext().get())
                    .put("scrollToTop", readerPreferences.lnReaderTtsScrollToTop().get()),
            )
    }

    private fun chapterGeneralSettingsJson(imageReader: Boolean = false): JSONObject {
        return JSONObject()
            .put("TTSEnable", readerPreferences.lnReaderTtsEnabled().get() && !imageReader)
            .put("showBatteryAndTime", false)
            .put("showScrollPercentage", false)
            .put("swipeGestures", readerPreferences.lnReaderSwipeGestures().get())
            .put("pageReader", readerPreferences.lnReaderPageReader().get() && !imageReader)
            .put("removeExtraParagraphSpacing", readerPreferences.lnReaderRemoveExtraParagraphSpacing().get())
            .put("bionicReading", readerPreferences.lnReaderBionicReading().get())
            .put("tapToScroll", readerPreferences.lnReaderTapToScroll().get() && !imageReader)
            .put("verticalSeekbar", readerPreferences.lnReaderVerticalSeekbar().get())
            .put("imageSidePadding", readerPreferences.webtoonSidePadding().get().coerceIn(0, 25))
            .put("imagePinchZoom", readerPreferences.webtoonPinchToZoomEnabled().get())
    }

    private fun bodyClasses(imageReader: Boolean): String {
        return buildList {
            if (!imageReader && readerPreferences.lnReaderPageReader().get()) add("page-reader")
            if (imageReader) add("nova-image-reader")
        }.joinToString(" ")
    }

    private fun novaImageReaderScript(): String {
        return """
                <script>
                  (function() {
                    if (!document.body.classList.contains('nova-image-reader')) return;
                    var chapter = document.querySelector('.nova-image-chapter');
                    if (!chapter || !window.reader) return;

                    var userScrolled = false;
                    var nextTriggered = false;
                    var bottomTimer = null;
                    var warmupFrame = null;
                    var maxAutomaticRetries = 2;
                    var activeFigureIndex = 0;

                    var stopImageContextMenu = function(event) {
                      if (event.target && event.target.closest && event.target.closest('.nova-image-chapter img')) {
                        event.preventDefault();
                        event.stopImmediatePropagation();
                      }
                    };
                    chapter.addEventListener('contextmenu', stopImageContextMenu, true);

                    var getPageHeight = function() {
                      return Math.max(
                        document.body ? document.body.scrollHeight : 0,
                        document.documentElement ? document.documentElement.scrollHeight : 0,
                        chapter.scrollHeight || 0
                      );
                    };
                    var getProgress = function() {
                      var maxScroll = Math.max(1, getPageHeight() - window.innerHeight);
                      return Math.max(0, Math.min(100, Math.round((window.scrollY / maxScroll) * 100)));
                    };
                    var saveProgress = function(force) {
                      var progress = force ? 100 : getProgress();
                      if (force || progress > (reader.chapter.progress || 0)) {
                        reader.chapter.progress = progress;
                        reader.post({ type: 'save', data: progress });
                      }
                    };
                    var isAtBottom = function() {
                      return window.scrollY + window.innerHeight >= getPageHeight() - 32;
                    };
                    var clearBottomTimer = function() {
                      if (bottomTimer) {
                        clearTimeout(bottomTimer);
                        bottomTimer = null;
                      }
                    };
                    var maybeLoadNextChapter = function() {
                      if (!isAtBottom()) {
                        clearBottomTimer();
                        return;
                      }
                      if (!reader.nextChapter || !userScrolled || nextTriggered || bottomTimer) return;
                      bottomTimer = setTimeout(function() {
                        bottomTimer = null;
                        if (!isAtBottom() || !userScrolled || nextTriggered || !reader.nextChapter) return;
                        nextTriggered = true;
                        saveProgress(true);
                        reader.post({ type: 'next' });
                      }, 350);
                    };
                    var markUserScrolled = function() {
                      userScrolled = true;
                    };
                    var retryImage = function(image, automatic) {
                      var figure = image.closest('.nova-reader-page');
                      if (!figure) return;
                      var attempts = parseInt(figure.dataset.retryAttempts || '0', 10);
                      if (automatic && attempts >= maxAutomaticRetries) {
                        figure.classList.add('nova-image-error');
                        return;
                      }
                      figure.dataset.retryAttempts = String(attempts + 1);
                      figure.classList.remove('nova-image-error');
                      figure.classList.add('nova-image-pending');
                      reader.post({
                        type: 'retry-image',
                        pageIndex: parseInt(figure.dataset.pageIndex || '-1', 10)
                      });
                      var source = image.dataset.originalSrc || image.currentSrc || image.src;
                      image.dataset.originalSrc = source.split('?novaRetry=')[0];
                      setTimeout(function() {
                        image.src = image.dataset.originalSrc + '?novaRetry=' + Date.now();
                      }, automatic ? 500 * (attempts + 1) : 100);
                    };
                    var warmNearbyImages = function() {
                      warmupFrame = null;
                      var figures = Array.prototype.slice.call(chapter.querySelectorAll('.nova-reader-page'));
                      if (!figures.length) return;
                      var threshold = -window.innerHeight * 0.25;
                      while (
                        activeFigureIndex < figures.length - 1 &&
                        figures[activeFigureIndex].getBoundingClientRect().bottom < threshold
                      ) {
                        activeFigureIndex += 1;
                      }
                      while (
                        activeFigureIndex > 0 &&
                        figures[activeFigureIndex].getBoundingClientRect().top > threshold
                      ) {
                        activeFigureIndex -= 1;
                      }
                      var start = Math.max(0, activeFigureIndex - 1);
                      var end = Math.min(figures.length, activeFigureIndex + 4);
                      for (var index = start; index < end; index += 1) {
                        var image = figures[index].querySelector('img');
                        if (!image) continue;
                        image.loading = 'eager';
                        image.fetchPriority = index <= activeFigureIndex + 1 ? 'high' : 'auto';
                      }
                    };
                    var scheduleWarmup = function() {
                      if (warmupFrame) return;
                      warmupFrame = requestAnimationFrame(warmNearbyImages);
                    };

                    window.addEventListener('touchmove', markUserScrolled, { passive: true });
                    window.addEventListener('wheel', markUserScrolled, { passive: true });
                    window.addEventListener('keydown', function(event) {
                      if (['ArrowDown', 'PageDown', 'End', ' '].indexOf(event.key) >= 0) {
                        markUserScrolled();
                      }
                    });
                    window.addEventListener('scroll', function() {
                      saveProgress(false);
                      scheduleWarmup();
                      maybeLoadNextChapter();
                    }, { passive: true });

                    Array.prototype.forEach.call(document.images, function(image) {
                      image.dataset.originalSrc = image.currentSrc || image.src;
                      image.addEventListener('load', function() {
                        var figure = image.closest('.nova-reader-page');
                        if (figure) {
                          figure.classList.remove('nova-image-pending', 'nova-image-error');
                          figure.dataset.retryAttempts = '0';
                        }
                        if (reader.refresh) reader.refresh();
                        scheduleWarmup();
                        maybeLoadNextChapter();
                      }, { passive: true });
                      image.addEventListener('error', function() {
                        var figure = image.closest('.nova-reader-page');
                        var attempts = figure ? parseInt(figure.dataset.retryAttempts || '0', 10) : maxAutomaticRetries;
                        if (figure) figure.classList.add('nova-image-error');
                        if (attempts < maxAutomaticRetries) retryImage(image, true);
                      });
                      var retryButton = image.parentElement && image.parentElement.querySelector('.nova-image-retry');
                      if (retryButton) {
                        retryButton.addEventListener('click', function(event) {
                          event.preventDefault();
                          event.stopPropagation();
                          if (image.parentElement) image.parentElement.dataset.retryAttempts = '0';
                          retryImage(image, false);
                        });
                      }
                      if (image.complete) {
                        setTimeout(function() {
                          image.dispatchEvent(new Event(image.naturalWidth > 0 ? 'load' : 'error'));
                        }, 0);
                      }
                    });
                    window.addEventListener('load', function() {
                      setTimeout(function() {
                        if (reader.refresh) reader.refresh();
                        warmNearbyImages();
                        maybeLoadNextChapter();
                      }, 250);
                    });
                  })();
                </script>
        """.trimIndent()
    }

    private fun settingsPayloadString(readerSettings: JSONObject, generalSettings: JSONObject): String {
        return JSONObject()
            .put("readerSettings", readerSettings)
            .put("chapterGeneralSettings", generalSettings)
            .toString()
    }

    private fun readerTheme(): ReaderTheme {
        return when (readerPreferences.lnReaderTheme().get()) {
            "black" -> ReaderTheme("#000000", "#ffffff", "#111111", "#3f3f46")
            "sepia" -> ReaderTheme("#f4ecd8", "#3b2f20", "#eadfc5", "#b9a98d")
            "light" -> ReaderTheme("#ffffff", "#202124", "#f1f3f4", "#c7c9cc")
            "custom" -> ReaderTheme(
                readerPreferences.lnReaderCustomBackgroundColor().get().asCssColor("#292832"),
                readerPreferences.lnReaderCustomTextColor().get().asCssColor("#CCCCCC"),
                "#1f1f2a",
                "#63606d",
            )
            else -> ReaderTheme("#121212", "#ececec", "#1e1e1e", "#4b5563")
        }
    }

    private fun String.escapeHtml(): String {
        return replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }

    private fun String.escapeRawTextTag(): String {
        return replace("</", "<\\/")
    }

    private fun String.asCssColor(fallback: String): String {
        return runCatching {
            android.graphics.Color.parseColor(this)
            this
        }.getOrDefault(fallback)
    }

    private fun String.toCssFontFamily(): String {
        return when (this) {
            "system" -> "system-ui"
            "monospace" -> "monospace"
            else -> this
        }
    }

    private fun JSONArray.toStringList(): List<String> {
        return buildList {
            for (index in 0 until length()) {
                optString(index)
                    .trim()
                    .takeIf { it.isNotEmpty() }
                    ?.let(::add)
            }
        }
    }

    private fun ByteArray.detectedImageMimeType(): String {
        return when {
            size >= 8 &&
                this[0] == 0x89.toByte() &&
                this[1] == 0x50.toByte() &&
                this[2] == 0x4E.toByte() &&
                this[3] == 0x47.toByte() -> "image/png"
            size >= 3 &&
                this[0] == 0xFF.toByte() &&
                this[1] == 0xD8.toByte() &&
                this[2] == 0xFF.toByte() -> "image/jpeg"
            size >= 12 &&
                matchesAscii(0, "RIFF") &&
                matchesAscii(8, "WEBP") -> "image/webp"
            size >= 6 &&
                this[0] == 0x47.toByte() &&
                this[1] == 0x49.toByte() &&
                this[2] == 0x46.toByte() -> "image/gif"
            size >= 12 &&
                matchesAscii(4, "ftyp") &&
                (matchesAscii(8, "avif") || matchesAscii(8, "avis")) -> "image/avif"
            size >= 12 &&
                matchesAscii(4, "ftyp") &&
                (
                    matchesAscii(8, "heic") ||
                        matchesAscii(8, "heix") ||
                        matchesAscii(8, "hevc") ||
                        matchesAscii(8, "hevx") ||
                        matchesAscii(8, "mif1") ||
                        matchesAscii(8, "msf1")
                    ) -> "image/heif"
            startsWithSvg() -> "image/svg+xml"
            else -> "application/octet-stream"
        }
    }

    private fun ByteArray.matchesAscii(offset: Int, value: String): Boolean {
        if (offset < 0 || size < offset + value.length) return false
        return value.indices.all { index -> this[offset + index] == value[index].code.toByte() }
    }

    private fun ByteArray.startsWithSvg(): Boolean {
        val head = toString(Charsets.UTF_8).trimStart()
        return head.startsWith("<svg", ignoreCase = true) ||
            head.startsWith("<?xml", ignoreCase = true) && head.contains("<svg", ignoreCase = true)
    }

    private fun String.needsWebViewTranscode(): Boolean {
        return this == "image/avif" ||
            this == "image/heif" ||
            this == "application/octet-stream"
    }

    private data class ReaderTheme(
        val background: String,
        val text: String,
        val surfaceVariant: String,
        val outline: String,
    )

    private companion object {
        const val ASSET_BASE_URL = "file:///android_asset/"
        const val NOVA_READER_IMAGE_HOST = "nova.local"
        const val NOVA_READER_IMAGE_PATH = "reader-page"
        const val IMAGE_HEADER_READ_LIMIT = 512
        const val IMAGE_TRANSCODE_QUALITY = 94
        const val IMAGE_STREAM_WAIT_ATTEMPTS = 120
        const val IMAGE_STREAM_WAIT_STEP_MS = 100L
        const val MAX_PRELOADED_CHAPTERS = 4
        const val READER_IMAGE_CACHE_SECONDS = 3600
    }
}
