package com.example.audio

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.MediaPlayer
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.DailyVachanDatabase
import com.example.data.local.DevotionalEntity
import com.example.data.local.DevotionalRepository
import com.example.data.local.UserPreferencesRepository
import com.example.data.model.DevotionalLanguage
import com.example.data.model.TimeSlot
import com.example.notifications.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

class VoicePlaybackService : Service() {

    companion object {
        private const val TAG = "VoicePlaybackService"
        const val ACTION_PLAY_DEVOTIONAL = "com.example.action.PLAY_DEVOTIONAL"
        const val ACTION_TOGGLE_PAUSE = "com.example.action.TOGGLE_PAUSE"
        const val ACTION_STOP = "com.example.action.STOP"
        const val ACTION_UPDATE_AMBIENT = "com.example.action.UPDATE_AMBIENT"
        const val EXTRA_DEVOTIONAL_ID = "extra_devotional_id"
        const val EXTRA_TIME_SLOT = "extra_time_slot"
        const val NOTIFICATION_ID = 2001

        fun playDevotional(context: Context, devotionalId: String? = null, slot: TimeSlot? = null) {
            val intent = Intent(context, VoicePlaybackService::class.java).apply {
                action = ACTION_PLAY_DEVOTIONAL
                devotionalId?.let { putExtra(EXTRA_DEVOTIONAL_ID, it) }
                slot?.let { putExtra(EXTRA_TIME_SLOT, it.key) }
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun buildPlayPendingIntent(
            context: Context,
            devotionalId: String? = null,
            slot: TimeSlot? = null,
            requestCode: Int = 101
        ): PendingIntent {
            val intent = Intent(context, VoicePlaybackService::class.java).apply {
                action = ACTION_PLAY_DEVOTIONAL
                devotionalId?.let { putExtra(EXTRA_DEVOTIONAL_ID, it) }
                slot?.let { putExtra(EXTRA_TIME_SLOT, it.key) }
            }
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                PendingIntent.getForegroundService(context, requestCode, intent, flags)
            } else {
                PendingIntent.getService(context, requestCode, intent, flags)
            }
        }

        fun togglePause(context: Context) {
            val intent = Intent(context, VoicePlaybackService::class.java).apply {
                action = ACTION_TOGGLE_PAUSE
            }
            context.startService(intent)
        }

        fun stopPlayback(context: Context) {
            val intent = Intent(context, VoicePlaybackService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun refreshAmbientSettings(context: Context) {
            val intent = Intent(context, VoicePlaybackService::class.java).apply {
                action = ACTION_UPDATE_AMBIENT
            }
            context.startService(intent)
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val ambientEngine = AmbientPadEngine()

    private lateinit var mediaSession: MediaSession
    private lateinit var audioManager: AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null

    private var mediaPlayer: MediaPlayer? = null
    private var tts: TextToSpeech? = null
    private var progressJob: Job? = null
    private var currentEntity: DevotionalEntity? = null
    private var currentSpeechRate: Float = 0.95f
    private var ambientEnabled: Boolean = true
    private var ambientVolume: Float = 0.28f

    private val audioFocusListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                if (PlaybackStateHolder.state.value.isPlaying) {
                    pausePlayback()
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                ambientEngine.setVolume((ambientVolume * 0.35f))
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                ambientEngine.setVolume(ambientVolume)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.ensureChannels(this)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        mediaSession = MediaSession(this, "DailyVachanSession").apply {
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() {
                    resumePlayback()
                }

                override fun onPause() {
                    pausePlayback()
                }

                override fun onStop() {
                    stopAllAndShutdown()
                }
            })
            isActive = true
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY_DEVOTIONAL -> {
                val devotionalId = intent.getStringExtra(EXTRA_DEVOTIONAL_ID)
                val slotKey = intent.getStringExtra(EXTRA_TIME_SLOT)
                startForeground(NOTIFICATION_ID, buildMediaNotification(null, isPlaying = true))
                serviceScope.launch {
                    loadAndPlay(devotionalId, slotKey)
                }
            }
            ACTION_TOGGLE_PAUSE -> {
                val current = PlaybackStateHolder.state.value
                if (current.isPlaying) {
                    pausePlayback()
                } else if (current.isPaused && currentEntity != null) {
                    resumePlayback()
                }
            }
            ACTION_UPDATE_AMBIENT -> {
                serviceScope.launch {
                    val prefs = UserPreferencesRepository(applicationContext).getCurrent()
                    ambientEnabled = prefs.ambientMusicEnabled
                    ambientVolume = prefs.ambientMusicVolume
                    if (PlaybackStateHolder.state.value.isPlaying && ambientEnabled) {
                        ambientEngine.start(ambientVolume)
                    } else {
                        ambientEngine.stop()
                    }
                }
            }
            ACTION_STOP -> stopAllAndShutdown()
        }
        return START_NOT_STICKY
    }

    private suspend fun loadAndPlay(devotionalId: String?, slotKey: String?) {
        PlaybackStateHolder.update {
            it.copy(isPreparing = true, errorMessage = null)
        }
        val db = DailyVachanDatabase.getInstance(applicationContext)
        val repo = DevotionalRepository(db.devotionalDao())
        val prefs = UserPreferencesRepository(applicationContext).getCurrent()
        currentSpeechRate = prefs.speechRate
        ambientEnabled = prefs.ambientMusicEnabled
        ambientVolume = prefs.ambientMusicVolume

        val entity = if (!devotionalId.isNullOrBlank()) {
            repo.getById(devotionalId)
        } else {
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val resolvedSlot = slotKey?.let { TimeSlot.fromKey(it) }
                ?: if (hour < 16) TimeSlot.MORNING else TimeSlot.EVENING
            repo.getOrCreateTodayDevotional(resolvedSlot, prefs.selectedLanguage, 0)
        }

        if (entity == null) {
            PlaybackStateHolder.update {
                it.copy(isPreparing = false, isPlaying = false, errorMessage = "Devotional not found.")
            }
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }

        currentEntity = entity
        repo.markPlayed(entity.id)
        requestAudioFocus()
        updateMediaMetadata(entity)

        if (ambientEnabled) {
            ambientEngine.start(ambientVolume)
        } else {
            ambientEngine.stop()
        }

        val cachedFile = OfflineAudioCacheManager.getAudioFile(applicationContext, entity.id)
        if (cachedFile.exists() && cachedFile.length() > 1024L) {
            playWavFile(entity, cachedFile.absolutePath)
        } else {
            playWithLiveTts(entity)
            // Cache in background so future offline playback has instant seekable audio
            serviceScope.launch(Dispatchers.IO) {
                val ok = OfflineAudioCacheManager.synthesizeToCache(
                    applicationContext,
                    entity,
                    currentSpeechRate
                )
                if (ok) {
                    repo.markAudioCached(entity.id, true)
                }
            }
        }
    }

    private fun playWavFile(entity: DevotionalEntity, path: String) {
        releaseEnginesOnly()
        try {
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                setDataSource(path)
                prepare()
                setOnCompletionListener {
                    onTrackCompleted()
                }
                start()
            }
            mediaPlayer = player
            val duration = player.duration.toLong().coerceAtLeast(1L)
            PlaybackStateHolder.update {
                PlaybackUiState(
                    activeDevotional = entity,
                    isPlaying = true,
                    isPaused = false,
                    isPreparing = false,
                    progressFraction = 0f,
                    elapsedMs = 0L,
                    durationMs = duration,
                    isOfflineFilePlayback = true
                )
            }
            updateMediaPlaybackState(PlaybackState.STATE_PLAYING, 0L)
            startForeground(NOTIFICATION_ID, buildMediaNotification(entity, isPlaying = true))
            startMediaPlayerProgressLoop(duration)
        } catch (e: Exception) {
            Log.e(TAG, "MediaPlayer failed, falling back to live TTS", e)
            playWithLiveTts(entity)
        }
    }

    private fun startMediaPlayerProgressLoop(durationMs: Long) {
        progressJob?.cancel()
        progressJob = serviceScope.launch {
            while (isActive) {
                val player = mediaPlayer
                if (player != null && player.isPlaying) {
                    val pos = player.currentPosition.toLong()
                    val fraction = (pos.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                    PlaybackStateHolder.update {
                        it.copy(
                            elapsedMs = pos,
                            durationMs = durationMs,
                            progressFraction = fraction
                        )
                    }
                }
                delay(250L)
            }
        }
    }

    private fun playWithLiveTts(entity: DevotionalEntity) {
        releaseEnginesOnly()
        val script = entity.fullSpokenScript()
        val estimatedDurationMs = ((script.length / 13.0) * 1000.0 / currentSpeechRate).toLong().coerceAtLeast(8000L)

        PlaybackStateHolder.update {
            PlaybackUiState(
                activeDevotional = entity,
                isPlaying = true,
                isPaused = false,
                isPreparing = false,
                progressFraction = 0f,
                elapsedMs = 0L,
                durationMs = estimatedDurationMs,
                isOfflineFilePlayback = false
            )
        }
        updateMediaPlaybackState(PlaybackState.STATE_PLAYING, 0L)
        startForeground(NOTIFICATION_ID, buildMediaNotification(entity, isPlaying = true))

        tts = TextToSpeech(applicationContext) { status ->
            if (status != TextToSpeech.SUCCESS) {
                PlaybackStateHolder.update {
                    it.copy(
                        isPlaying = false,
                        isPreparing = false,
                        errorMessage = "Device voice engine unavailable. Read the verse below peacefully."
                    )
                }
                ambientEngine.stop()
                return@TextToSpeech
            }
            val lang = DevotionalLanguage.fromCode(entity.languageCode)
            val locale = Locale.forLanguageTag(lang.ttsLocaleTag)
            val availability = tts?.setLanguage(locale) ?: TextToSpeech.LANG_NOT_SUPPORTED
            if (availability == TextToSpeech.LANG_MISSING_DATA ||
                availability == TextToSpeech.LANG_NOT_SUPPORTED
            ) {
                tts?.setLanguage(Locale("en", "IN"))
            }
            tts?.setSpeechRate(currentSpeechRate)
            tts?.setPitch(0.98f)
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    startTtsEstimatedProgressLoop(estimatedDurationMs)
                }

                override fun onDone(utteranceId: String?) {
                    serviceScope.launch { onTrackCompleted() }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    serviceScope.launch { onTrackCompleted() }
                }
            })
            val params = Bundle()
            tts?.speak(script, TextToSpeech.QUEUE_FLUSH, params, "live_${entity.id}")
        }
    }

    private fun startTtsEstimatedProgressLoop(estimatedMs: Long) {
        progressJob?.cancel()
        progressJob = serviceScope.launch {
            val start = System.currentTimeMillis()
            while (isActive && PlaybackStateHolder.state.value.isPlaying) {
                val elapsed = (System.currentTimeMillis() - start).coerceAtMost(estimatedMs)
                val fraction = (elapsed.toFloat() / estimatedMs.toFloat()).coerceIn(0f, 0.98f)
                PlaybackStateHolder.update {
                    it.copy(elapsedMs = elapsed, durationMs = estimatedMs, progressFraction = fraction)
                }
                delay(300L)
            }
        }
    }

    private fun pausePlayback() {
        progressJob?.cancel()
        try {
            mediaPlayer?.let {
                if (it.isPlaying) it.pause()
            }
            tts?.stop()
        } catch (_: Exception) {
        }
        ambientEngine.stop()
        PlaybackStateHolder.update {
            it.copy(isPlaying = false, isPaused = true)
        }
        updateMediaPlaybackState(PlaybackState.STATE_PAUSED, PlaybackStateHolder.state.value.elapsedMs)
        startForeground(NOTIFICATION_ID, buildMediaNotification(currentEntity, isPlaying = false))
    }

    private fun resumePlayback() {
        val entity = currentEntity ?: return
        requestAudioFocus()
        if (ambientEnabled) {
            ambientEngine.start(ambientVolume)
        }
        val player = mediaPlayer
        if (player != null) {
            try {
                player.start()
                PlaybackStateHolder.update { it.copy(isPlaying = true, isPaused = false) }
                updateMediaPlaybackState(PlaybackState.STATE_PLAYING, player.currentPosition.toLong())
                startForeground(NOTIFICATION_ID, buildMediaNotification(entity, isPlaying = true))
                startMediaPlayerProgressLoop(player.duration.toLong().coerceAtLeast(1L))
                return
            } catch (_: Exception) {
            }
        }
        playWithLiveTts(entity)
    }

    private fun onTrackCompleted() {
        progressJob?.cancel()
        ambientEngine.stop()
        PlaybackStateHolder.update {
            it.copy(
                isPlaying = false,
                isPaused = false,
                progressFraction = 1f,
                elapsedMs = it.durationMs
            )
        }
        updateMediaPlaybackState(PlaybackState.STATE_STOPPED, 0L)
        stopForeground(STOP_FOREGROUND_DETACH)
        val notification = buildMediaNotification(currentEntity, isPlaying = false)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun stopAllAndShutdown() {
        progressJob?.cancel()
        releaseEnginesOnly()
        ambientEngine.stop()
        abandonAudioFocus()
        PlaybackStateHolder.reset()
        updateMediaPlaybackState(PlaybackState.STATE_STOPPED, 0L)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun releaseEnginesOnly() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
        } catch (_: Exception) {
        } finally {
            mediaPlayer = null
        }

        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {
        } finally {
            tts = null
        }
    }

    private fun updateMediaMetadata(entity: DevotionalEntity) {
        val metadata = MediaMetadata.Builder()
            .putString(MediaMetadata.METADATA_KEY_TITLE, entity.verseReference)
            .putString(MediaMetadata.METADATA_KEY_ARTIST, "Daily Vachan • ${entity.moodTag}")
            .putString(MediaMetadata.METADATA_KEY_ALBUM, DevotionalLanguage.fromCode(entity.languageCode).nativeLabel)
            .build()
        mediaSession.setMetadata(metadata)
    }

    private fun updateMediaPlaybackState(stateCode: Int, positionMs: Long) {
        val actions = PlaybackState.ACTION_PLAY or
            PlaybackState.ACTION_PAUSE or
            PlaybackState.ACTION_PLAY_PAUSE or
            PlaybackState.ACTION_STOP
        val state = PlaybackState.Builder()
            .setActions(actions)
            .setState(stateCode, positionMs, 1.0f)
            .build()
        mediaSession.setPlaybackState(state)
    }

    private fun buildMediaNotification(entity: DevotionalEntity?, isPlaying: Boolean): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPending = PendingIntent.getActivity(
            this,
            10,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val toggleIntent = Intent(this, VoicePlaybackService::class.java).apply {
            action = ACTION_TOGGLE_PAUSE
        }
        val togglePending = PendingIntent.getService(
            this,
            11,
            toggleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, VoicePlaybackService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPending = PendingIntent.getService(
            this,
            12,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = entity?.verseReference ?: getString(R.string.app_name)
        val subtitle = entity?.verseText ?: "Preparing today's devotional voice..."
        val toggleLabel = if (isPlaying) getString(R.string.action_pause_voice) else getString(R.string.action_play_voice)
        val toggleIcon = if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, NotificationHelper.CHANNEL_PLAYBACK)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(subtitle)
                .setContentIntent(openPending)
                .setOngoing(isPlaying)
                .addAction(Notification.Action.Builder(toggleIcon, toggleLabel, togglePending).build())
                .addAction(
                    Notification.Action.Builder(
                        android.R.drawable.ic_menu_close_clear_cancel,
                        getString(R.string.action_stop_voice),
                        stopPending
                    ).build()
                )
                .setStyle(
                    Notification.MediaStyle()
                        .setMediaSession(mediaSession.sessionToken)
                        .setShowActionsInCompactView(0, 1)
                )
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(subtitle)
                .setContentIntent(openPending)
                .addAction(toggleIcon, toggleLabel, togglePending)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, getString(R.string.action_stop_voice), stopPending)
                .setStyle(
                    Notification.MediaStyle()
                        .setMediaSession(mediaSession.sessionToken)
                        .setShowActionsInCompactView(0, 1)
                )
                .build()
        }
    }

    private fun requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setOnAudioFocusChangeListener(audioFocusListener)
                .build()
            audioFocusRequest = req
            audioManager.requestAudioFocus(req)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                audioFocusListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(audioFocusListener)
        }
    }

    override fun onDestroy() {
        progressJob?.cancel()
        releaseEnginesOnly()
        ambientEngine.stop()
        abandonAudioFocus()
        mediaSession.release()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
