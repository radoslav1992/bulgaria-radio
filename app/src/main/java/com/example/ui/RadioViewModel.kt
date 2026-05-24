package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.NetworkCountry
import com.example.data.model.NetworkLanguage
import com.example.data.model.StationEntity
import com.example.data.network.RetrofitInstance
import com.example.data.repository.RadioRepository
import com.example.player.RadioPlaybackState
import com.example.player.RadioPlayerManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class CountriesUiState {
    object Loading : CountriesUiState()
    data class Success(val countries: List<NetworkCountry>) : CountriesUiState()
    data class Error(val message: String) : CountriesUiState()
}

sealed class LanguagesUiState {
    object Loading : LanguagesUiState()
    data class Success(val languages: List<NetworkLanguage>) : LanguagesUiState()
    data class Error(val message: String) : LanguagesUiState()
}

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

    // UI state filters
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedCountry = MutableStateFlow<NetworkCountry?>(null)
    val selectedCountry = _selectedCountry.asStateFlow()

    private val _selectedLanguage = MutableStateFlow<NetworkLanguage?>(null)
    val selectedLanguage = _selectedLanguage.asStateFlow()

    // Screen states
    val countriesState: StateFlow<CountriesUiState> = flow {
        emit(CountriesUiState.Loading)
        try {
            val list = repository.getCountries()
                .filter { it.name.isNotBlank() && it.stationcount > 0 }
                .sortedByDescending { it.stationcount }
            emit(CountriesUiState.Success(list))
        } catch (e: Exception) {
            emit(CountriesUiState.Error(e.localizedMessage ?: "Failed to fetch locations"))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CountriesUiState.Loading)

    val languagesState: StateFlow<LanguagesUiState> = flow {
        emit(LanguagesUiState.Loading)
        try {
            val list = repository.getLanguages()
                .filter { it.name.isNotBlank() && it.stationcount > 0 }
                .sortedByDescending { it.stationcount }
            emit(LanguagesUiState.Success(list))
        } catch (e: Exception) {
            emit(LanguagesUiState.Error(e.localizedMessage ?: "Failed to fetch languages"))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LanguagesUiState.Loading)

    private val _stationsState = MutableStateFlow<StationsUiState>(StationsUiState.Idle)
    val stationsState = _stationsState.asStateFlow()

    // Active Player status
    val playbackState: StateFlow<RadioPlaybackState> = playerManager.playbackState
    val currentStation: StateFlow<StationEntity?> = playerManager.currentStation

    // Room Persistent lists
    val favorites: StateFlow<List<StationEntity>> = repository.favorites
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentlyPlayed: StateFlow<List<StationEntity>> = repository.recentlyPlayed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Fetch recommendations on launch
        fetchFeatured()
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCountry(country: NetworkCountry?) {
        _selectedCountry.value = country
        performSearch()
    }

    fun selectLanguage(language: NetworkLanguage?) {
        _selectedLanguage.value = language
        performSearch()
    }

    fun fetchFeatured() {
        viewModelScope.launch {
            _stationsState.value = StationsUiState.Loading
            try {
                val stations = repository.getTopStations()
                _stationsState.value = StationsUiState.Success(stations)
            } catch (e: Exception) {
                _stationsState.value = StationsUiState.Error(e.localizedMessage ?: "Failed to load active stations")
            }
        }
    }

    fun performSearch() {
        viewModelScope.launch {
            _stationsState.value = StationsUiState.Loading
            try {
                val q = _searchQuery.value.trim().ifEmpty { null }
                val c = _selectedCountry.value?.name
                val l = _selectedLanguage.value?.name

                if (q == null && c == null && l == null) {
                    val stations = repository.getTopStations()
                    _stationsState.value = StationsUiState.Success(stations)
                } else {
                    val stations = repository.searchStations(name = q, country = c, language = l)
                    _stationsState.value = StationsUiState.Success(stations)
                }
            } catch (e: Exception) {
                _stationsState.value = StationsUiState.Error(e.localizedMessage ?: "Failed search response")
            }
        }
    }

    fun playStation(station: StationEntity) {
        playerManager.play(station)
        viewModelScope.launch {
            // Record last played station in DB
            repository.recordPlayback(station)
        }
    }

    fun togglePlayPause() {
        playerManager.togglePlayPause()
    }

    fun setVolume(volume: Float) {
        playerManager.setVolume(volume)
    }

    fun toggleFavorite(station: StationEntity) {
        viewModelScope.launch {
            val isCurrentlyFav = favorites.value.any { it.stationuuid == station.stationuuid }
            repository.updateFavoriteStatus(station, !isCurrentlyFav)
        }
    }

    override fun onCleared() {
        super.onCleared()
        // We can release resources of the player when VM is cleared
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
