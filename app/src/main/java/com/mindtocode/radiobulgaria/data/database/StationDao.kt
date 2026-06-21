package com.mindtocode.radiobulgaria.data.database

import androidx.room.*
import com.mindtocode.radiobulgaria.data.model.StationEntity
import com.mindtocode.radiobulgaria.data.model.VotedStationEntity
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

    @Query("UPDATE stations SET votes = votes + 1 WHERE stationuuid = :uuid")
    suspend fun incrementVotes(uuid: String)

    // --- Voting ---

    @Query("SELECT stationuuid FROM voted_stations")
    fun getVotedStationIds(): Flow<List<String>>

    @Query("SELECT EXISTS(SELECT 1 FROM voted_stations WHERE stationuuid = :uuid)")
    suspend fun hasVoted(uuid: String): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertVote(vote: VotedStationEntity)
}
