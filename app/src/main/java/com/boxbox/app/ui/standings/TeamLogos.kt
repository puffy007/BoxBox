package com.boxbox.app.ui.standings

/**
 * Team logo (white version) image URLs, scraped directly from formula1.com/en/teams
 * (Cloudinary CDN, confirmed real by fetching that page's HTML - the team-switcher nav
 * on every team page includes these exact logo URLs alongside each team name).
 *
 * Like the car silhouette images, there's no equivalent in OpenF1 or Jolpica - neither
 * API returns any kind of team logo asset - so this is a small, explicit, documented
 * exception alongside teamCarPhotos.kt and driverHighResPhotos. Standings data itself
 * (position, points, wins) still comes entirely from Jolpica.
 *
 * Keyed the same way as teamCarPhotos.kt and teamAccentColors, including the
 * "RB F1 Team" alias since Jolpica's Constructor.name can use either that or
 * "Racing Bulls" depending on data source/season.
 */
val teamLogos: Map<String, String> = mapOf(
    "alpine" to "https://media.formula1.com/image/upload/c_lfill,w_48/q_auto/v1740000001/common/f1/2026/alpine/2026alpinelogowhite.webp",
    "aston martin" to "https://media.formula1.com/image/upload/c_lfill,w_48/q_auto/v1740000001/common/f1/2026/astonmartin/2026astonmartinlogowhite.webp",
    "audi" to "https://media.formula1.com/image/upload/c_lfill,w_48/q_auto/v1740000001/common/f1/2026/audi/2026audilogowhite.webp",
    "cadillac" to "https://media.formula1.com/image/upload/c_lfill,w_48/q_auto/v1740000001/common/f1/2026/cadillac/2026cadillaclogowhite.webp",
    "ferrari" to "https://media.formula1.com/image/upload/c_lfill,w_48/q_auto/v1740000001/common/f1/2026/ferrari/2026ferrarilogowhite.webp",
    "haas" to "https://media.formula1.com/image/upload/c_lfill,w_48/q_auto/v1740000001/common/f1/2026/haasf1team/2026haasf1teamlogowhite.webp",
    "mclaren" to "https://media.formula1.com/image/upload/c_lfill,w_48/q_auto/v1740000001/common/f1/2026/mclaren/2026mclarenlogowhite.webp",
    "mercedes" to "https://media.formula1.com/image/upload/c_lfill,w_48/q_auto/v1740000001/common/f1/2026/mercedes/2026mercedeslogowhite.webp",
    "racing bulls" to "https://media.formula1.com/image/upload/c_lfill,w_48/q_auto/v1740000001/common/f1/2026/racingbulls/2026racingbullslogowhite.webp",
    "rb f1 team" to "https://media.formula1.com/image/upload/c_lfill,w_48/q_auto/v1740000001/common/f1/2026/racingbulls/2026racingbullslogowhite.webp",
    "red bull racing" to "https://media.formula1.com/image/upload/c_lfill,w_48/q_auto/v1740000001/common/f1/2026/redbullracing/2026redbullracinglogowhite.webp",
    "williams" to "https://media.formula1.com/image/upload/c_lfill,w_48/q_auto/v1740000001/common/f1/2026/williams/2026williamslogowhite.webp"
)

/**
 * Looks up a team's logo by name. Tries an exact match first (case-insensitive) to
 * avoid risky substring collisions between unrelated team names, then falls back to a
 * loose containment check only if no exact match is found.
 */
fun resolveTeamLogo(teamName: String): String? {
    val key = teamName.trim().lowercase()
    teamLogos.forEach { (mapKey, url) ->
        if (mapKey.equals(key, ignoreCase = true)) return url
    }
    teamLogos.forEach { (mapKey, url) ->
        if (teamName.contains(mapKey, ignoreCase = true) || mapKey.contains(teamName, ignoreCase = true)) {
            return url
        }
    }
    return null
}
