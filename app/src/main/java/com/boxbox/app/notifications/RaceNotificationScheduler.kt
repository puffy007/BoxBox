package com.boxbox.app.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.boxbox.app.data.model.Race
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

object RaceNotificationScheduler {

    fun scheduleRaceNotification(context: Context, race: Race) {
        try {
            val dateStr = "${race.date}T${race.time ?: "00:00:00Z"}"
            val raceTime = ZonedDateTime.parse(dateStr, DateTimeFormatter.ISO_DATE_TIME)
            val notifyTime = raceTime.minusMinutes(30).toInstant().toEpochMilli()

            if (notifyTime <= System.currentTimeMillis()) return

            val intent = Intent(context, RaceAlarmReceiver::class.java).apply {
                putExtra("title", "🏎️ ${race.raceName} in 30 minutes!")
                putExtra("body", "Get ready — the race starts soon at ${race.Circuit.Location.locality}")
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                race.round.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, notifyTime, pendingIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun cancelRaceNotification(context: Context, race: Race) {
        val intent = Intent(context, RaceAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            race.round.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
    }
}
