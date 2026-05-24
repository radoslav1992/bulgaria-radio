package com.example.data.network

import com.example.data.model.NetworkCountry
import com.example.data.model.NetworkLanguage
import com.example.data.model.NetworkStation
import retrofit2.http.GET
import retrofit2.http.Query

interface RadioBrowserApi {
    @GET("json/countries")
    suspend fun getCountries(
        @Query("order") order: String = "stationcount",
        @Query("reverse") reverse: Boolean = true
    ): List<NetworkCountry>

    @GET("json/languages")
    suspend fun getLanguages(
        @Query("order") order: String = "stationcount",
        @Query("reverse") reverse: Boolean = true
    ): List<NetworkLanguage>

    @GET("json/stations/search")
    suspend fun searchStations(
        @Query("name") name: String? = null,
        @Query("country") country: String? = null,
        @Query("language") language: String? = null,
        @Query("tag") tag: String? = null,
        @Query("limit") limit: Int = 100,
        @Query("order") order: String = "votes",
        @Query("reverse") reverse: Boolean = true,
        @Query("hidebroken") hidebroken: Boolean = true
    ): List<NetworkStation>

    @GET("json/stations")
    suspend fun getTopStations(
        @Query("limit") limit: Int = 50,
        @Query("order") order: String = "clickcount",
        @Query("reverse") reverse: Boolean = true,
        @Query("hidebroken") hidebroken: Boolean = true
    ): List<NetworkStation>
}
