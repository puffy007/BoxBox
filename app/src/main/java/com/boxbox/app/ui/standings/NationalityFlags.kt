package com.boxbox.app.ui.standings

/**
 * Jolpica's nationality field returns a demonym (e.g. "Italian", "British", "Dutch"),
 * not an ISO country code or full country name. This maps each demonym on the current
 * F1 grid to both:
 *   - an ISO 3166-1 alpha-2 code, used to build a flagcdn.com image URL
 *     (https://flagcdn.com/w40/{code}.png) - a real flag image rather than emoji, which
 *     can render as plain text on some Android fonts/devices
 *   - the full country name, since the reference design shows "United Kingdom" rather
 *     than the demonym "British"
 *
 * Falls back to just showing the demonym text with no flag for anything not covered.
 */
data class CountryInfo(val isoCode: String, val countryName: String)

val nationalityToCountry: Map<String, CountryInfo> = mapOf(
    "British" to CountryInfo("gb", "United Kingdom"),
    "Dutch" to CountryInfo("nl", "Netherlands"),
    "Italian" to CountryInfo("it", "Italy"),
    "Monegasque" to CountryInfo("mc", "Monaco"),
    "German" to CountryInfo("de", "Germany"),
    "Spanish" to CountryInfo("es", "Spain"),
    "French" to CountryInfo("fr", "France"),
    "Australian" to CountryInfo("au", "Australia"),
    "Mexican" to CountryInfo("mx", "Mexico"),
    "Finnish" to CountryInfo("fi", "Finland"),
    "Canadian" to CountryInfo("ca", "Canada"),
    "Thai" to CountryInfo("th", "Thailand"),
    "Japanese" to CountryInfo("jp", "Japan"),
    "New Zealander" to CountryInfo("nz", "New Zealand"),
    "Argentine" to CountryInfo("ar", "Argentina"),
    "Brazilian" to CountryInfo("br", "Brazil"),
    "American" to CountryInfo("us", "United States"),
    "Danish" to CountryInfo("dk", "Denmark"),
    "Belgian" to CountryInfo("be", "Belgium"),
    "Swiss" to CountryInfo("ch", "Switzerland"),
    "Austrian" to CountryInfo("at", "Austria"),
    "Swedish" to CountryInfo("se", "Sweden"),
    "Polish" to CountryInfo("pl", "Poland"),
    "Russian" to CountryInfo("ru", "Russia"),
    "Chinese" to CountryInfo("cn", "China"),
    "Indonesian" to CountryInfo("id", "Indonesia"),
    "Indian" to CountryInfo("in", "India")
)

fun countryInfoFor(nationality: String): CountryInfo? = nationalityToCountry[nationality.trim()]

/** Builds a flagcdn.com URL for the given ISO country code at a small fixed width. */
fun flagUrlFor(isoCode: String): String = "https://flagcdn.com/w40/$isoCode.png"
