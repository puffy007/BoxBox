package com.boxbox.app.data.api

import com.boxbox.app.data.model.*
import retrofit2.http.GET
import retrofit2.http.Query

// ---- OpenF1 API ----
interface OpenF1Api {

    @GET("sessions")
    suspend fun getSessions(
        @Query("year") year: Int,
        @Query("session_type") sessionType: String? = null
    ): List<Session>

    @GET("sessions")
    suspend fun getLatestSession(
        @Query("session_key") sessionKey: String = "latest"
    ): List<Session>

    @GET("drivers")
    suspend fun getDrivers(
        @Query("session_key") sessionKey: String = "latest"
    ): List<Driver>

    @GET("position")
    suspend fun getPositions(
        @Query("session_key") sessionKey: String = "latest"
    ): List<Position>

    @GET("laps")
    suspend fun getLaps(
        @Query("session_key") sessionKey: String = "latest",
        @Query("driver_number") driverNumber: Int? = null
    ): List<Lap>

    @GET("race_control")
    suspend fun getRaceControl(
        @Query("session_key") sessionKey: String = "latest"
    ): List<RaceControlMessage>

    @GET("stints")
    suspend fun getStints(
        @Query("session_key") sessionKey: String = "latest"
    ): List<Stint>

    @GET("intervals")
    suspend fun getIntervals(
        @Query("session_key") sessionKey: String = "latest"
    ): List<Interval>

    @GET("meetings")
    suspend fun getMeetings(
        @Query("year") year: Int
    ): List<Meeting>
}

// ---- Jolpica (Ergast) API ----
interface JolpicaApi {

    @GET("f1/current/driverStandings.json")
    suspend fun getDriverStandings(): ErgastResponse

    @GET("f1/current/constructorStandings.json")
    suspend fun getConstructorStandings(): ErgastResponse

    @GET("f1/current.json")
    suspend fun getCurrentSchedule(): ErgastResponse

    @GET("f1/current/last/results.json")
    suspend fun getLastRaceResults(): ErgastResponse
}
