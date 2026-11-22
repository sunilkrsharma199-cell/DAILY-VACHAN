package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.DevotionalLanguage
import com.example.data.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "daily_vachan_prefs")

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val LANGUAGE = stringPreferencesKey("selected_language")
        val MORNING_ENABLED = booleanPreferencesKey("morning_enabled")
        val MORNING_HOUR = intPreferencesKey("morning_hour")
        val MORNING_MINUTE = intPreferencesKey("morning_minute")
        val EVENING_ENABLED = booleanPreferencesKey("evening_enabled")
        val EVENING_HOUR = intPreferencesKey("evening_hour")
        val EVENING_MINUTE = intPreferencesKey("evening_minute")
        val ONE_TAP_AUDIO = booleanPreferencesKey("one_tap_notification_audio")
        val AMBIENT_MUSIC = booleanPreferencesKey("ambient_music_enabled")
        val AMBIENT_VOLUME = floatPreferencesKey("ambient_music_volume")
        val AUTO_DOWNLOAD_OFFLINE = booleanPreferencesKey("auto_download_offline")
        val SPEECH_RATE = floatPreferencesKey("speech_rate")
        val PHONE_NUMBER = stringPreferencesKey("phone_number")
        val WELCOME_NOTIF_SENT = booleanPreferencesKey("welcome_notification_sent")
    }

    val preferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        UserPreferences(
            selectedLanguage = DevotionalLanguage.fromCode(prefs[Keys.LANGUAGE]),
            morningEnabled = prefs[Keys.MORNING_ENABLED] ?: true,
            morningHour = prefs[Keys.MORNING_HOUR] ?: 6,
            morningMinute = prefs[Keys.MORNING_MINUTE] ?: 30,
            eveningEnabled = prefs[Keys.EVENING_ENABLED] ?: true,
            eveningHour = prefs[Keys.EVENING_HOUR] ?: 20,
            eveningMinute = prefs[Keys.EVENING_MINUTE] ?: 30,
            oneTapNotificationAudio = prefs[Keys.ONE_TAP_AUDIO] ?: true,
            ambientMusicEnabled = prefs[Keys.AMBIENT_MUSIC] ?: true,
            ambientMusicVolume = prefs[Keys.AMBIENT_VOLUME] ?: 0.28f,
            autoDownloadOffline = prefs[Keys.AUTO_DOWNLOAD_OFFLINE] ?: true,
            speechRate = prefs[Keys.SPEECH_RATE] ?: 0.95f,
            phoneNumber = prefs[Keys.PHONE_NUMBER] ?: "",
            welcomeNotificationSent = prefs[Keys.WELCOME_NOTIF_SENT] ?: false
        )
    }

    suspend fun getCurrent(): UserPreferences = preferencesFlow.first()

    suspend fun setLanguage(language: DevotionalLanguage) {
        context.dataStore.edit { it[Keys.LANGUAGE] = language.code }
    }

    suspend fun setMorningSchedule(enabled: Boolean, hour: Int, minute: Int) {
        context.dataStore.edit {
            it[Keys.MORNING_ENABLED] = enabled
            it[Keys.MORNING_HOUR] = hour
            it[Keys.MORNING_MINUTE] = minute
        }
    }

    suspend fun setEveningSchedule(enabled: Boolean, hour: Int, minute: Int) {
        context.dataStore.edit {
            it[Keys.EVENING_ENABLED] = enabled
            it[Keys.EVENING_HOUR] = hour
            it[Keys.EVENING_MINUTE] = minute
        }
    }

    suspend fun setOneTapNotificationAudio(enabled: Boolean) {
        context.dataStore.edit { it[Keys.ONE_TAP_AUDIO] = enabled }
    }

    suspend fun setAmbientMusicEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AMBIENT_MUSIC] = enabled }
    }

    suspend fun setAmbientMusicVolume(volume: Float) {
        context.dataStore.edit { it[Keys.AMBIENT_VOLUME] = volume.coerceIn(0f, 1f) }
    }

    suspend fun setSpeechRate(rate: Float) {
        context.dataStore.edit { it[Keys.SPEECH_RATE] = rate.coerceIn(0.75f, 1.25f) }
    }

    suspend fun setAutoDownloadOffline(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_DOWNLOAD_OFFLINE] = enabled }
    }

    suspend fun setPendingPhoneNumber(phone: String) {
        context.dataStore.edit { it[Keys.PHONE_NUMBER] = phone.take(20) }
    }

    suspend fun markWelcomeNotificationSent() {
        context.dataStore.edit { it[Keys.WELCOME_NOTIF_SENT] = true }
    }
}
