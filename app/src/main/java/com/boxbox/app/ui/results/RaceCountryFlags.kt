package com.boxbox.app.ui.results

/**
 * Jolpica's Race.Circuit.Location.country field returns the country name as used in race
 * names (e.g. "Australia", "United Kingdom", "USA"), which is different from the
 * demonym format ("Australian", "British") used by driver/constructor nationality
 * elsewhere in the app. This is a separate, dedicated mapping for that.
 *
 * Used as the primary flag source for race cards instead of relying on OpenF1's
 * meeting.country_flag, since that depends on successfully matching a Jolpica race to
 * an OpenF1 meeting by location name - a match that can silently fail for some races
 * (different location naming between the two sources) and leave the flag blank with no
 * visible fallback. flagcdn.com by ISO code is more reliable since it only depends on
 * the country name Jolpica already gives us directly on the race itself.
 */
val raceCountryToIso: Map<String, String> = mapOf(
    "Australia" to "au",
    "China" to "cn",
    "Japan" to "jp",
    "Bahrain" to "bh",
    "Saudi Arabia" to "sa",
    "USA" to "us",
    "United States" to "us",
    "Italy" to "it",
    "Monaco" to "mc",
    "Canada" to "ca",
    "Spain" to "es",
    "Austria" to "at",
    "UK" to "gb",
    "United Kingdom" to "gb",
    "Great Britain" to "gb",
    "Hungary" to "hu",
    "Belgium" to "be",
    "Netherlands" to "nl",
    "Azerbaijan" to "az",
    "Singapore" to "sg",
    "Mexico" to "mx",
    "Brazil" to "br",
    "Qatar" to "qa",
    "UAE" to "ae",
    "Abu Dhabi" to "ae",
    "France" to "fr",
    "Germany" to "de",
    "Portugal" to "pt",
    "Turkey" to "tr",
    "Russia" to "ru",
    "Vietnam" to "vn",
    "South Korea" to "kr",
    "India" to "in",
    "Malaysia" to "my",
    "South Africa" to "za",
    "Argentina" to "ar"
)

fun raceFlagUrlFor(countryName: String): String? {
    val iso = raceCountryToIso[countryName.trim()] ?: return null
    return "https://flagcdn.com/w80/$iso.png"
}
