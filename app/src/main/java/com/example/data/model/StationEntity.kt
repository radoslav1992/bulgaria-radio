package com.example.data.model

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
