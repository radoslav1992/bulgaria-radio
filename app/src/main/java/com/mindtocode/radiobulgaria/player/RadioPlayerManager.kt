package com.mindtocode.radiobulgaria.player

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
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

@OptIn(UnstableApi::class)
class RadioPlayerManager(private val context: Context) {

    private var exoPlayer: ExoPlayer? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _playbackState = MutableStateFlow<RadioPlaybackState>(RadioPlaybackState.Idle)
    val playbackState: StateFlow<RadioPlaybackState> = _playbackState.asStateFlow()

    private val _currentStation = MutableStateFlow<StationEntity?>(null)
    val currentStation: StateFlow<StationEntity?> = _currentStation.asStateFlow()

    init {
        mainHandler.post {
            initializePlayer()
        }
    }

    private fun initializePlayer() {
        if (exoPlayer != null) return
        
        exoPlayer = ExoPlayer.Builder(context)
            .setAudioAttributes(AudioAttributes.DEFAULT, true)
            .build()
            .apply {
                addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        updatePlaybackState()
                    }

                    override fun onPlaybackStateChanged(state: Int) {
                        updatePlaybackState()
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        _playbackState.value = RadioPlaybackState.Error(
                            error.localizedMessage ?: "Network playback error"
                        )
                    }
                })
            }
    }

    private fun updatePlaybackState() {
        val player = exoPlayer ?: return
        val isPlayerPlaying = player.isPlaying
        val state = player.playbackState

        _playbackState.value = when (state) {
            Player.STATE_BUFFERING -> RadioPlaybackState.Buffering
            Player.STATE_READY -> {
                if (isPlayerPlaying) RadioPlaybackState.Playing else RadioPlaybackState.Paused
            }
            Player.STATE_IDLE -> RadioPlaybackState.Idle
            Player.STATE_ENDED -> RadioPlaybackState.Idle
            else -> RadioPlaybackState.Idle
        }
    }

    fun play(station: StationEntity) {
        _currentStation.value = station
        mainHandler.post {
            val player = exoPlayer ?: return@post
            try {
                _playbackState.value = RadioPlaybackState.Buffering
                val mediaItem = MediaItem.fromUri(Uri.parse(station.urlResolved))
                player.setMediaItem(mediaItem)
                player.prepare()
                player.play()
            } catch (e: Exception) {
                _playbackState.value = RadioPlaybackState.Error(
                    e.localizedMessage ?: "Failed to start streaming"
                )
            }
        }
    }

    fun togglePlayPause() {
        mainHandler.post {
            val player = exoPlayer ?: return@post
            if (player.isPlaying) {
                player.pause()
            } else {
                if (player.playbackState == Player.STATE_IDLE && _currentStation.value != null) {
                    _currentStation.value?.let { play(it) }
                } else {
                    player.play()
                }
            }
        }
    }

    fun pause() {
        mainHandler.post {
            exoPlayer?.pause()
        }
    }

    fun stop() {
        mainHandler.post {
            exoPlayer?.stop()
            _playbackState.value = RadioPlaybackState.Idle
        }
    }

    fun setVolume(volume: Float) {
        mainHandler.post {
            exoPlayer?.volume = volume.coerceIn(0f, 1f)
        }
    }

    fun release() {
        mainHandler.post {
            exoPlayer?.release()
            exoPlayer = null
        }
    }
}
