package com.radiobulgaria.app.data.repository

import com.radiobulgaria.app.data.database.StationDao
import com.radiobulgaria.app.data.model.NetworkStation
import com.radiobulgaria.app.data.model.StationEntity
import com.radiobulgaria.app.data.network.RadioBrowserApi
import kotlinx.coroutines.flow.Flow

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
