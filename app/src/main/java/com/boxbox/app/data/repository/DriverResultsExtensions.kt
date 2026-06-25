package com.boxbox.app.data.repository

import com.boxbox.app.data.model.DriverResultsResponse
import com.boxbox.app.data.model.DriverSeasonSummary
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Jolpica's per-driver results endpoint, used to compute honest season totals
 * (races entered, wins, podiums) since the standings endpoint only provides
 * position/points/wins and nothing more granular.
 */
interface JolpicaResultsApi {
    @GET("f1/current/drivers/{driverId}/results.json")
    suspend fun getDriverResultsCurrentSeason(@Path("driverId") driverId: String): DriverResultsResponse
}

/**
 * Fetches this driver's results for the current season and computes races entered,
 * wins, and podiums by counting actual finishing positions - no fabricated numbers,
 * everything here is derived directly from Jolpica's per-race result data.
 */
suspend fun BoxBoxRepository.getDriverSeasonSummary(driverId: String): DriverSeasonSummary? {
    return try {
        val api = retrofit2.Retrofit.Builder()
            .baseUrl("https://api.jolpi.ca/ergast/")
            .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
            .build()
            .create(JolpicaResultsApi::class.java)

        val response = api.getDriverResultsCurrentSeason(driverId)
        val races = response.MRData.RaceTable?.Races.orEmpty()

        var wins = 0
        var podiums = 0
        var totalPoints = 0.0

        races.forEach { race ->
            val result = race.Results.firstOrNull()
            val pos = result?.position?.toIntOrNull()
            if (pos == 1) wins++
            if (pos != null && pos <= 3) podiums++
            totalPoints += result?.points?.toDoubleOrNull() ?: 0.0
        }

        DriverSeasonSummary(
            racesEntered = races.size,
            wins = wins,
            podiums = podiums,
            totalPoints = totalPoints
        )
    } catch (e: Exception) {
        null
    }
}
