package com.mindtocode.radiobulgaria.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stations")
data class StationEntity(
    @PrimaryKey val stationuuid: String,
    val name: String,
    val urlResolved: String,
    val favicon: String,
    val tags: String,
    val country: String,
    val countrycode: String,
    val language: String,
    val votes: Int = 0,
    val clickcount: Int = 0,
    val isFavorite: Boolean = false,
    val lastPlayedTime: Long? = null
)

/**
 * Records that the current user has voted for a station, so each user is
 * limited to a single vote per station. Persisted independently of whether
 * the station itself is saved (favorited or recently played).
 */
@Entity(tableName = "voted_stations")
data class VotedStationEntity(
    @PrimaryKey val stationuuid: String,
    val votedAt: Long = System.currentTimeMillis()
)
