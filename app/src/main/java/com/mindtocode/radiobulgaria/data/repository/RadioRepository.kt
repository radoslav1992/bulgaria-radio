package com.mindtocode.radiobulgaria.data.repository

import com.mindtocode.radiobulgaria.data.database.StationDao
import com.mindtocode.radiobulgaria.data.model.NetworkStation
import com.mindtocode.radiobulgaria.data.model.StationEntity
import com.mindtocode.radiobulgaria.data.network.RadioBrowserApi
import kotlinx.coroutines.flow.Flow

sealed class VoteResult {
    object Success : VoteResult()
    data class Error(val message: String) : VoteResult()
}

class RadioRepository(
    private val stationDao: StationDao,
    private val radioBrowserApi: RadioBrowserApi
) {
    val favorites: Flow<List<StationEntity>> = stationDao.getFavorites()
    val recentlyPlayed: Flow<List<StationEntity>> = stationDao.getRecentlyPlayed()

    suspend fun getTopStations(): List<StationEntity> {
        return radioBrowserApi.getTopStations().map { it.toEntity() }
    }

    suspend fun searchStations(name: String? = null): List<StationEntity> {
        return radioBrowserApi.searchStations(name = name).map { it.toEntity() }
    }

    suspend fun getStationById(uuid: String): StationEntity? {
        return stationDao.getStationById(uuid)
    }

    suspend fun saveStationToDb(station: StationEntity) {
        val existing = stationDao.getStationById(station.stationuuid)
        if (existing == null) {
            stationDao.insertStation(station)
        }
    }

    suspend fun updateFavoriteStatus(station: StationEntity, isFavorite: Boolean) {
        val existing = stationDao.getStationById(station.stationuuid)
        if (existing == null) {
            stationDao.insertStation(station.copy(isFavorite = isFavorite))
        } else {
            stationDao.updateFavoriteStatus(station.stationuuid, isFavorite)
        }
    }

    suspend fun recordPlayback(station: StationEntity) {
        val existing = stationDao.getStationById(station.stationuuid)
        val timestamp = System.currentTimeMillis()
        if (existing == null) {
            stationDao.insertStation(station.copy(lastPlayedTime = timestamp))
        } else {
            stationDao.updateLastPlayedTime(station.stationuuid, timestamp)
        }
    }

    /**
     * Casts a vote for the station via the Radio Browser API. There is no
     * local limit on how often a user may vote; the API applies its own
     * server-side throttling per client.
     */
    suspend fun voteForStation(station: StationEntity): VoteResult {
        return try {
            val response = radioBrowserApi.voteForStation(station.stationuuid)
            if (response.ok) {
                stationDao.incrementVotes(station.stationuuid)
                VoteResult.Success
            } else {
                VoteResult.Error(response.message ?: "Гласуването е неуспешно")
            }
        } catch (e: Exception) {
            VoteResult.Error(e.localizedMessage ?: "Грешка при гласуване")
        }
    }

    /**
     * Reports that the user started playing this station so it counts towards
     * the Radio Browser click-count rankings. Best-effort; failures are ignored.
     */
    suspend fun registerStationClick(station: StationEntity) {
        try {
            radioBrowserApi.registerClick(station.stationuuid)
        } catch (_: Exception) {
            // Non-critical telemetry; ignore network/parse errors.
        }
    }

    private fun NetworkStation.toEntity(): StationEntity {
        return StationEntity(
            stationuuid = this.stationuuid,
            name = this.name.trim(),
            urlResolved = this.urlResolved ?: this.url, // fallback to normal url
            favicon = this.favicon ?: "",
            tags = this.tags ?: "",
            country = this.country ?: "",
            countrycode = this.countrycode ?: "",
            language = this.language ?: "",
            votes = this.votes ?: 0,
            clickcount = this.clickcount ?: 0
        )
    }
}
