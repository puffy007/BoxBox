package com.boxbox.app.notifications

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Drives RaceCountdownWorker on a user-configurable interval (1-12 hours), using a real
 * PeriodicWorkRequest. The user picks the interval via a wheel picker in Profile
 * settings (see HoursWheelPicker); the minimum is clamped to 1 hour both in the UI and
 * here, comfortably above WorkManager/Android's own 15-minute minimum for periodic
 * work, so there's no OS-level clamping surprise.
 *
 * This is separate from RaceNotificationScheduler's one-shot "30 minutes before the
 * race" AlarmManager alarm, which fires once per race rather than repeating - that
 * stays as the precise pre-race reminder, while this is the repeating "how long until
 * the next race" countdown notification the user can tune to their liking.
 */
private const val WORK_NAME = "race_countdown"

object RaceCountdownScheduler {

    /**
     * Starts (or restarts, if already running with a different interval) the repeating
     * countdown notification on the given interval.
     */
    fun start(context: Context, intervalHours: Int) {
        val clampedHours = intervalHours.coerceIn(1, 12)
        val request = PeriodicWorkRequestBuilder<RaceCountdownWorker>(
            clampedHours.toLong(), TimeUnit.HOURS
        ).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    /** Stops the repeating countdown notification - call when the user disables it. */
    fun stop(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}