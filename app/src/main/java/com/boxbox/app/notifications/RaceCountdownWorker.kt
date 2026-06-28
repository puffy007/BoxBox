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
 * Periodic worker (runs every ~1 minute while enabled) that finds the next race on the
 * schedule and shows a notification describing how far away it is - e.g.
 * "Race in 3h 24m" or, once the scheduled start time has passed, "Race is on now!".
 *
 * This is a TEST/dev tool to verify the countdown text and that notifications actually
 * arrive on a real device, separate from RaceNotificationScheduler's one-shot "30
 * minutes before" alarm. It's driven by WorkManager (not AlarmManager) because we want
 * a repeating job, and WorkManager handles periodic background work more reliably
 * across Doze/battery-optimization than a self-rescheduling AlarmManager chain would.
 *
 * Per WorkManager's own constraints, the OS will not run periodic work more often than
 * every 15 minutes in production - see RaceCountdownScheduler for how the 1-minute
 * interval requested here is actually achieved for testing.
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

            // Keep the chain going - schedule the next check 1 minute from now.
            RaceCountdownScheduler.scheduleNext(applicationContext)
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            // Still keep the chain alive even if this particular run failed (e.g.
            // a transient network error fetching the schedule).
            RaceCountdownScheduler.scheduleNext(applicationContext)
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
