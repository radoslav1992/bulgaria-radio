package com.mindtocode.radiobulgaria.player

import android.app.PendingIntent
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * Foreground media service that owns the ExoPlayer and a MediaSession so that
 * playback continues in the background and surfaces a media notification with
 * lock-screen / headset / Bluetooth / Android Auto controls.
 *
 * Also performs automatic reconnection with exponential backoff when a live
 * stream drops (e.g. on a network handover).
 */
@OptIn(UnstableApi::class)
class RadioPlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private val handler = Handler(Looper.getMainLooper())
    private var retryCount = 0

    private val reconnectListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            // A clean READY state means we're streaming again; reset backoff.
            if (playbackState == Player.STATE_READY) {
                retryCount = 0
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            val player = mediaSession?.player ?: return
            // Only try to recover live playback the user actually wants running.
            if (player.playWhenReady && player.mediaItemCount > 0 && retryCount < MAX_RETRIES) {
                retryCount++
                val delayMs = (BASE_RETRY_DELAY_MS shl (retryCount - 1)).coerceAtMost(MAX_RETRY_DELAY_MS)
                handler.postDelayed({
                    val p = mediaSession?.player ?: return@postDelayed
                    p.prepare()
                    p.play()
                }, delayMs)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, /* handleAudioFocus = */ true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()
            .apply { addListener(reconnectListener) }

        // Tapping the notification re-opens the app.
        val sessionActivityPendingIntent = packageManager
            .getLaunchIntentForPackage(packageName)
            ?.let { intent ->
                PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            }

        mediaSession = MediaSession.Builder(this, player)
            .apply { sessionActivityPendingIntent?.let { setSessionActivity(it) } }
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        mediaSession?.run {
            player.removeListener(reconnectListener)
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }

    companion object {
        private const val MAX_RETRIES = 5
        private const val BASE_RETRY_DELAY_MS = 2_000L
        private const val MAX_RETRY_DELAY_MS = 16_000L
    }
}
