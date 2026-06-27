package com.boxbox.app.ui.results

/**
 * Circuit reference facts - circuit length, first Grand Prix year, number of laps, race
 * distance, and lap record - for every circuit on the 2026 calendar.
 *
 * IMPORTANT: none of this data is available through OpenF1 or Jolpica's public APIs.
 * Jolpica's /circuits/ endpoint only returns circuitId, circuitName, and Location
 * (lat/long/locality/country) - no length, lap count, or records. OpenF1 has no circuit
 * metadata endpoint at all beyond a plain outline image. This is a small, explicit,
 * documented static reference (sourced from Sportmonks' circuit guide), the same
 * approach already used for driver photos, team logos, and car images elsewhere in
 * this app - everything else on this screen (race results, points, standings) still
 * comes from the live APIs.
 *
 * Keyed by Jolpica's circuitId so it can be looked up directly from Race.Circuit.circuitId.
 */
data class CircuitFacts(
    val firstGrandPrix: String,
    val circuitLengthKm: Double,
    val numberOfLaps: Int,
    val raceDistanceKm: Double,
    val lapRecordTime: String,
    val lapRecordDriver: String,
    val lapRecordYear: String
)

val circuitFactsById: Map<String, CircuitFacts> = mapOf(
    "albert_park" to CircuitFacts("1996", 5.278, 58, 306.124, "1:20.235", "Sergio Pérez", "2023"),
    "shanghai" to CircuitFacts("2004", 5.451, 56, 305.066, "1:32.238", "Michael Schumacher", "2004"),
    "suzuka" to CircuitFacts("1987", 5.807, 53, 307.471, "1:30.983", "Lewis Hamilton", "2019"),
    "bahrain" to CircuitFacts("2004", 5.412, 57, 308.238, "1:31.447", "Pedro de la Rosa", "2005"),
    "jeddah" to CircuitFacts("2021", 6.174, 50, 308.450, "1:30.734", "Lewis Hamilton", "2021"),
    "miami" to CircuitFacts("2022", 5.412, 57, 308.326, "1:29.708", "Max Verstappen", "2023"),
    "villeneuve" to CircuitFacts("1978", 4.361, 70, 305.270, "1:13.078", "Valtteri Bottas", "2019"),
    "monaco" to CircuitFacts("1950", 3.337, 78, 260.286, "1:12.909", "Lewis Hamilton", "2021"),
    "catalunya" to CircuitFacts("1991", 4.675, 66, 308.424, "1:16.330", "Max Verstappen", "2023"),
    "red_bull_ring" to CircuitFacts("1970", 4.318, 71, 306.452, "1:05.619", "Carlos Sainz", "2020"),
    "silverstone" to CircuitFacts("1950", 5.891, 52, 306.198, "1:27.097", "Max Verstappen", "2020"),
    "spa" to CircuitFacts("1950", 7.004, 44, 308.052, "1:46.286", "Valtteri Bottas", "2018"),
    "hungaroring" to CircuitFacts("1986", 4.381, 70, 306.630, "1:16.627", "Lewis Hamilton", "2020"),
    "zandvoort" to CircuitFacts("1952", 4.259, 72, 306.587, "1:11.097", "Lewis Hamilton", "2021"),
    "monza" to CircuitFacts("1950", 5.793, 53, 306.720, "1:21.046", "Rubens Barrichello", "2004"),
    "madrid" to CircuitFacts("2026 (Inaugural)", 5.470, 57, 311.790, "—", "—", "—"),
    "baku" to CircuitFacts("2016", 6.003, 51, 306.049, "1:43.009", "Charles Leclerc", "2019"),
    "marina_bay" to CircuitFacts("2008", 5.063, 61, 308.706, "1:35.867", "Lewis Hamilton", "2023"),
    "americas" to CircuitFacts("2012", 5.513, 56, 308.405, "1:36.169", "Charles Leclerc", "2019"),
    "rodriguez" to CircuitFacts("1963", 4.304, 71, 305.354, "1:17.774", "Valtteri Bottas", "2021"),
    "interlagos" to CircuitFacts("1973", 4.309, 71, 305.879, "1:10.540", "Valtteri Bottas", "2018"),
    "las_vegas" to CircuitFacts("2023", 6.120, 50, 306.000, "1:35.490", "Oscar Piastri", "2023"),
    "losail" to CircuitFacts("2021", 5.380, 57, 308.238, "1:24.319", "Max Verstappen", "2023"),
    "yas_marina" to CircuitFacts("2009", 5.281, 58, 306.183, "1:26.103", "Max Verstappen", "2021")
)

fun circuitFactsFor(circuitId: String): CircuitFacts? = circuitFactsById[circuitId]
