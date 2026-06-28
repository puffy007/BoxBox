package com.boxbox.app.notifications

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Drives RaceCountdownWorker on a ~1-minute cadence for testing/dev purposes.
 *
 * WorkManager's PeriodicWorkRequest has a hard 15-minute minimum interval enforced by
 * the OS - there's no way to ask for "every 1 minute" with it. To get a 1-minute test
 * cadence, each run of RaceCountdownWorker enqueues the *next* run itself via a
 * OneTimeWorkRequest with a 1-minute initial delay, chaining indefinitely until
 * cancelled. This is intentionally a short-interval TEST mode - for the real "notify
 * 30 minutes before the race" feature, RaceNotificationScheduler's exact AlarmManager
 * alarm remains the right tool, since that only needs to fire once per race, not every
 * minute forever (which would drain battery in production).
 */
private const val WORK_NAME = "race_countdown_test"

object RaceCountdownScheduler {

    /** Starts the repeating 1-minute countdown notification chain. */
    fun start(context: Context) {
        val request = OneTimeWorkRequestBuilder<RaceCountdownWorker>()
            .setInitialDelay(0, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    /** Stops the chain - call this when the user disables race notifications. */
    fun stop(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    /**
     * Enqueues the next run, 1 minute from now. Called by RaceCountdownWorker itself
     * at the end of doWork() to keep the chain going - this is what RaceCountdownWorker
     * uses instead of returning Result.retry() (which has its own backoff timing we
     * don't want here).
     */
    fun scheduleNext(context: Context) {
        val request = OneTimeWorkRequestBuilder<RaceCountdownWorker>()
            .setInitialDelay(1, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}
