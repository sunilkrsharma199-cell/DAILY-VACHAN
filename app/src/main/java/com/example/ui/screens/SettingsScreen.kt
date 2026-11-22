package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.DevotionalLanguage
import com.example.data.model.UserPreferences
import com.example.data.remote.UserProfile
import com.example.notifications.NotificationHelper
import java.util.Locale

private enum class TimePickerTarget { MORNING, EVENING }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    preferences: UserPreferences,
    userProfile: UserProfile?,
    userEmail: String,
    userDisplayName: String,
    cacheStats: Pair<Int, Long>,
    isPreparingOffline: Boolean,
    onSelectLanguage: (DevotionalLanguage) -> Unit,
    onSaveMobileNumber: (String) -> Unit,
    onSignOut: () -> Unit,
    onMorningScheduleChange: (Boolean, Int, Int) -> Unit,
    onEveningScheduleChange: (Boolean, Int, Int) -> Unit,
    onOneTapToggle: (Boolean) -> Unit,
    onAmbientToggle: (Boolean) -> Unit,
    onAmbientVolumeChange: (Float) -> Unit,
    onSpeechRateChange: (Float) -> Unit,
    onAutoDownloadToggle: (Boolean) -> Unit,
    onPrecacheTomorrow: () -> Unit,
    onClearCache: () -> Unit,
    onSendTestNotification: () -> Unit,
    onScheduleTenSecondTestNotification: () -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var pickerTarget by remember { mutableStateOf<TimePickerTarget?>(null) }
    var phoneInput by remember { mutableStateOf(preferences.phoneNumber) }

    LaunchedEffect(userProfile?.phoneNumber, preferences.phoneNumber) {
        val cloudPhone = userProfile?.phoneNumber.orEmpty()
        if (cloudPhone.isNotBlank()) {
            phoneInput = cloudPhone
        } else if (phoneInput.isBlank() && preferences.phoneNumber.isNotBlank()) {
            phoneInput = preferences.phoneNumber
        }
    }

    var pendingScheduledTest by remember { mutableStateOf(false) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            if (pendingScheduledTest) {
                onScheduleTenSecondTestNotification()
            } else {
                onSendTestNotification()
            }
        } else {
            onShowMessage("Please allow notifications in Android Settings to receive daily morning/evening voice reminders.")
        }
        pendingScheduledTest = false
    }

    fun formatTime(hour: Int, minute: Int): String {
        val amPm = if (hour < 12) "AM" else "PM"
        val h12 = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        return String.format(Locale.US, "%d:%02d %s", h12, minute, amPm)
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 660.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Account & Voice Settings",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            // 1. Account & Mobile Profile Card
            SettingsSectionCard(title = "Google Account & Mobile Number") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = userProfile?.displayName?.ifBlank { userDisplayName } ?: userDisplayName,
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (userEmail.isNotBlank()) {
                            Text(
                                text = userEmail,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = onSignOut,
                        modifier = Modifier.testTag("settings_sign_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Sign Out")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = phoneInput,
                        onValueChange = { if (it.length <= 16) phoneInput = it },
                        label = { Text("Mobile Number") },
                        placeholder = { Text("+91 98765 43210") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("settings_phone_input")
                    )
                    Button(
                        onClick = { onSaveMobileNumber(phoneInput) },
                        modifier = Modifier
                            .height(54.dp)
                            .testTag("settings_save_phone_button")
                    ) {
                        Text("Save")
                    }
                }
            }

            // 2. Language Card
            SettingsSectionCard(title = "Devotional Language") {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DevotionalLanguage.entries.forEach { lang ->
                        FilterChip(
                            selected = preferences.selectedLanguage == lang,
                            onClick = { onSelectLanguage(lang) },
                            label = { Text("${lang.nativeLabel} (${lang.displayName})") },
                            modifier = Modifier.testTag("settings_lang_${lang.code}")
                        )
                    }
                }
            }

            // 3. Daily Voice Schedule Card
            SettingsSectionCard(title = "Daily Voice Notifications") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Morning Voice", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "Daily at ${formatTime(preferences.morningHour, preferences.morningMinute)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(
                            onClick = { pickerTarget = TimePickerTarget.MORNING },
                            modifier = Modifier.testTag("settings_morning_time_button")
                        ) {
                            Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Time")
                        }
                        Switch(
                            checked = preferences.morningEnabled,
                            onCheckedChange = {
                                onMorningScheduleChange(it, preferences.morningHour, preferences.morningMinute)
                            },
                            modifier = Modifier.testTag("settings_morning_switch")
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Evening Peace", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "Daily at ${formatTime(preferences.eveningHour, preferences.eveningMinute)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(
                            onClick = { pickerTarget = TimePickerTarget.EVENING },
                            modifier = Modifier.testTag("settings_evening_time_button")
                        ) {
                            Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Time")
                        }
                        Switch(
                            checked = preferences.eveningEnabled,
                            onCheckedChange = {
                                onEveningScheduleChange(it, preferences.eveningHour, preferences.eveningMinute)
                            },
                            modifier = Modifier.testTag("settings_evening_switch")
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("One-Tap Notification Play", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "Play voice directly from notification tray without opening app",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = preferences.oneTapNotificationAudio,
                        onCheckedChange = onOneTapToggle,
                        modifier = Modifier.testTag("settings_onetap_switch")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            if (NotificationHelper.hasNotificationPermission(context)) {
                                onSendTestNotification()
                            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                pendingScheduledTest = false
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                onSendTestNotification()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("settings_test_notification_button")
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Notify Now")
                    }

                    OutlinedButton(
                        onClick = {
                            if (NotificationHelper.hasNotificationPermission(context)) {
                                onScheduleTenSecondTestNotification()
                            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                pendingScheduledTest = true
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                onScheduleTenSecondTestNotification()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("settings_test_scheduled_button")
                    ) {
                        Icon(Icons.Default.Alarm, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("In 10 Sec")
                    }
                }
            }

            // 4. Voice & Background Music Card
            SettingsSectionCard(title = "Audio & Calm Background") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Soft Ambient Pad Music", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "Gentle warm background harmony behind spoken verse",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = preferences.ambientMusicEnabled,
                        onCheckedChange = onAmbientToggle,
                        modifier = Modifier.testTag("settings_ambient_switch")
                    )
                }

                if (preferences.ambientMusicEnabled) {
                    Column {
                        Text(
                            text = "Background Music Volume (${(preferences.ambientMusicVolume * 100).toInt()}%)",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Slider(
                            value = preferences.ambientMusicVolume,
                            onValueChange = onAmbientVolumeChange,
                            valueRange = 0.05f..0.75f,
                            modifier = Modifier.testTag("settings_ambient_slider")
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Voice Pace", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            0.85f to "Calm 0.85x",
                            0.95f to "Natural 0.95x",
                            1.10f to "Clear 1.1x"
                        ).forEach { (rate, label) ->
                            FilterChip(
                                selected = kotlin.math.abs(preferences.speechRate - rate) < 0.03f,
                                onClick = { onSpeechRateChange(rate) },
                                label = { Text(label) },
                                modifier = Modifier.testTag("settings_pace_${label.replace(" ", "_")}")
                            )
                        }
                    }
                }
            }

            // 5. Lite Offline-First Storage Card
            SettingsSectionCard(title = "Lite & Offline Auto-Cache") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Auto-Prepare Next Day at Night", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "Pre-renders tomorrow's audio at 11:15 PM so morning playback needs zero data",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = preferences.autoDownloadOffline,
                        onCheckedChange = onAutoDownloadToggle,
                        modifier = Modifier.testTag("settings_autodownload_switch")
                    )
                }

                val sizeKb = (cacheStats.second / 1024L).coerceAtLeast(0L)
                Text(
                    text = "Offline Audio Cache: ${cacheStats.first} clips (${sizeKb} KB)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onPrecacheTomorrow,
                        enabled = !isPreparingOffline,
                        modifier = Modifier
                            .weight(1.4f)
                            .height(48.dp)
                            .testTag("settings_precache_tomorrow_button")
                    ) {
                        if (isPreparingOffline) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Cache Tomorrow")
                    }

                    OutlinedButton(
                        onClick = onClearCache,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("settings_clear_cache_button")
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Clear")
                    }
                }
            }

            Spacer(modifier = Modifier.height(72.dp))
        }
    }

    pickerTarget?.let { target ->
        val initialHour = if (target == TimePickerTarget.MORNING) preferences.morningHour else preferences.eveningHour
        val initialMinute = if (target == TimePickerTarget.MORNING) preferences.morningMinute else preferences.eveningMinute
        val timePickerState = rememberTimePickerState(
            initialHour = initialHour,
            initialMinute = initialMinute,
            is24Hour = false
        )

        AlertDialog(
            onDismissRequest = { pickerTarget = null },
            title = {
                Text(
                    if (target == TimePickerTarget.MORNING) {
                        "Set Morning Voice Time"
                    } else {
                        "Set Evening Peace Time"
                    }
                )
            },
            text = {
                TimePicker(state = timePickerState)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (target == TimePickerTarget.MORNING) {
                            onMorningScheduleChange(
                                preferences.morningEnabled,
                                timePickerState.hour,
                                timePickerState.minute
                            )
                        } else {
                            onEveningScheduleChange(
                                preferences.eveningEnabled,
                                timePickerState.hour,
                                timePickerState.minute
                            )
                        }
                        pickerTarget = null
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { pickerTarget = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            content()
        }
    }
}
