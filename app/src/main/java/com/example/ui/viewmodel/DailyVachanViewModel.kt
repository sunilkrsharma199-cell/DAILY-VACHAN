package com.example.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.BolkarSamadhanRepository
import com.example.audio.OfflineAudioCacheManager
import com.example.audio.PlaybackStateHolder
import com.example.audio.PlaybackUiState
import com.example.audio.VoicePlaybackService
import com.example.data.local.DevotionalEntity
import com.example.data.local.DevotionalRepository
import com.example.data.local.OfflineDevotionalCatalog
import com.example.data.local.UserPreferencesRepository
import com.example.data.model.DevotionalLanguage
import com.example.data.model.StatusCardTheme
import com.example.data.model.TimeSlot
import com.example.data.model.UserPreferences
import com.example.data.remote.CloudSavedVerse
import com.example.data.remote.CloudUserRepository
import com.example.data.remote.UserProfile
import com.example.notifications.DailyAlarmScheduler
import com.example.notifications.NotificationHelper
import com.example.share.StatusCardGenerator
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

sealed interface SamadhanUiState {
    data object Idle : SamadhanUiState
    data object Loading : SamadhanUiState
    data class Success(val devotional: DevotionalEntity) : SamadhanUiState
    data class Error(val message: String) : SamadhanUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
class DailyVachanViewModel(
    private val appContext: Context,
    private val currentUserId: String,
    private val devotionalRepo: DevotionalRepository,
    private val prefsRepo: UserPreferencesRepository,
    private val cloudRepo: CloudUserRepository,
    private val aiRepo: BolkarSamadhanRepository
) : ViewModel() {

    companion object {
        private const val TAG = "DailyVachanVM"
    }

    val preferences: StateFlow<UserPreferences> = prefsRepo.preferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPreferences()
        )

    private val _cloudSyncError = MutableStateFlow<String?>(null)
    val cloudSyncError: StateFlow<String?> = _cloudSyncError.asStateFlow()

    val userProfile: StateFlow<UserProfile?> = cloudRepo.observeProfile(currentUserId)
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing user profile", rootError)
            _cloudSyncError.value = rootError.localizedMessage
            emit(null)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    private val cloudSavedVerses: StateFlow<List<CloudSavedVerse>> =
        cloudRepo.observeSavedVerses(currentUserId)
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing cloud saved verses", rootError)
                _cloudSyncError.value = rootError.localizedMessage
                emit(emptyList())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    private val initialSlot = if (Calendar.getInstance().get(Calendar.HOUR_OF_DAY) < 16) {
        TimeSlot.MORNING
    } else {
        TimeSlot.EVENING
    }

    private val _selectedSlot = MutableStateFlow(initialSlot)
    val selectedSlot: StateFlow<TimeSlot> = _selectedSlot.asStateFlow()

    val todayDevotional: StateFlow<DevotionalEntity?> = combine(
        preferences.map { it.selectedLanguage },
        _selectedSlot
    ) { language, slot -> language to slot }
        .flatMapLatest { (language, slot) ->
            val todayKey = OfflineDevotionalCatalog.dateKeyOffset(0)
            devotionalRepo.observeDayDevotionals(todayKey, language)
                .map { list -> list.firstOrNull { it.timeSlot == slot.key } }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val allHistory: StateFlow<List<DevotionalEntity>> = devotionalRepo.observeAllHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bookmarkedHistory: StateFlow<List<DevotionalEntity>> = devotionalRepo.observeBookmarked()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val samadhanHistory: StateFlow<List<DevotionalEntity>> = devotionalRepo.observeSamadhanHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playbackState: StateFlow<PlaybackUiState> = PlaybackStateHolder.state

    private val _samadhanState = MutableStateFlow<SamadhanUiState>(SamadhanUiState.Idle)
    val samadhanState: StateFlow<SamadhanUiState> = _samadhanState.asStateFlow()

    private val _isRefreshingToday = MutableStateFlow(false)
    val isRefreshingToday: StateFlow<Boolean> = _isRefreshingToday.asStateFlow()

    private val _isPreparingOffline = MutableStateFlow(false)
    val isPreparingOffline: StateFlow<Boolean> = _isPreparingOffline.asStateFlow()

    private val _cacheStats = MutableStateFlow(0 to 0L)
    val cacheStats: StateFlow<Pair<Int, Long>> = _cacheStats.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _shareTarget = MutableStateFlow<DevotionalEntity?>(null)
    val shareTarget: StateFlow<DevotionalEntity?> = _shareTarget.asStateFlow()

    init {
        viewModelScope.launch {
            val currentUser = Firebase.auth.currentUser
            val initialPrefs = prefsRepo.getCurrent()
            if (currentUser != null && currentUser.uid == currentUserId) {
                val res = cloudRepo.ensureUserProfile(
                    user = currentUser,
                    phoneNumber = initialPrefs.phoneNumber.ifBlank { null },
                    language = initialPrefs.selectedLanguage,
                    morningEnabled = initialPrefs.morningEnabled,
                    eveningEnabled = initialPrefs.eveningEnabled
                )
                res.onFailure { _cloudSyncError.value = it.localizedMessage }
            }
        }

        // Sync cloud bookmarks back into local Room database so restored accounts see their verses
        viewModelScope.launch {
            cloudSavedVerses.collect { verses ->
                for (cloud in verses) {
                    val existing = devotionalRepo.getById(cloud.id)
                    if (existing == null) {
                        devotionalRepo.upsert(
                            DevotionalEntity(
                                id = cloud.id,
                                dateKey = cloud.dateKey,
                                timeSlot = cloud.timeSlot,
                                languageCode = cloud.languageCode,
                                verseReference = cloud.verseReference,
                                verseText = cloud.verseText,
                                reflection = cloud.reflection,
                                shortPrayer = cloud.shortPrayer,
                                moodTag = cloud.moodTag,
                                isBookmarked = cloud.bookmarked
                            )
                        )
                    } else if (existing.isBookmarked != cloud.bookmarked) {
                        devotionalRepo.setBookmarked(existing.id, cloud.bookmarked)
                    }
                }
            }
        }

        viewModelScope.launch {
            preferences.collect { prefs ->
                devotionalRepo.ensureSeededForLanguage(prefs.selectedLanguage)
                DailyAlarmScheduler.rescheduleAll(appContext, prefs)
                refreshCacheStats()
            }
        }
    }

    fun onNotificationPermissionGranted() {
        viewModelScope.launch {
            val prefs = prefsRepo.getCurrent()
            DailyAlarmScheduler.rescheduleAll(appContext, prefs)
            if (!prefs.welcomeNotificationSent) {
                val today = devotionalRepo.getOrCreateTodayDevotional(
                    slot = _selectedSlot.value,
                    language = prefs.selectedLanguage,
                    daysOffset = 0
                )
                val posted = NotificationHelper.postDailyVoiceNotification(
                    context = appContext,
                    entity = today,
                    oneTapAudioEnabled = prefs.oneTapNotificationAudio
                )
                if (posted) {
                    prefsRepo.markWelcomeNotificationSent()
                    _snackbarMessage.value = "Daily voice notifications enabled! Check your notification tray."
                }
            }
        }
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun showMessage(message: String) {
        _snackbarMessage.value = message
    }

    fun selectSlot(slot: TimeSlot) {
        _selectedSlot.value = slot
    }

    fun selectLanguage(language: DevotionalLanguage) {
        viewModelScope.launch {
            prefsRepo.setLanguage(language)
            devotionalRepo.ensureSeededForLanguage(language)
            val prefs = prefsRepo.getCurrent()
            cloudRepo.updateSyncPreferences(language, prefs.morningEnabled, prefs.eveningEnabled)
        }
    }

    fun saveMobileNumber(phoneNumber: String) {
        val clean = phoneNumber.trim()
        viewModelScope.launch {
            prefsRepo.setPendingPhoneNumber(clean)
            val res = cloudRepo.updatePhoneNumber(clean)
            _snackbarMessage.value = if (res.isSuccess) {
                "Mobile number saved to your profile."
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "Saved locally; cloud sync will retry."
            }
        }
    }

    fun playOrPause(entity: DevotionalEntity) {
        val current = playbackState.value
        if (current.activeDevotional?.id == entity.id && (current.isPlaying || current.isPaused)) {
            VoicePlaybackService.togglePause(appContext)
        } else {
            VoicePlaybackService.playDevotional(appContext, entity.id, TimeSlot.fromKey(entity.timeSlot))
        }
    }

    fun stopPlayback() {
        VoicePlaybackService.stopPlayback(appContext)
    }

    fun toggleBookmark(entity: DevotionalEntity) {
        viewModelScope.launch {
            val nextBookmarked = !entity.isBookmarked
            devotionalRepo.setBookmarked(entity.id, nextBookmarked)
            val samadhan = _samadhanState.value
            if (samadhan is SamadhanUiState.Success && samadhan.devotional.id == entity.id) {
                _samadhanState.value = SamadhanUiState.Success(
                    samadhan.devotional.copy(isBookmarked = nextBookmarked)
                )
            }
            cloudRepo.syncSavedVerse(entity, nextBookmarked)
            _snackbarMessage.value = if (nextBookmarked) {
                "Saved to Favorites & synced to cloud"
            } else {
                "Removed from Favorites"
            }
        }
    }

    fun prepareOfflineAudio(entity: DevotionalEntity) {
        if (_isPreparingOffline.value) return
        viewModelScope.launch {
            _isPreparingOffline.value = true
            try {
                val ok = OfflineAudioCacheManager.synthesizeToCache(
                    appContext,
                    entity,
                    preferences.value.speechRate
                )
                if (ok) {
                    devotionalRepo.markAudioCached(entity.id, true)
                    refreshCacheStats()
                    _snackbarMessage.value = "Saved offline audio for ${entity.verseReference}"
                } else {
                    _snackbarMessage.value = "Could not pre-cache audio on this device TTS engine; live voice playback still works."
                }
            } finally {
                _isPreparingOffline.value = false
            }
        }
    }

    fun precacheTomorrowAudio() {
        if (_isPreparingOffline.value) return
        viewModelScope.launch {
            _isPreparingOffline.value = true
            try {
                val prefs = preferences.value
                var count = 0
                for (slot in listOf(TimeSlot.MORNING, TimeSlot.EVENING)) {
                    val tomorrow = devotionalRepo.getOrCreateTodayDevotional(
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
                        devotionalRepo.markAudioCached(tomorrow.id, true)
                        count++
                    }
                }
                refreshCacheStats()
                _snackbarMessage.value = if (count > 0) {
                    "Tomorrow's Morning & Evening voice cached offline."
                } else {
                    "Device TTS offline synthesis unavailable; live voice remains ready."
                }
            } finally {
                _isPreparingOffline.value = false
            }
        }
    }

    fun clearOfflineAudioCache() {
        viewModelScope.launch {
            OfflineAudioCacheManager.clearCache(appContext)
            refreshCacheStats()
            _snackbarMessage.value = "Offline audio cache cleared."
        }
    }

    fun generateFreshTodayReflection() {
        if (_isRefreshingToday.value) return
        viewModelScope.launch {
            _isRefreshingToday.value = true
            try {
                val prefs = preferences.value
                val slot = _selectedSlot.value
                val dateKey = OfflineDevotionalCatalog.dateKeyOffset(0)
                val existing = devotionalRepo.getOrCreateTodayDevotional(slot, prefs.selectedLanguage, 0)
                val generated = aiRepo.generateDailyDevotional(dateKey, slot, prefs.selectedLanguage)
                    .copy(isBookmarked = existing.isBookmarked)
                devotionalRepo.upsert(generated)
                _snackbarMessage.value = "Updated today's ${slot.label.lowercase()} reflection."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _snackbarMessage.value = e.localizedMessage ?: "Unable to refresh reflection right now."
            } finally {
                _isRefreshingToday.value = false
            }
        }
    }

    fun askBolkarSamadhan(situationText: String) {
        val cleanInput = situationText.trim()
        if (cleanInput.isEmpty()) {
            _samadhanState.value = SamadhanUiState.Error("Please speak or type what is on your heart first.")
            return
        }
        viewModelScope.launch {
            _samadhanState.value = SamadhanUiState.Loading
            try {
                val lang = preferences.value.selectedLanguage
                val entity = aiRepo.askBibleForSituation(cleanInput, lang)
                devotionalRepo.upsert(entity)
                cloudRepo.syncSavedVerse(entity, entity.isBookmarked)
                _samadhanState.value = SamadhanUiState.Success(entity)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _samadhanState.value = SamadhanUiState.Error(
                    e.localizedMessage ?: "Could not connect to the AI companion. Please try again."
                )
            }
        }
    }

    fun openShareSheet(entity: DevotionalEntity) {
        _shareTarget.value = entity
    }

    fun dismissShareSheet() {
        _shareTarget.value = null
    }

    fun shareStatusCard(entity: DevotionalEntity, theme: StatusCardTheme, preferWhatsApp: Boolean) {
        viewModelScope.launch {
            try {
                val uri = StatusCardGenerator.generateStatusCardUri(appContext, entity, theme)
                StatusCardGenerator.shareStatusImage(appContext, uri, entity, preferWhatsApp)
                _shareTarget.value = null
            } catch (e: Exception) {
                _snackbarMessage.value = "Unable to generate status card: ${e.localizedMessage}"
            }
        }
    }

    fun toggleAmbientMusic(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.setAmbientMusicEnabled(enabled)
            if (playbackState.value.isPlaying) {
                VoicePlaybackService.refreshAmbientSettings(appContext)
            }
        }
    }

    fun setAmbientVolume(volume: Float) {
        viewModelScope.launch {
            prefsRepo.setAmbientMusicVolume(volume)
            if (playbackState.value.isPlaying) {
                VoicePlaybackService.refreshAmbientSettings(appContext)
            }
        }
    }

    fun setSpeechRate(rate: Float) {
        viewModelScope.launch {
            prefsRepo.setSpeechRate(rate)
        }
    }

    fun setMorningSchedule(enabled: Boolean, hour: Int, minute: Int) {
        viewModelScope.launch {
            prefsRepo.setMorningSchedule(enabled, hour, minute)
            val prefs = prefsRepo.getCurrent()
            cloudRepo.updateSyncPreferences(prefs.selectedLanguage, enabled, prefs.eveningEnabled)
        }
    }

    fun setEveningSchedule(enabled: Boolean, hour: Int, minute: Int) {
        viewModelScope.launch {
            prefsRepo.setEveningSchedule(enabled, hour, minute)
            val prefs = prefsRepo.getCurrent()
            cloudRepo.updateSyncPreferences(prefs.selectedLanguage, prefs.morningEnabled, enabled)
        }
    }

    fun setOneTapNotificationAudio(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.setOneTapNotificationAudio(enabled)
        }
    }

    fun setAutoDownloadOffline(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.setAutoDownloadOffline(enabled)
        }
    }

    fun sendTestVoiceNotification() {
        viewModelScope.launch {
            val prefs = preferences.value
            val entity = devotionalRepo.getOrCreateTodayDevotional(
                slot = _selectedSlot.value,
                language = prefs.selectedLanguage,
                daysOffset = 0
            )
            val posted = NotificationHelper.postDailyVoiceNotification(
                context = appContext,
                entity = entity,
                oneTapAudioEnabled = prefs.oneTapNotificationAudio
            )
            if (posted) {
                prefsRepo.markWelcomeNotificationSent()
                _snackbarMessage.value = "Notification sent! Pull down your notification bar and tap \"Play Voice\"."
            } else {
                _snackbarMessage.value = "Notification permission is not granted yet. Please allow notifications."
            }
        }
    }

    fun scheduleTenSecondTestNotification() {
        DailyAlarmScheduler.scheduleQuickReminderInSeconds(appContext, 10)
        _snackbarMessage.value = "Scheduled notification in 10 seconds! You can lock or minimize the app."
    }

    fun openDevotionalById(devotionalId: String) {
        viewModelScope.launch {
            val found = devotionalRepo.getById(devotionalId) ?: return@launch
            val slot = TimeSlot.fromKey(found.timeSlot)
            if (slot == TimeSlot.MORNING || slot == TimeSlot.EVENING) {
                _selectedSlot.value = slot
            }
        }
    }

    private fun refreshCacheStats() {
        _cacheStats.value = OfflineAudioCacheManager.getCacheStats(appContext)
    }
}
