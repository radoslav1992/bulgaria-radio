package com.example.data.database

import androidx.room.*
import com.example.data.model.StationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StationDao {
    @Query("SELECT * FROM stations WHERE isFavorite = 1 ORDER BY name ASC")
    fun getFavorites(): Flow<List<StationEntity>>

    @Query("SELECT * FROM stations WHERE lastPlayedTime IS NOT NULL ORDER BY lastPlayedTime DESC LIMIT 30")
    fun getRecentlyPlayed(): Flow<List<StationEntity>>

    @Query("SELECT * FROM stations WHERE stationuuid = :uuid")
    suspend fun getStationById(uuid: String): StationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStation(station: StationEntity)

    @Query("UPDATE stations SET isFavorite = :isFavorite WHERE stationuuid = :uuid")
    suspend fun updateFavoriteStatus(uuid: String, isFavorite: Boolean)

    @Query("UPDATE stations SET lastPlayedTime = :timestamp WHERE stationuuid = :uuid")
    suspend fun updateLastPlayedTime(uuid: String, timestamp: Long)
}
