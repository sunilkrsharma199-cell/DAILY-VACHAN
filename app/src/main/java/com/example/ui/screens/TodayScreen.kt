package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DownloadForOffline
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.audio.PlaybackUiState
import com.example.data.local.DevotionalEntity
import com.example.data.model.DevotionalLanguage
import com.example.data.model.TimeSlot
import com.example.data.model.UserPreferences
import com.example.notifications.NotificationHelper
import com.example.ui.theme.PlayfairDisplayFontFamily

@Composable
fun TodayScreen(
    preferences: UserPreferences,
    selectedSlot: TimeSlot,
    todayDevotional: DevotionalEntity?,
    playbackState: PlaybackUiState,
    isRefreshingToday: Boolean,
    isPreparingOffline: Boolean,
    onSelectSlot: (TimeSlot) -> Unit,
    onSelectLanguage: (DevotionalLanguage) -> Unit,
    onPlayPause: (DevotionalEntity) -> Unit,
    onToggleBookmark: (DevotionalEntity) -> Unit,
    onPrepareOffline: (DevotionalEntity) -> Unit,
    onRefreshToday: () -> Unit,
    onShareStatus: (DevotionalEntity) -> Unit,
    onOpenSamadhan: () -> Unit,
    onToggleAmbient: (Boolean) -> Unit,
    onAmbientVolumeChange: (Float) -> Unit,
    onNotificationPermissionGranted: () -> Unit,
    onSendNotificationNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasNotifPermission by remember {
        mutableStateOf(NotificationHelper.hasNotificationPermission(context))
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotifPermission = granted
        if (granted) {
            onNotificationPermissionGranted()
        }
    }

    LaunchedEffect(Unit) {
        if (hasNotifPermission) {
            onNotificationPermissionGranted()
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val isThisPlaying = todayDevotional != null &&
        playbackState.activeDevotional?.id == todayDevotional.id &&
        playbackState.isPlaying
    val isThisActive = todayDevotional != null &&
        playbackState.activeDevotional?.id == todayDevotional.id &&
        (playbackState.isPlaying || playbackState.isPaused || playbackState.isPreparing)

    val greeting = when (selectedSlot) {
        TimeSlot.MORNING -> when (preferences.selectedLanguage) {
            DevotionalLanguage.HINDI, DevotionalLanguage.MARATHI -> "शुभ प्रभात"
            DevotionalLanguage.HINGLISH -> "Shubh Prabhat"
            else -> "Peaceful Morning"
        }
        else -> when (preferences.selectedLanguage) {
            DevotionalLanguage.HINDI, DevotionalLanguage.MARATHI -> "शुभ संध्या"
            DevotionalLanguage.HINGLISH -> "Shubh Sandhya"
            else -> "Calm Evening"
        }
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
            // Greeting & Today Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = greeting,
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Your daily word & calm voice sanctuary",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            if (hasNotifPermission) {
                                onSendNotificationNow()
                            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                onSendNotificationNow()
                            }
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("today_send_notification_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "Send voice notification now",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                    if (todayDevotional != null) {
                        IconButton(
                            onClick = onRefreshToday,
                            enabled = !isRefreshingToday,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("refresh_today_button")
                        ) {
                            if (isRefreshingToday) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Generate fresh reflection",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(visible = !hasNotifPermission) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Turn On Daily Voice Notifications",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                text = "Allow notifications to receive calm Morning & Evening verses.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f)
                            )
                        }
                        Button(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            },
                            modifier = Modifier.testTag("allow_notifications_banner_button")
                        ) {
                            Text("Allow")
                        }
                    }
                }
            }

            // Quick Language Selector Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DevotionalLanguage.entries.forEach { lang ->
                    val selected = lang == preferences.selectedLanguage
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectLanguage(lang) },
                        label = {
                            Text(
                                text = lang.nativeLabel,
                                style = MaterialTheme.typography.labelLarge
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.testTag("lang_chip_${lang.code}")
                    )
                }
            }

            // Morning / Evening Segmented Toggle
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf(TimeSlot.MORNING, TimeSlot.EVENING).forEachIndexed { index, slot ->
                    SegmentedButton(
                        selected = selectedSlot == slot,
                        onClick = { onSelectSlot(slot) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = 2),
                        icon = {
                            Icon(
                                imageVector = if (slot == TimeSlot.MORNING) {
                                    Icons.Default.WbSunny
                                } else {
                                    Icons.Default.NightsStay
                                },
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        modifier = Modifier.testTag("slot_toggle_${slot.key.lowercase()}")
                    ) {
                        Text(if (slot == TimeSlot.MORNING) "Morning Voice" else "Evening Peace")
                    }
                }
            }

            if (todayDevotional == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                // Hero Verse Card with Generated Atmospheric Artwork
                ElevatedCard(
                    shape = RoundedCornerShape(26.dp),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Image(
                            painter = painterResource(
                                id = if (selectedSlot == TimeSlot.MORNING) {
                                    R.drawable.img_dawn_sunrise
                                } else {
                                    R.drawable.img_evening_lantern
                                }
                            ),
                            contentDescription = "Devotional atmosphere artwork",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .matchParentSize()
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0x991C130D),
                                            Color(0xCC1B120C),
                                            Color(0xEE170F0A)
                                        )
                                    )
                                )
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(22.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = Color(0x3DF2C97D)
                                ) {
                                    Text(
                                        text = todayDevotional.moodTag,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color(0xFFF6D79A),
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (todayDevotional.audioCached) {
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = Color(0x3D9BB8A2)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CloudDone,
                                                    contentDescription = "Offline audio ready",
                                                    tint = Color(0xFFC8E3CE),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "Offline Ready",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color(0xFFC8E3CE)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    IconButton(
                                        onClick = { onToggleBookmark(todayDevotional) },
                                        modifier = Modifier.testTag("today_bookmark_button")
                                    ) {
                                        Icon(
                                            imageVector = if (todayDevotional.isBookmarked) {
                                                Icons.Default.Bookmark
                                            } else {
                                                Icons.Outlined.BookmarkBorder
                                            },
                                            contentDescription = if (todayDevotional.isBookmarked) {
                                                "Remove bookmark"
                                            } else {
                                                "Bookmark verse"
                                            },
                                            tint = Color(0xFFF6D79A)
                                        )
                                    }
                                }
                            }

                            Text(
                                text = "“${todayDevotional.verseText}”",
                                fontFamily = PlayfairDisplayFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.headlineSmall,
                                color = Color(0xFFFFF8EE)
                            )

                            Text(
                                text = "— ${todayDevotional.verseReference}",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color(0xFFF2C97D)
                            )
                        }
                    }
                }

                // Voice Player Card
                VoicePlayerCard(
                    entity = todayDevotional,
                    isPlaying = isThisPlaying,
                    isActive = isThisActive,
                    isPreparing = playbackState.isPreparing && isThisActive,
                    progressFraction = if (isThisActive) playbackState.progressFraction else 0f,
                    ambientEnabled = preferences.ambientMusicEnabled,
                    ambientVolume = preferences.ambientMusicVolume,
                    isPreparingOffline = isPreparingOffline,
                    onPlayPause = { onPlayPause(todayDevotional) },
                    onPrepareOffline = { onPrepareOffline(todayDevotional) },
                    onToggleAmbient = onToggleAmbient,
                    onAmbientVolumeChange = onAmbientVolumeChange
                )

                // Reflection & Closing Prayer Card
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Today's Reflection",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = todayDevotional.reflection,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Short Prayer",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = todayDevotional.shortPrayer,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Quick Actions: Share WhatsApp Status & Bolkar Samadhan
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilledTonalButton(
                        onClick = { onShareStatus(todayDevotional) },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("today_share_status_button"),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Status Card", maxLines = 1)
                    }

                    Button(
                        onClick = onOpenSamadhan,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("today_open_samadhan_button"),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ask Bible", maxLines = 1)
                    }
                }

                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }
}

@Composable
private fun VoicePlayerCard(
    entity: DevotionalEntity,
    isPlaying: Boolean,
    isActive: Boolean,
    isPreparing: Boolean,
    progressFraction: Float,
    ambientEnabled: Boolean,
    ambientVolume: Float,
    isPreparingOffline: Boolean,
    onPlayPause: () -> Unit,
    onPrepareOffline: () -> Unit,
    onToggleAmbient: (Boolean) -> Unit,
    onAmbientVolumeChange: (Float) -> Unit
) {
    val pulseTransition = rememberInfiniteTransition(label = "voicePulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isPlaying) 1.09f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(62.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable(onClick = onPlayPause)
                        .testTag("today_play_voice_button")
                ) {
                    if (isPreparing) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause daily voice" else "Play daily voice",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isPlaying) "Speaking Calmly…" else "Listen to Today's Voice",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (entity.audioCached) {
                            "Cached offline • ~1 min gentle voice + prayer"
                        } else {
                            "Tap Play for instant devotional voice + prayer"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }

            if (isActive) {
                LinearProgressIndicator(
                    progress = { progressFraction.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.14f)
                )
            }

            // Soft Ambient Music Control + Offline Cache Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Soft Pad",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = ambientEnabled,
                        onCheckedChange = onToggleAmbient,
                        modifier = Modifier.testTag("today_ambient_switch")
                    )
                }

                if (!entity.audioCached) {
                    OutlinedButton(
                        onClick = onPrepareOffline,
                        enabled = !isPreparingOffline,
                        modifier = Modifier.testTag("today_cache_offline_button")
                    ) {
                        if (isPreparingOffline) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.DownloadForOffline,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Offline")
                    }
                } else {
                    AssistChip(
                        onClick = {},
                        label = { Text("Saved Offline") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
            }

            AnimatedVisibility(visible = ambientEnabled) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Music Vol",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Slider(
                        value = ambientVolume,
                        onValueChange = onAmbientVolumeChange,
                        valueRange = 0.05f..0.75f,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("today_ambient_volume_slider")
                    )
                }
            }
        }
    }
}
