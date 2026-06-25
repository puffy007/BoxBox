package com.boxbox.app.ui.standings

/**
 * Team car silhouette image URLs, scraped directly from formula1.com (Cloudinary CDN,
 * confirmed real by fetching formula1.com/en/drivers, where these same URLs appear in
 * the team-switcher section of the page - not guessed).
 *
 * OpenF1 has no equivalent - its car_data endpoint is telemetry (speed, throttle, brake,
 * gear) sampled during a session, not an image asset. There's no API field anywhere in
 * OpenF1 or Jolpica that returns a picture of the car, so this is a small, explicit,
 * documented exception: every other piece of data in this screen (standings, points,
 * driver lineup, driver photos) still comes from the live APIs.
 *
 * Keyed by normalized (accent-stripped, lowercase) team name, matching the keys used in
 * Theme.kt's teamAccentColors map.
 */
val teamCarPhotos: Map<String, String> = mapOf(
    "alpine" to "https://media.formula1.com/image/upload/c_lfill,w_512/q_auto/d_common:f1:2026:fallback:car:2026fallbackcarright.webp/v1740000001/common/f1/2026/alpine/2026alpinecarright.webp",
    "aston martin" to "https://media.formula1.com/image/upload/c_lfill,w_512/q_auto/d_common:f1:2026:fallback:car:2026fallbackcarright.webp/v1740000001/common/f1/2026/astonmartin/2026astonmartincarright.webp",
    "audi" to "https://media.formula1.com/image/upload/c_lfill,w_512/q_auto/d_common:f1:2026:fallback:car:2026fallbackcarright.webp/v1740000001/common/f1/2026/audi/2026audicarright.webp",
    "cadillac" to "https://media.formula1.com/image/upload/c_lfill,w_512/q_auto/d_common:f1:2026:fallback:car:2026fallbackcarright.webp/v1740000001/common/f1/2026/cadillac/2026cadillaccarright.webp",
    "ferrari" to "https://media.formula1.com/image/upload/c_lfill,w_512/q_auto/d_common:f1:2026:fallback:car:2026fallbackcarright.webp/v1740000001/common/f1/2026/ferrari/2026ferraricarright.webp",
    "haas" to "https://media.formula1.com/image/upload/c_lfill,w_512/q_auto/d_common:f1:2026:fallback:car:2026fallbackcarright.webp/v1740000001/common/f1/2026/haasf1team/2026haasf1teamcarright.webp",
    "mclaren" to "https://media.formula1.com/image/upload/c_lfill,w_512/q_auto/d_common:f1:2026:fallback:car:2026fallbackcarright.webp/v1740000001/common/f1/2026/mclaren/2026mclarencarright.webp",
    "mercedes" to "https://media.formula1.com/image/upload/c_lfill,w_512/q_auto/d_common:f1:2026:fallback:car:2026fallbackcarright.webp/v1740000001/common/f1/2026/mercedes/2026mercedescarright.webp",
    "racing bulls" to "https://media.formula1.com/image/upload/c_lfill,w_512/q_auto/d_common:f1:2026:fallback:car:2026fallbackcarright.webp/v1740000001/common/f1/2026/racingbulls/2026racingbullscarright.webp",
    "rb f1 team" to "https://media.formula1.com/image/upload/c_lfill,w_512/q_auto/d_common:f1:2026:fallback:car:2026fallbackcarright.webp/v1740000001/common/f1/2026/racingbulls/2026racingbullscarright.webp",
    "red bull racing" to "https://media.formula1.com/image/upload/c_lfill,w_512/q_auto/d_common:f1:2026:fallback:car:2026fallbackcarright.webp/v1740000001/common/f1/2026/redbullracing/2026redbullracingcarright.webp",
    "williams" to "https://media.formula1.com/image/upload/c_lfill,w_512/q_auto/d_common:f1:2026:fallback:car:2026fallbackcarright.webp/v1740000001/common/f1/2026/williams/2026williamscarright.webp"
)

/**
 * Looks up a team's car image by name. Tries an exact match first (case-insensitive) to
 * avoid risky substring collisions between unrelated team names, then falls back to a
 * loose containment check only if no exact match is found.
 */
fun resolveTeamCarPhoto(teamName: String): String? {
    val key = teamName.trim().lowercase()
    teamCarPhotos.forEach { (mapKey, url) ->
        if (mapKey.equals(key, ignoreCase = true)) return url
    }
    teamCarPhotos.forEach { (mapKey, url) ->
        if (teamName.contains(mapKey, ignoreCase = true) || mapKey.contains(teamName, ignoreCase = true)) {
            return url
        }
    }
    return null
}
