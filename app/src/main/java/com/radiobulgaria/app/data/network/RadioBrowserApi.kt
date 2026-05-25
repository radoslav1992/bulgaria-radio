package com.radiobulgaria.app.data.network

import com.radiobulgaria.app.data.model.NetworkStation
import retrofit2.http.GET
import retrofit2.http.Query

interface RadioBrowserApi {
    @GET("json/stations/search")
    suspend fun searchStations(
        @Query("name") name: String? = null,
        @Query("countrycode") countrycode: String = "BG",
        @Query("tag") tag: String? = null,
        @Query("limit") limit: Int = 100,
        @Query("order") order: String = "votes",
        @Query("reverse") reverse: Boolean = true,
        @Query("hidebroken") hidebroken: Boolean = true
    ): List<NetworkStation>

    @GET("json/stations/search")
    suspend fun getTopStations(
        @Query("countrycode") countrycode: String = "BG",
        @Query("limit") limit: Int = 50,
        @Query("order") order: String = "clickcount",
        @Query("reverse") reverse: Boolean = true,
        @Query("hidebroken") hidebroken: Boolean = true
    ): List<NetworkStation>
}
