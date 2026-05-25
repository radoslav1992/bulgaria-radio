package com.radiobulgaria.app.data.model

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Json

@JsonClass(generateAdapter = true)
data class NetworkCountry(
    @Json(name = "name") val name: String,
    @Json(name = "iso_3166_1") val iso31661: String?,
    @Json(name = "stationcount") val stationcount: Int
)

@JsonClass(generateAdapter = true)
data class NetworkLanguage(
    @Json(name = "name") val name: String,
    @Json(name = "stationcount") val stationcount: Int
)

@JsonClass(generateAdapter = true)
data class NetworkStation(
    @Json(name = "stationuuid") val stationuuid: String,
    @Json(name = "name") val name: String,
    @Json(name = "url") val url: String,
    @Json(name = "url_resolved") val urlResolved: String?,
    @Json(name = "homepage") val homepage: String?,
    @Json(name = "favicon") val favicon: String?,
    @Json(name = "tags") val tags: String?,
    @Json(name = "country") val country: String?,
    @Json(name = "countrycode") val countrycode: String?,
    @Json(name = "language") val language: String?,
    @Json(name = "votes") val votes: Int?,
    @Json(name = "clickcount") val clickcount: Int?
)
