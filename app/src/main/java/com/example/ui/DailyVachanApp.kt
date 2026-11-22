package com.example.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.MicNone
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.ui.auth.signOutWithCleanup
import com.example.ui.components.MiniPlayerBar
import com.example.ui.components.StatusShareSheet
import com.example.ui.screens.HistorySavedScreen
import com.example.ui.screens.SamadhanScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TodayScreen
import com.example.ui.viewmodel.DailyVachanViewModel

const val ROUTE_TODAY = "today"
const val ROUTE_SAMADHAN = "samadhan"
const val ROUTE_SAVED = "saved"
const val ROUTE_SETTINGS = "settings"

private data class NavDestinationItem(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val navItems = listOf(
    NavDestinationItem(
        route = ROUTE_TODAY,
        labelRes = R.string.nav_today,
        selectedIcon = Icons.Filled.WbSunny,
        unselectedIcon = Icons.Outlined.WbSunny
    ),
    NavDestinationItem(
        route = ROUTE_SAMADHAN,
        labelRes = R.string.nav_samadhan,
        selectedIcon = Icons.Filled.Mic,
        unselectedIcon = Icons.Outlined.MicNone
    ),
    NavDestinationItem(
        route = ROUTE_SAVED,
        labelRes = R.string.nav_saved,
        selectedIcon = Icons.Filled.Bookmarks,
        unselectedIcon = Icons.Outlined.Bookmarks
    ),
    NavDestinationItem(
        route = ROUTE_SETTINGS,
        labelRes = R.string.nav_settings,
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    )
)

@Composable
fun DailyVachanApp(
    viewModel: DailyVachanViewModel,
    userEmail: String,
    userDisplayName: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ROUTE_TODAY

    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val selectedSlot by viewModel.selectedSlot.collectAsStateWithLifecycle()
    val todayDevotional by viewModel.todayDevotional.collectAsStateWithLifecycle()
    val allHistory by viewModel.allHistory.collectAsStateWithLifecycle()
    val bookmarkedHistory by viewModel.bookmarkedHistory.collectAsStateWithLifecycle()
    val samadhanHistory by viewModel.samadhanHistory.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val samadhanState by viewModel.samadhanState.collectAsStateWithLifecycle()
    val isRefreshingToday by viewModel.isRefreshingToday.collectAsStateWithLifecycle()
    val isPreparingOffline by viewModel.isPreparingOffline.collectAsStateWithLifecycle()
    val cacheStats by viewModel.cacheStats.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val shareTarget by viewModel.shareTarget.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        val msg = snackbarMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    fun navigateTo(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                if (!useRail) {
                    Column {
                        MiniPlayerBar(
                            playbackState = playbackState,
                            onTogglePlayPause = viewModel::playOrPause,
                            onStop = viewModel::stopPlayback
                        )
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            navItems.forEach { item ->
                                val selected = currentRoute == item.route
                                val label = stringResource(item.labelRes)
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateTo(item.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                            contentDescription = label
                                        )
                                    },
                                    label = { Text(label) },
                                    modifier = Modifier.testTag("nav_${item.route}")
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        navItems.forEach { item ->
                            val selected = currentRoute == item.route
                            val label = stringResource(item.labelRes)
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTo(item.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label) },
                                modifier = Modifier.testTag("rail_${item.route}")
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    NavHost(
                        navController = navController,
                        startDestination = ROUTE_TODAY,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        composable(ROUTE_TODAY) {
                            TodayScreen(
                                preferences = preferences,
                                selectedSlot = selectedSlot,
                                todayDevotional = todayDevotional,
                                playbackState = playbackState,
                                isRefreshingToday = isRefreshingToday,
                                isPreparingOffline = isPreparingOffline,
                                onSelectSlot = viewModel::selectSlot,
                                onSelectLanguage = viewModel::selectLanguage,
                                onPlayPause = viewModel::playOrPause,
                                onToggleBookmark = viewModel::toggleBookmark,
                                onPrepareOffline = viewModel::prepareOfflineAudio,
                                onRefreshToday = viewModel::generateFreshTodayReflection,
                                onShareStatus = viewModel::openShareSheet,
                                onOpenSamadhan = { navigateTo(ROUTE_SAMADHAN) },
                                onToggleAmbient = viewModel::toggleAmbientMusic,
                                onAmbientVolumeChange = viewModel::setAmbientVolume,
                                onNotificationPermissionGranted = viewModel::onNotificationPermissionGranted,
                                onSendNotificationNow = viewModel::sendTestVoiceNotification
                            )
                        }
                        composable(ROUTE_SAMADHAN) {
                            SamadhanScreen(
                                language = preferences.selectedLanguage,
                                samadhanState = samadhanState,
                                playbackState = playbackState,
                                onAskSamadhan = viewModel::askBolkarSamadhan,
                                onPlayPause = viewModel::playOrPause,
                                onToggleBookmark = viewModel::toggleBookmark,
                                onShareStatus = viewModel::openShareSheet
                            )
                        }
                        composable(ROUTE_SAVED) {
                            HistorySavedScreen(
                                allHistory = allHistory,
                                bookmarkedHistory = bookmarkedHistory,
                                samadhanHistory = samadhanHistory,
                                playbackState = playbackState,
                                onPlayPause = viewModel::playOrPause,
                                onToggleBookmark = viewModel::toggleBookmark,
                                onShareStatus = viewModel::openShareSheet
                            )
                        }
                        composable(ROUTE_SETTINGS) {
                            SettingsScreen(
                                preferences = preferences,
                                userProfile = userProfile,
                                userEmail = userEmail,
                                userDisplayName = userDisplayName,
                                cacheStats = cacheStats,
                                isPreparingOffline = isPreparingOffline,
                                onSelectLanguage = viewModel::selectLanguage,
                                onSaveMobileNumber = viewModel::saveMobileNumber,
                                onSignOut = {
                                    viewModel.stopPlayback()
                                    signOutWithCleanup(credentialManager, scope)
                                },
                                onMorningScheduleChange = viewModel::setMorningSchedule,
                                onEveningScheduleChange = viewModel::setEveningSchedule,
                                onOneTapToggle = viewModel::setOneTapNotificationAudio,
                                onAmbientToggle = viewModel::toggleAmbientMusic,
                                onAmbientVolumeChange = viewModel::setAmbientVolume,
                                onSpeechRateChange = viewModel::setSpeechRate,
                                onAutoDownloadToggle = viewModel::setAutoDownloadOffline,
                                onPrecacheTomorrow = viewModel::precacheTomorrowAudio,
                                onClearCache = viewModel::clearOfflineAudioCache,
                                onSendTestNotification = viewModel::sendTestVoiceNotification,
                                onScheduleTenSecondTestNotification = viewModel::scheduleTenSecondTestNotification,
                                onShowMessage = viewModel::showMessage
                            )
                        }
                    }

                    if (useRail) {
                        MiniPlayerBar(
                            playbackState = playbackState,
                            onTogglePlayPause = viewModel::playOrPause,
                            onStop = viewModel::stopPlayback,
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                    }
                }
            }
        }
    }

    shareTarget?.let { target ->
        StatusShareSheet(
            entity = target,
            onDismiss = viewModel::dismissShareSheet,
            onShare = { theme, preferWhatsApp ->
                viewModel.shareStatusCard(target, theme, preferWhatsApp)
            }
        )
    }
}
