package com.boxbox.app.data.model

import com.google.firebase.firestore.PropertyName

// --- OpenF1 Models ---

data class Session(
    val session_key: Int = 0,
    val session_name: String = "",
    val session_type: String = "",
    val date_start: String = "",
    val date_end: String = "",
    val location: String = "",
    val country_name: String = "",
    val circuit_short_name: String = "",
    val year: Int = 0
)

data class Driver(
    val driver_number: Int = 0,
    val broadcast_name: String = "",
    val full_name: String = "",
    val name_acronym: String = "",
    val team_name: String = "",
    val team_colour: String = "",
    val country_code: String = "",
    val headshot_url: String = "",
    val session_key: Int = 0
)

data class Position(
    val driver_number: Int = 0,
    val date: String = "",
    val position: Int = 0,
    val session_key: Int = 0
)

data class Lap(
    val driver_number: Int = 0,
    val lap_number: Int = 0,
    val lap_duration: Double? = null,
    val is_pit_out_lap: Boolean = false,
    val duration_sector_1: Double? = null,
    val duration_sector_2: Double? = null,
    val duration_sector_3: Double? = null,
    val session_key: Int = 0
)

data class RaceControlMessage(
    val date: String = "",
    val lap_number: Int? = null,
    val category: String = "",
    val flag: String? = null,
    val message: String = "",
    val session_key: Int = 0
)

data class Stint(
    val driver_number: Int = 0,
    val stint_number: Int = 0,
    val lap_start: Int = 0,
    val lap_end: Int? = null,
    val compound: String = "",
    val tyre_age_at_start: Int = 0,
    val session_key: Int = 0
)

data class Interval(
    val driver_number: Int = 0,
    val date: String = "",
    val gap_to_leader: Double? = null,
    val interval: Double? = null,
    val session_key: Int = 0
)

// Meeting: added circuit_image and country_flag, both real documented fields on
// OpenF1's /meetings endpoint, used by the Results screen for track outline images
// and country flag icons. Everything else unchanged from before.
data class Meeting(
    val meeting_key: Int = 0,
    val meeting_name: String = "",
    val meeting_official_name: String = "",
    val location: String = "",
    val country_name: String = "",
    val country_code: String = "",
    val country_flag: String = "",
    val circuit_short_name: String = "",
    val circuit_image: String = "",
    val date_start: String = "",
    val year: Int = 0
)

// --- Jolpica / Ergast Models ---

data class ErgastResponse(
    val MRData: MRData = MRData()
)

data class MRData(
    val StandingsTable: StandingsTable? = null,
    val RaceTable: RaceTable? = null
)

data class StandingsTable(
    val StandingsLists: List<StandingsList> = emptyList()
)

data class StandingsList(
    val DriverStandings: List<DriverStanding> = emptyList(),
    val ConstructorStandings: List<ConstructorStanding> = emptyList()
)

data class DriverStanding(
    val position: String = "",
    val points: String = "",
    val wins: String = "",
    val Driver: ErgastDriver = ErgastDriver(),
    val Constructors: List<ErgastConstructor> = emptyList()
)

data class ConstructorStanding(
    val position: String = "",
    val points: String = "",
    val wins: String = "",
    val Constructor: ErgastConstructor = ErgastConstructor()
)

data class ErgastDriver(
    val driverId: String = "",
    val permanentNumber: String = "",
    val code: String = "",
    val givenName: String = "",
    val familyName: String = "",
    val nationality: String = ""
)

data class ErgastConstructor(
    val constructorId: String = "",
    val name: String = "",
    val nationality: String = ""
)

data class RaceTable(
    val Races: List<Race> = emptyList()
)

data class Race(
    val season: String = "",
    val round: String = "",
    val raceName: String = "",
    val Circuit: Circuit = Circuit(),
    val date: String = "",
    val time: String = "",
    val FirstPractice: SessionTime? = null,
    val SecondPractice: SessionTime? = null,
    val ThirdPractice: SessionTime? = null,
    val Qualifying: SessionTime? = null,
    val Sprint: SessionTime? = null
)

data class Circuit(
    val circuitId: String = "",
    val circuitName: String = "",
    val Location: Location = Location()
)

data class Location(
    val locality: String = "",
    val country: String = ""
)

data class SessionTime(
    val date: String = "",
    val time: String = ""
)

// --- Firebase User Profile ---

data class UserProfile(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val favouriteDriver: String = "",
    val favouriteTeam: String = "",
    val notificationsEnabled: Boolean = true,
    val notificationIntervalHours: Int = 1,
    val racesWatched: Int = 0,
    @get:PropertyName("isDarkMode")
    @set:PropertyName("isDarkMode")
    var isDarkMode: Boolean = false
)

// --- UI State helpers ---

sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}

// Team colours map
val teamColours = mapOf(
    "Red Bull Racing" to 0xFF3671C6,
    "Ferrari" to 0xFFE8002D,
    "McLaren" to 0xFFFF8000,
    "Mercedes" to 0xFF27F4D2,
    "Aston Martin" to 0xFF229971,
    "Alpine" to 0xFF0093CC,
    "Williams" to 0xFF64C4FF,
    "RB" to 0xFF6692FF,
    "Kick Sauber" to 0xFF52E252,
    "Haas" to 0xFFB6BABD
)
