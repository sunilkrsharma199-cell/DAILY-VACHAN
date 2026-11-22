package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.audio.OfflineAudioCacheManager
import com.example.data.local.DailyVachanDatabase
import com.example.data.local.DevotionalRepository
import com.example.data.local.UserPreferencesRepository
import com.example.data.model.TimeSlot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class DailyReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val pendingResult = goAsync()
        val action = intent?.action

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val appContext = context.applicationContext
                val prefs = UserPreferencesRepository(appContext).getCurrent()
                val dao = DailyVachanDatabase.getInstance(appContext).devotionalDao()
                val repo = DevotionalRepository(dao)

                when (action) {
                    DailyAlarmScheduler.ACTION_MORNING_REMINDER -> {
                        val devotional = repo.getOrCreateTodayDevotional(
                            TimeSlot.MORNING,
                            prefs.selectedLanguage,
                            0
                        )
                        NotificationHelper.postDailyVoiceNotification(
                            appContext,
                            devotional,
                            prefs.oneTapNotificationAudio
                        )
                    }
                    DailyAlarmScheduler.ACTION_EVENING_REMINDER -> {
                        val devotional = repo.getOrCreateTodayDevotional(
                            TimeSlot.EVENING,
                            prefs.selectedLanguage,
                            0
                        )
                        NotificationHelper.postDailyVoiceNotification(
                            appContext,
                            devotional,
                            prefs.oneTapNotificationAudio
                        )
                    }
                    DailyAlarmScheduler.ACTION_QUICK_TEST_REMINDER -> {
                        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                        val slot = if (hour < 16) TimeSlot.MORNING else TimeSlot.EVENING
                        val devotional = repo.getOrCreateTodayDevotional(
                            slot,
                            prefs.selectedLanguage,
                            0
                        )
                        NotificationHelper.postDailyVoiceNotification(
                            appContext,
                            devotional,
                            prefs.oneTapNotificationAudio
                        )
                    }
                    DailyAlarmScheduler.ACTION_NIGHT_PRECACHE -> {
                        if (prefs.autoDownloadOffline) {
                            for (slot in listOf(TimeSlot.MORNING, TimeSlot.EVENING)) {
                                val tomorrow = repo.getOrCreateTodayDevotional(
                                    slot = slot,
                                    language = prefs.selectedLanguage,
                                    daysOffset = 1
                                )
                                val ok = OfflineAudioCacheManager.synthesizeToCache(
                                    appContext,
                                    tomorrow,
                                    prefs.speechRate
                                )
                                if (ok) {
                                    repo.markAudioCached(tomorrow.id, true)
                                }
                            }
                        }
                    }
                }
                DailyAlarmScheduler.rescheduleAll(appContext, prefs)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
