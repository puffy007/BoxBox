package com.boxbox.app.data.repository

import com.boxbox.app.data.model.Driver
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.text.Normalizer

/**
 * Extension helpers used by Driver/Team detail screens and the Standings list.
 *
 * Pure OpenF1 approach (no static/hardcoded URLs): to reliably get a photo for every
 * driver on the current grid - including ones missing from the single "latest" session
 * (reserve stand-ins, drivers who sat out a session, etc.) - this walks through every
 * session of the current season and merges driver records until it has a full grid.
 *
 * The expensive part (many parallel session calls) only happens ONCE per app process,
 * thanks to an in-memory cache. Every screen that needs driver photos reads from that
 * cache instead of re-querying OpenF1, so navigating between Standings / Driver Detail /
 * Team Detail doesn't repeatedly hit the API or risk OpenF1's rate limit.
 *
 * Names are compared with accents/diacritics stripped (normalizeForMatch), since Jolpica
 * and OpenF1 don't always agree on whether a name has e.g. "Hülkenberg" vs "Hulkenberg".
 */

/** Removes accents/diacritics so "Hülkenberg" and "Hulkenberg" compare as equal. */
fun normalizeForMatch(input: String): String {
    val decomposed = Normalizer.normalize(input, Normalizer.Form.NFD)
    return decomposed.replace(Regex("[\\p{Mn}]"), "").trim().lowercase()
}

private fun surnameKey(fullName: String): String =
    normalizeForMatch(fullName.trim().split(" ").lastOrNull().orEmpty())

// Process-lifetime cache: built once, reused by every screen/ViewModel.
private object DriverPhotoCache {
    @Volatile var cached: Map<String, Driver>? = null
    val lock = Any()
}

/**
 * Returns surname -> Driver map covering the full current-season grid, built by walking
 * OpenF1 sessions (in small parallel batches, stopping once ~20 drivers are found) and
 * caching the result for the lifetime of the app process.
 */
suspend fun BoxBoxRepository.allDriversBySurname(): Map<String, Driver> {
    DriverPhotoCache.cached?.let { return it }

    val result = coroutineScope {
        val merged = mutableMapOf<String, Driver>()

        fun mergeIn(list: List<Driver>) {
            list.forEach { d ->
                val key = surnameKey(d.full_name)
                if (key.isNotEmpty()) merged.putIfAbsent(key, d)
            }
        }

        // Fast path: "latest" usually covers most of the grid in one call.
        runCatching { getDrivers("latest") }.getOrNull()?.let(::mergeIn)

        if (merged.size < 20) {
            val currentYear = java.time.Year.now().value
            val sessions = runCatching { getSessions(currentYear) }.getOrNull().orEmpty()
                .sortedByDescending { it.date_start }

            // Walk sessions in small batches so we don't fire 60-100 calls at once
            // (OpenF1's free tier allows 30 req/10s), stopping as soon as the grid is full.
            val batchSize = 6
            var index = 0
            while (index < sessions.size && merged.size < 20) {
                val batch = sessions.subList(index, minOf(index + batchSize, sessions.size))
                val results = batch.map { session ->
                    async { runCatching { getDrivers(session.session_key.toString()) }.getOrNull().orEmpty() }
                }.awaitAll()
                results.forEach(::mergeIn)
                index += batchSize
            }

            // If the current season still doesn't have a full grid (e.g. very early in
            // the year before OpenF1 has published many sessions), check last season too.
            if (merged.size < 20) {
                val prevSessions = runCatching { getSessions(currentYear - 1) }.getOrNull().orEmpty()
                    .sortedByDescending { it.date_start }
                    .take(6)
                if (prevSessions.isNotEmpty()) {
                    val results = prevSessions.map { session ->
                        async { runCatching { getDrivers(session.session_key.toString()) }.getOrNull().orEmpty() }
                    }.awaitAll()
                    results.forEach(::mergeIn)
                }
            }
        }

        merged.toMap()
    }

    DriverPhotoCache.cached = result
    return result
}

suspend fun BoxBoxRepository.findDriverByName(familyName: String, givenName: String): Driver? {
    return try {
        val all = allDriversBySurname().values
        val familyKey = normalizeForMatch(familyName)
        val givenKey = normalizeForMatch(givenName)
        all.firstOrNull { d ->
            val nameKey = normalizeForMatch(d.full_name)
            nameKey.contains(familyKey) && nameKey.contains(givenKey)
        } ?: all.firstOrNull { d ->
            normalizeForMatch(d.full_name).contains(familyKey)
        }
    } catch (e: Exception) {
        null
    }
}

suspend fun BoxBoxRepository.findDriversByTeam(teamName: String): List<Driver> {
    return try {
        val teamKey = normalizeForMatch(teamName)
        allDriversBySurname().values.filter {
            val driverTeamKey = normalizeForMatch(it.team_name)
            driverTeamKey.contains(teamKey) || teamKey.contains(driverTeamKey)
        }
    } catch (e: Exception) {
        emptyList()
    }
}

/** Builds a surname -> Driver map (accent-normalized keys) for use by the Standings list. */
suspend fun BoxBoxRepository.allRecentDriversBySurname(): Map<String, Driver> {
    return try {
        allDriversBySurname()
    } catch (e: Exception) {
        emptyMap()
    }
}
