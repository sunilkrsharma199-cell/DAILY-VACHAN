package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.audio.VoicePlaybackService
import com.example.data.local.DevotionalEntity
import com.example.data.model.TimeSlot

object NotificationHelper {

    const val CHANNEL_DAILY = "daily_voice_channel"
    const val CHANNEL_PLAYBACK = "playback_channel"
    const val DAILY_NOTIFICATION_ID = 1001

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val dailyChannel = NotificationChannel(
                CHANNEL_DAILY,
                context.getString(R.string.channel_daily_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.channel_daily_desc)
                enableVibration(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }

            val playbackChannel = NotificationChannel(
                CHANNEL_PLAYBACK,
                context.getString(R.string.channel_playback_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.channel_playback_desc)
                setShowBadge(false)
            }

            manager.createNotificationChannel(dailyChannel)
            manager.createNotificationChannel(playbackChannel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun postDailyVoiceNotification(
        context: Context,
        entity: DevotionalEntity,
        oneTapAudioEnabled: Boolean
    ): Boolean {
        if (!hasNotificationPermission(context)) return false
        ensureChannels(context)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(VoicePlaybackService.EXTRA_DEVOTIONAL_ID, entity.id)
        }
        val openPending = PendingIntent.getActivity(
            context,
            200,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val slot = TimeSlot.fromKey(entity.timeSlot)
        val greeting = if (slot == TimeSlot.MORNING) "Morning Vachan" else "Evening Peace"

        val builder = NotificationCompat.Builder(context, CHANNEL_DAILY)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("$greeting • ${entity.verseReference}")
            .setContentText(entity.verseText)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("${entity.verseText}\n\nTap \"Play Voice\" to listen right away.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(openPending)

        if (oneTapAudioEnabled) {
            val playPending = VoicePlaybackService.buildPlayPendingIntent(
                context = context,
                devotionalId = entity.id,
                slot = slot,
                requestCode = 201
            )
            builder.addAction(
                android.R.drawable.ic_media_play,
                "Play Voice",
                playPending
            )
        }

        NotificationManagerCompat.from(context).notify(DAILY_NOTIFICATION_ID, builder.build())
        return true
    }
}
