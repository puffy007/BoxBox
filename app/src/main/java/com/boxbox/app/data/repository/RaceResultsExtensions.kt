package com.boxbox.app.data.repository

import com.boxbox.app.data.model.ErgastDriver
import com.boxbox.app.ui.results.TopThreeEntry
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Jolpica's per-race results endpoint (f1/{season}/{round}/results.json), used to fetch
 * the real finishers for a given race - both the short top-3 podium summary on the
 * Results list, and the fuller results table (Pos./Driver/Time/Pts) on the race detail
 * screen. Genuine per-race data, not derived or estimated.
 */
interface JolpicaRaceResultApi {
    @GET("f1/{season}/{round}/results.json")
    suspend fun getRaceResults(
        @Path("season") season: String,
        @Path("round") round: String
    ): RaceResultsResponse
}

data class RaceResultsResponse(val MRData: RaceResultsMRData = RaceResultsMRData())
data class RaceResultsMRData(val RaceTable: RaceResultsRaceTable? = null)
data class RaceResultsRaceTable(val Races: List<RaceResultsRace> = emptyList())
data class RaceResultsRace(val Results: List<FullRaceResult> = emptyList())
data class FullRaceResult(
    val position: String = "",
    val points: String = "",
    val status: String = "",
    val Driver: ErgastDriver? = null,
    val Time: RaceResultTime? = null
)
data class RaceResultTime(val time: String = "")

private val jolpicaRaceResultApi: JolpicaRaceResultApi by lazy {
    Retrofit.Builder()
        .baseUrl("https://api.jolpi.ca/ergast/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(JolpicaRaceResultApi::class.java)
}

/**
 * Fetches the top-3 finishers for a specific race round, used for the Results list's
 * compact podium chips. Returns an empty list (rather than throwing) if the request
 * fails, so a single missing race doesn't break the whole list.
 */
suspend fun BoxBoxRepository.getRaceTopThree(season: String, round: String): List<TopThreeEntry> {
    return getRaceResultsTable(season, round).take(3)
}

/**
 * Fetches the full results table for a race (full grid, up to 22 finishers), used on the
 * race detail screen. Each entry includes points, unlike the lighter top-3 summary used
 * in the list. The UI shows only the top 5 by default with a "Show all" toggle to reveal
 * the rest, but the data itself covers the whole grid so that toggle has something to show.
 */
suspend fun BoxBoxRepository.getRaceResultsTable(
    season: String,
    round: String,
    limit: Int = 22
): List<TopThreeEntry> {
    return try {
        val response = jolpicaRaceResultApi.getRaceResults(season, round)
        val race = response.MRData.RaceTable?.Races?.firstOrNull()
        val results = race?.Results?.take(limit).orEmpty()

        results.map { r ->
            TopThreeEntry(
                position = r.position,
                driverCode = r.Driver?.code ?: "",
                driverFamilyName = r.Driver?.familyName ?: "",
                gapOrTime = r.Time?.time ?: r.status ?: "",
                points = r.points
            )
        }
    } catch (e: Exception) {
        emptyList()
    }
}
