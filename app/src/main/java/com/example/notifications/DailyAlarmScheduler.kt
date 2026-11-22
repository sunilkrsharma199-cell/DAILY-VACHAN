package com.example.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.data.model.UserPreferences
import java.util.Calendar

object DailyAlarmScheduler {

    const val ACTION_MORNING_REMINDER = "com.example.alarm.MORNING_REMINDER"
    const val ACTION_EVENING_REMINDER = "com.example.alarm.EVENING_REMINDER"
    const val ACTION_NIGHT_PRECACHE = "com.example.alarm.NIGHT_PRECACHE"
    const val ACTION_QUICK_TEST_REMINDER = "com.example.alarm.QUICK_TEST_REMINDER"

    private const val REQ_MORNING = 3001
    private const val REQ_EVENING = 3002
    private const val REQ_PRECACHE = 3003
    private const val REQ_QUICK_TEST = 3004

    fun rescheduleAll(context: Context, prefs: UserPreferences) {
        if (prefs.morningEnabled) {
            scheduleDailyAlarm(
                context = context,
                action = ACTION_MORNING_REMINDER,
                requestCode = REQ_MORNING,
                hour = prefs.morningHour,
                minute = prefs.morningMinute
            )
        } else {
            cancelAlarm(context, ACTION_MORNING_REMINDER, REQ_MORNING)
        }

        if (prefs.eveningEnabled) {
            scheduleDailyAlarm(
                context = context,
                action = ACTION_EVENING_REMINDER,
                requestCode = REQ_EVENING,
                hour = prefs.eveningHour,
                minute = prefs.eveningMinute
            )
        } else {
            cancelAlarm(context, ACTION_EVENING_REMINDER, REQ_EVENING)
        }

        if (prefs.autoDownloadOffline) {
            scheduleDailyAlarm(
                context = context,
                action = ACTION_NIGHT_PRECACHE,
                requestCode = REQ_PRECACHE,
                hour = 23,
                minute = 15
            )
        } else {
            cancelAlarm(context, ACTION_NIGHT_PRECACHE, REQ_PRECACHE)
        }
    }

    fun scheduleQuickReminderInSeconds(context: Context, seconds: Int = 10) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, DailyReminderReceiver::class.java).apply {
            action = ACTION_QUICK_TEST_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQ_QUICK_TEST,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val triggerAt = System.currentTimeMillis() + (seconds * 1000L)
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAt,
            pendingIntent
        )
    }

    private fun scheduleDailyAlarm(
        context: Context,
        action: String,
        requestCode: Int,
        hour: Int,
        minute: Int
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, DailyReminderReceiver::class.java).apply {
            this.action = action
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            cal.timeInMillis,
            pendingIntent
        )
    }

    private fun cancelAlarm(context: Context, action: String, requestCode: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, DailyReminderReceiver::class.java).apply {
            this.action = action
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}
