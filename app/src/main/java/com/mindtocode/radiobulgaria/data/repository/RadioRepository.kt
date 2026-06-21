package com.mindtocode.radiobulgaria.data.repository

import com.mindtocode.radiobulgaria.data.database.StationDao
import com.mindtocode.radiobulgaria.data.model.NetworkStation
import com.mindtocode.radiobulgaria.data.model.StationEntity
import com.mindtocode.radiobulgaria.data.model.VotedStationEntity
import com.mindtocode.radiobulgaria.data.network.RadioBrowserApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

sealed class VoteResult {
    object Success : VoteResult()
    object AlreadyVoted : VoteResult()
    data class Error(val message: String) : VoteResult()
}

class RadioRepository(
    private val stationDao: StationDao,
    private val radioBrowserApi: RadioBrowserApi
) {
    val favorites: Flow<List<StationEntity>> = stationDao.getFavorites()
    val recentlyPlayed: Flow<List<StationEntity>> = stationDao.getRecentlyPlayed()
    val votedStationIds: Flow<Set<String>> =
        stationDao.getVotedStationIds().map { it.toSet() }

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
     * Casts a single vote for the station via the Radio Browser API and
     * records it locally so the user cannot vote for the same station again.
     */
    suspend fun voteForStation(station: StationEntity): VoteResult {
        if (stationDao.hasVoted(station.stationuuid)) {
            return VoteResult.AlreadyVoted
        }
        return try {
            val response = radioBrowserApi.voteForStation(station.stationuuid)
            if (response.ok) {
                stationDao.insertVote(VotedStationEntity(station.stationuuid))
                stationDao.incrementVotes(station.stationuuid)
                VoteResult.Success
            } else {
                VoteResult.Error(response.message ?: "Гласуването е неуспешно")
            }
        } catch (e: Exception) {
            VoteResult.Error(e.localizedMessage ?: "Грешка при гласуване")
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
