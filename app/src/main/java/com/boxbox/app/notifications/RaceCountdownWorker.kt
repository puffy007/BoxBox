package com.boxbox.app.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.boxbox.app.data.repository.BoxBoxRepository
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Periodic worker (runs on the interval the user picked, 1-12 hours) that finds the
 * next race on the schedule and shows a notification describing how far away it is -
 * e.g. "Race in 3h 24m" or, once the scheduled start time has passed, "Race is on now!".
 *
 * Scheduled by RaceCountdownScheduler as a real PeriodicWorkRequest - the user-chosen
 * interval (minimum 1 hour) is comfortably above WorkManager's own 15-minute minimum,
 * so no self-rescheduling chain is needed here; WorkManager handles the repetition.
 */
class RaceCountdownWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val repository = BoxBoxRepository()
            val races = repository.getCurrentSchedule()
            val now = LocalDateTime.now()

            val nextRace = races
                .mapNotNull { race ->
                    val raceDateTime = parseRaceDateTime(race.date, race.time) ?: return@mapNotNull null
                    race to raceDateTime
                }
                .filter { (_, raceDateTime) ->
                    // Keep races up to 3 hours after their scheduled start, so the "the
                    // race is on now" message stays visible for roughly a race's
                    // duration instead of disappearing the instant the start time passes.
                    raceDateTime.isAfter(now.minusHours(3))
                }
                .minByOrNull { (_, raceDateTime) -> raceDateTime }

            if (nextRace != null) {
                val (race, raceDateTime) = nextRace
                val title = "🏎️ ${race.raceName}"
                val body = describeCountdown(now, raceDateTime)
                showNotification(applicationContext, title, body, channelId = CHANNEL_ID_RACE)
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.success()
        }
    }

    private fun parseRaceDateTime(date: String, time: String?): LocalDateTime? {
        return try {
            val localDate = LocalDate.parse(date)
            val localTime = time
                ?.removeSuffix("Z")
                ?.let { LocalTime.parse(it, DateTimeFormatter.ofPattern("HH:mm:ss")) }
                ?: LocalTime.MIDNIGHT
            LocalDateTime.of(localDate, localTime)
        } catch (e: Exception) {
            null
        }
    }

    private fun describeCountdown(now: LocalDateTime, raceDateTime: LocalDateTime): String {
        if (now.isAfter(raceDateTime)) {
            return "The race is on now! 🏁"
        }
        val duration = Duration.between(now, raceDateTime)
        val totalMinutes = duration.toMinutes()
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return when {
            hours > 0 -> "Race in ${hours}h ${minutes}m"
            else -> "Race in ${minutes}m"
        }
    }
}