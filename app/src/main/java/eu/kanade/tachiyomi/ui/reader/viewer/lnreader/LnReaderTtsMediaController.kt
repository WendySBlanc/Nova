package eu.kanade.tachiyomi.ui.reader.viewer.lnreader

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaStyleNotificationHelper
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.notification.Notifications
import java.io.File
import java.net.URL
import kotlin.math.max

@androidx.annotation.OptIn(UnstableApi::class)
internal class LnReaderTtsMediaController(
    private val context: Context,
    private val callbacks: Callbacks,
) {

    private val appContext = context.applicationContext
    private val actionPrefix = "${appContext.packageName}.lnreader.tts"
    private val actionPlay = "$actionPrefix.PLAY"
    private val actionPause = "$actionPrefix.PAUSE"
    private val actionStop = "$actionPrefix.STOP"
    private val actionPrev = "$actionPrefix.PREV"
    private val actionNext = "$actionPrefix.NEXT"
    private val actionRewind = "$actionPrefix.REWIND"

    private var player: TtsSessionPlayer? = null
    private var mediaSession: MediaSession? = null
    private var receiverRegistered = false
    private var coverBitmap: Bitmap? = null
    private var currentCoverUri: String? = null
    private var title: String = ""
    private var subtitle: String = ""
    private var isPlaying = false
    private var currentPosition = 0L
    private var totalDuration = 1L

    private val mediaReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                actionPlay -> {
                    isPlaying = true
                    callbacks.onPlay()
                    updateSessionState()
                    updateNotification()
                }
                actionPause -> {
                    isPlaying = false
                    callbacks.onPause()
                    updateSessionState()
                    updateNotification()
                }
                actionStop -> callbacks.onStop()
                actionPrev -> callbacks.onPrevious()
                actionNext -> callbacks.onNext()
                actionRewind -> callbacks.onRewind()
            }
        }
    }

    fun show(
        title: String,
        subtitle: String,
        coverUri: String?,
        isPlaying: Boolean,
    ) {
        this.title = title
        this.subtitle = subtitle
        this.isPlaying = isPlaying
        ensureMediaSession()
        registerReceiver()
        loadCoverBitmap(coverUri.orEmpty())
        updateSessionState()
        updateNotification()
    }

    fun updatePlaybackState(isPlaying: Boolean) {
        this.isPlaying = isPlaying
        updateSessionState()
        updateNotification()
    }

    fun updateProgress(current: Int, total: Int) {
        currentPosition = current.coerceAtLeast(0).toLong()
        totalDuration = total.coerceAtLeast(1).toLong()
        updateSessionState()
        updateNotification()
    }

    fun dismiss() {
        mediaSession?.release()
        mediaSession = null
        player?.release()
        player = null

        val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(Notifications.ID_READER_TTS)

        if (receiverRegistered) {
            runCatching { appContext.unregisterReceiver(mediaReceiver) }
            receiverRegistered = false
        }

        coverBitmap = null
        currentCoverUri = null
        currentPosition = 0L
        totalDuration = 1L
        isPlaying = false
    }

    private fun ensureMediaSession() {
        if (mediaSession != null) return

        val sessionPlayer = TtsSessionPlayer(
            looper = Looper.getMainLooper(),
            callbacks = callbacks,
            stateProvider = ::mediaState,
            stateSink = { playing, position ->
                isPlaying = playing
                currentPosition = position
                updateNotification()
            },
        )
        player = sessionPlayer
        mediaSession = MediaSession.Builder(appContext, sessionPlayer)
            .setId("NovaLnReaderTTS")
            .build()
    }

    private fun registerReceiver() {
        if (receiverRegistered) return

        val filter = IntentFilter().apply {
            addAction(actionPlay)
            addAction(actionPause)
            addAction(actionStop)
            addAction(actionPrev)
            addAction(actionNext)
            addAction(actionRewind)
        }
        ContextCompat.registerReceiver(
            appContext,
            mediaReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        receiverRegistered = true
    }

    private fun buildPendingIntent(action: String): PendingIntent {
        val intent = Intent(action).setPackage(appContext.packageName)
        return PendingIntent.getBroadcast(
            appContext,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun contentIntent(): PendingIntent {
        val intent = appContext.packageManager.getLaunchIntentForPackage(appContext.packageName) ?: Intent()
        return PendingIntent.getActivity(
            appContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun updateSessionState() {
        player?.invalidateFromController()
    }

    private fun updateNotification() {
        val session = mediaSession ?: return
        if (!canPostNotifications()) return

        val playPauseAction = if (isPlaying) {
            NotificationCompat.Action.Builder(
                android.R.drawable.ic_media_pause,
                "Pause",
                buildPendingIntent(actionPause),
            ).build()
        } else {
            NotificationCompat.Action.Builder(
                android.R.drawable.ic_media_play,
                "Play",
                buildPendingIntent(actionPlay),
            ).build()
        }

        val notification = NotificationCompat.Builder(appContext, Notifications.CHANNEL_READER_TTS)
            .setContentTitle(subtitle)
            .setContentText(title)
            .setSmallIcon(R.drawable.ic_nova_mark)
            .setLargeIcon(coverBitmap)
            .setContentIntent(contentIntent())
            .setDeleteIntent(buildPendingIntent(actionStop))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(isPlaying)
            .setOnlyAlertOnce(true)
            .addAction(android.R.drawable.ic_media_previous, "Previous", buildPendingIntent(actionPrev))
            .addAction(android.R.drawable.ic_media_rew, "Replay", buildPendingIntent(actionRewind))
            .addAction(playPauseAction)
            .addAction(android.R.drawable.ic_media_next, "Next", buildPendingIntent(actionNext))
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", buildPendingIntent(actionStop))
            .setStyle(
                MediaStyleNotificationHelper.MediaStyle(session)
                    .setShowActionsInCompactView(1, 2, 3),
            )
            .build()

        val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(Notifications.ID_READER_TTS, notification)
    }

    private fun canPostNotifications(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            PermissionChecker.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS) ==
            PermissionChecker.PERMISSION_GRANTED
    }

    private fun loadCoverBitmap(coverUri: String) {
        if (coverUri == currentCoverUri && coverBitmap != null) return
        currentCoverUri = coverUri

        if (coverUri.isBlank()) {
            coverBitmap = null
            return
        }

        if (coverUri.startsWith("file://")) {
            val path = coverUri.removePrefix("file://").substringBefore("?")
            coverBitmap = File(path)
                .takeIf { it.exists() }
                ?.let { BitmapFactory.decodeFile(it.absolutePath) }
        } else if (coverUri.startsWith("http")) {
            coverBitmap = null
            Thread {
                runCatching {
                    URL(coverUri).openStream().use(BitmapFactory::decodeStream)
                }.onSuccess {
                    coverBitmap = it
                    updateNotification()
                }
            }.start()
        }
    }

    private fun mediaState(): TtsMediaState {
        return TtsMediaState(
            title = title.ifBlank { "Nova" },
            subtitle = subtitle.ifBlank { "Lecture" },
            coverUri = currentCoverUri?.takeIf { it.isNotBlank() },
            playing = isPlaying,
            positionMs = currentPosition.coerceAtLeast(0) * 1000L,
            durationMs = max(totalDuration, currentPosition + 1L) * 1000L,
        )
    }

    interface Callbacks {
        fun onPlay()
        fun onPause()
        fun onStop()
        fun onPrevious()
        fun onNext()
        fun onRewind()
        fun onSeekTo(index: Int)
    }

    private data class TtsMediaState(
        val title: String,
        val subtitle: String,
        val coverUri: String?,
        val playing: Boolean,
        val positionMs: Long,
        val durationMs: Long,
    )

    private class TtsSessionPlayer(
        looper: Looper,
        private val callbacks: Callbacks,
        private val stateProvider: () -> TtsMediaState,
        private val stateSink: (playing: Boolean, position: Long) -> Unit,
    ) : SimpleBasePlayer(looper) {

        private val commands = Player.Commands.Builder()
            .add(Player.COMMAND_PLAY_PAUSE)
            .add(Player.COMMAND_STOP)
            .add(Player.COMMAND_SEEK_BACK)
            .add(Player.COMMAND_SEEK_FORWARD)
            .add(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)
            .add(Player.COMMAND_SEEK_TO_PREVIOUS)
            .add(Player.COMMAND_SEEK_TO_NEXT)
            .build()

        override fun getState(): State {
            val mediaState = stateProvider()
            val metadata = MediaMetadata.Builder()
                .setTitle(mediaState.subtitle)
                .setArtist(mediaState.title)
                .setArtworkUri(mediaState.coverUri?.let(Uri::parse))
                .build()
            val mediaItem = MediaItem.Builder()
                .setMediaId("nova-lnreader-tts")
                .setMediaMetadata(metadata)
                .build()
            val mediaItemData = MediaItemData.Builder("nova-lnreader-tts")
                .setMediaItem(mediaItem)
                .setMediaMetadata(metadata)
                .setDurationUs(mediaState.durationMs * 1000L)
                .setIsSeekable(true)
                .build()

            return State.Builder()
                .setAvailableCommands(commands)
                .setPlaylist(listOf(mediaItemData))
                .setCurrentMediaItemIndex(0)
                .setPlaybackState(Player.STATE_READY)
                .setPlayWhenReady(mediaState.playing, Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
                .setContentPositionMs(
                    PositionSupplier.getConstant(mediaState.positionMs.coerceIn(0, mediaState.durationMs)),
                )
                .setContentBufferedPositionMs(PositionSupplier.getConstant(mediaState.durationMs))
                .setSeekBackIncrementMs(1000L)
                .setSeekForwardIncrementMs(1000L)
                .build()
        }

        override fun handleSetPlayWhenReady(playWhenReady: Boolean): ListenableFuture<*> {
            if (playWhenReady) {
                callbacks.onPlay()
            } else {
                callbacks.onPause()
            }
            val position = stateProvider().positionMs / 1000L
            stateSink(playWhenReady, position)
            return Futures.immediateVoidFuture()
        }

        override fun handleStop(): ListenableFuture<*> {
            callbacks.onStop()
            stateSink(false, stateProvider().positionMs / 1000L)
            return Futures.immediateVoidFuture()
        }

        override fun handleSeek(
            mediaItemIndex: Int,
            positionMs: Long,
            seekCommand: Int,
        ): ListenableFuture<*> {
            when (seekCommand) {
                Player.COMMAND_SEEK_TO_PREVIOUS -> callbacks.onPrevious()
                Player.COMMAND_SEEK_TO_NEXT -> callbacks.onNext()
                Player.COMMAND_SEEK_BACK -> callbacks.onRewind()
                else -> {
                    val safePosition = positionMs.coerceAtLeast(0L)
                    callbacks.onSeekTo((safePosition / 1000L).toInt())
                    stateSink(stateProvider().playing, safePosition / 1000L)
                }
            }
            return Futures.immediateVoidFuture()
        }

        fun invalidateFromController() {
            invalidateState()
        }
    }
}
