package com.mindtocode.radiobulgaria.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.mindtocode.radiobulgaria.data.model.StationEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class RadioPlaybackState {
    object Idle : RadioPlaybackState()
    object Buffering : RadioPlaybackState()
    object Playing : RadioPlaybackState()
    object Paused : RadioPlaybackState()
    data class Error(val message: String) : RadioPlaybackState()
}

/**
 * UI-facing playback controller. It connects to [RadioPlaybackService] through
 * a Media3 [MediaController] so playback survives in the background, while
 * exposing the same StateFlows the rest of the app already relies on.
 */
class RadioPlayerManager(context: Context) {

    private val appContext = context.applicationContext

    private var controller: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null

    /** Station queued before the controller finished connecting. */
    private var pendingStation: StationEntity? = null

    private val _playbackState = MutableStateFlow<RadioPlaybackState>(RadioPlaybackState.Idle)
    val playbackState: StateFlow<RadioPlaybackState> = _playbackState.asStateFlow()

    private val _currentStation = MutableStateFlow<StationEntity?>(null)
    val currentStation: StateFlow<StationEntity?> = _currentStation.asStateFlow()

    /** Current song / show title parsed from the stream's ICY metadata, if any. */
    private val _currentTrackTitle = MutableStateFlow<String?>(null)
    val currentTrackTitle: StateFlow<String?> = _currentTrackTitle.asStateFlow()

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) = updatePlaybackState()
        override fun onPlaybackStateChanged(playbackState: Int) = updatePlaybackState()

        override fun onPlayerError(error: PlaybackException) {
            _playbackState.value = RadioPlaybackState.Error(
                error.localizedMessage ?: "Network playback error"
            )
        }

        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
            val title = mediaMetadata.title?.toString()?.trim()
            val stationName = _currentStation.value?.name
            // ExoPlayer folds ICY stream titles into mediaMetadata.title; only
            // treat it as "now playing" when it differs from the station name.
            _currentTrackTitle.value =
                if (!title.isNullOrBlank() && title != stationName) title else null
        }
    }

    init {
        val sessionToken = SessionToken(
            appContext,
            ComponentName(appContext, RadioPlaybackService::class.java)
        )
        val future = MediaController.Builder(appContext, sessionToken).buildAsync()
        controllerFuture = future
        future.addListener(
            {
                controller = future.get().apply { addListener(playerListener) }
                updatePlaybackState()
                pendingStation?.let { station ->
                    pendingStation = null
                    play(station)
                }
            },
            ContextCompat.getMainExecutor(appContext)
        )
    }

    private fun updatePlaybackState() {
        val c = controller ?: return
        _playbackState.value = when (c.playbackState) {
            Player.STATE_BUFFERING -> RadioPlaybackState.Buffering
            Player.STATE_READY -> if (c.isPlaying) RadioPlaybackState.Playing else RadioPlaybackState.Paused
            else -> RadioPlaybackState.Idle
        }
    }

    fun play(station: StationEntity) {
        _currentStation.value = station
        _currentTrackTitle.value = null

        val c = controller
        if (c == null) {
            pendingStation = station
            _playbackState.value = RadioPlaybackState.Buffering
            return
        }

        val metadataBuilder = MediaMetadata.Builder()
            .setTitle(station.name)
            .setStation(station.name)
            .setArtist(station.language.ifEmpty { "България" })
            .setIsBrowsable(false)
            .setIsPlayable(true)
        if (station.favicon.isNotBlank()) {
            metadataBuilder.setArtworkUri(Uri.parse(station.favicon))
        }

        val mediaItem = MediaItem.Builder()
            .setUri(station.urlResolved)
            .setMediaId(station.stationuuid)
            .setMediaMetadata(metadataBuilder.build())
            .build()

        c.setMediaItem(mediaItem)
        c.prepare()
        c.playWhenReady = true
    }

    fun togglePlayPause() {
        val c = controller ?: return
        if (c.isPlaying) {
            c.pause()
        } else {
            if (c.playbackState == Player.STATE_IDLE && _currentStation.value != null) {
                _currentStation.value?.let { play(it) }
            } else {
                c.play()
            }
        }
    }

    fun pause() {
        controller?.pause()
    }

    fun stop() {
        controller?.stop()
        _playbackState.value = RadioPlaybackState.Idle
    }

    fun setVolume(volume: Float) {
        controller?.volume = volume.coerceIn(0f, 1f)
    }

    fun release() {
        controller?.removeListener(playerListener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        controller = null
    }
}
