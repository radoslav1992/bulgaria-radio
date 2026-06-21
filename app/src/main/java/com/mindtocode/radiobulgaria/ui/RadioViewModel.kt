package com.mindtocode.radiobulgaria.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mindtocode.radiobulgaria.data.database.AppDatabase
import com.mindtocode.radiobulgaria.data.model.StationEntity
import com.mindtocode.radiobulgaria.data.network.RetrofitInstance
import com.mindtocode.radiobulgaria.data.repository.RadioRepository
import com.mindtocode.radiobulgaria.data.repository.VoteResult
import com.mindtocode.radiobulgaria.player.RadioPlaybackState
import com.mindtocode.radiobulgaria.player.RadioPlayerManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class StationsUiState {
    object Idle : StationsUiState()
    object Loading : StationsUiState()
    data class Success(val stations: List<StationEntity>) : StationsUiState()
    data class Error(val message: String) : StationsUiState()
}

class RadioViewModel(
    application: Application,
    private val repository: RadioRepository,
    private val playerManager: RadioPlayerManager
) : AndroidViewModel(application) {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _stationsState = MutableStateFlow<StationsUiState>(StationsUiState.Idle)
    val stationsState = _stationsState.asStateFlow()

    val playbackState: StateFlow<RadioPlaybackState> = playerManager.playbackState
    val currentStation: StateFlow<StationEntity?> = playerManager.currentStation

    val favorites: StateFlow<List<StationEntity>> = repository.favorites
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentlyPlayed: StateFlow<List<StationEntity>> = repository.recentlyPlayed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _voteMessage = MutableStateFlow<String?>(null)
    val voteMessage = _voteMessage.asStateFlow()

    /** Epoch millis at which the sleep timer will pause playback, or null if off. */
    private val _sleepTimerEndTime = MutableStateFlow<Long?>(null)
    val sleepTimerEndTime = _sleepTimerEndTime.asStateFlow()
    private var sleepTimerJob: Job? = null

    init {
        fetchFeatured()
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun fetchFeatured() {
        viewModelScope.launch {
            _stationsState.value = StationsUiState.Loading
            try {
                val stations = repository.getTopStations()
                _stationsState.value = StationsUiState.Success(stations)
            } catch (e: Exception) {
                _stationsState.value = StationsUiState.Error(e.localizedMessage ?: "Грешка при зареждане на станции")
            }
        }
    }

    fun performSearch() {
        viewModelScope.launch {
            _stationsState.value = StationsUiState.Loading
            try {
                val q = _searchQuery.value.trim().ifEmpty { null }
                if (q == null) {
                    val stations = repository.getTopStations()
                    _stationsState.value = StationsUiState.Success(stations)
                } else {
                    val stations = repository.searchStations(name = q)
                    _stationsState.value = StationsUiState.Success(stations)
                }
            } catch (e: Exception) {
                _stationsState.value = StationsUiState.Error(e.localizedMessage ?: "Грешка при търсене")
            }
        }
    }

    fun playStation(station: StationEntity) {
        playerManager.play(station)
        viewModelScope.launch {
            repository.recordPlayback(station)
        }
    }

    fun togglePlayPause() {
        playerManager.togglePlayPause()
    }

    fun setVolume(volume: Float) {
        playerManager.setVolume(volume)
    }

    fun vote(station: StationEntity) {
        viewModelScope.launch {
            when (val result = repository.voteForStation(station)) {
                is VoteResult.Success -> {
                    // Optimistically reflect the new tally in the visible list.
                    val current = _stationsState.value
                    if (current is StationsUiState.Success) {
                        _stationsState.value = StationsUiState.Success(
                            current.stations.map {
                                if (it.stationuuid == station.stationuuid)
                                    it.copy(votes = it.votes + 1)
                                else it
                            }
                        )
                    }
                    _voteMessage.value = "Благодарим за гласа!"
                }
                is VoteResult.Error -> {
                    _voteMessage.value = result.message
                }
            }
        }
    }

    fun clearVoteMessage() {
        _voteMessage.value = null
    }

    /**
     * Starts (or replaces) a sleep timer that pauses playback after [minutes].
     * Passing a non-positive value cancels any active timer.
     */
    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) {
            _sleepTimerEndTime.value = null
            return
        }
        val durationMillis = minutes * 60_000L
        _sleepTimerEndTime.value = System.currentTimeMillis() + durationMillis
        sleepTimerJob = viewModelScope.launch {
            delay(durationMillis)
            playerManager.pause()
            _sleepTimerEndTime.value = null
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        _sleepTimerEndTime.value = null
    }

    fun toggleFavorite(station: StationEntity) {
        viewModelScope.launch {
            val isCurrentlyFav = favorites.value.any { it.stationuuid == station.stationuuid }
            repository.updateFavoriteStatus(station, !isCurrentlyFav)
        }
    }

    override fun onCleared() {
        super.onCleared()
        playerManager.release()
    }
}

class RadioViewModelFactory(
    private val application: Application,
    private val repository: RadioRepository,
    private val playerManager: RadioPlayerManager
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RadioViewModel::class.java)) {
            return RadioViewModel(application, repository, playerManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
