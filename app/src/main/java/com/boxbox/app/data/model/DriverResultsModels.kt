package com.boxbox.app.data.model

/**
 * Models for Jolpica's per-driver results endpoint
 * (f1/{season}/drivers/{driverId}/results.json), used to compute season totals like
 * races entered, podiums, and wins from real per-race data rather than guessing or
 * fabricating numbers the standings endpoint doesn't provide.
 */
data class DriverResultsResponse(
    val MRData: DriverResultsMRData = DriverResultsMRData()
)

data class DriverResultsMRData(
    val RaceTable: DriverResultsRaceTable? = null
)

data class DriverResultsRaceTable(
    val Races: List<RaceResultEntry> = emptyList()
)

data class RaceResultEntry(
    val season: String = "",
    val round: String = "",
    val raceName: String = "",
    val Results: List<DriverRaceResult> = emptyList()
)

data class DriverRaceResult(
    val position: String = "",
    val positionText: String = "",
    val points: String = "",
    val status: String = ""
)

/** Honest, computed-from-real-results season summary for a driver. */
data class DriverSeasonSummary(
    val racesEntered: Int,
    val wins: Int,
    val podiums: Int,
    val totalPoints: Double
)
