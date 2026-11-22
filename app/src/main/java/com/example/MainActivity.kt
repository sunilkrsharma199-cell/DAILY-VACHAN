package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.ai.BolkarSamadhanRepository
import com.example.ai.configureAppCheck
import com.example.audio.VoicePlaybackService
import com.example.data.local.DailyVachanDatabase
import com.example.data.local.DevotionalRepository
import com.example.data.local.UserPreferencesRepository
import com.example.data.remote.CloudUserRepository
import com.example.notifications.NotificationHelper
import com.example.ui.DailyVachanApp
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.authStateFlow
import com.example.ui.theme.DailyVachanTheme
import com.example.ui.viewmodel.DailyVachanViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {

    private var pendingDevotionalId by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure App Check with launch intent before any UI, ViewModels, or Firebase operations
        configureAppCheck(this, intent)

        NotificationHelper.ensureChannels(this)
        enableEdgeToEdge()

        pendingDevotionalId = intent?.getStringExtra(VoicePlaybackService.EXTRA_DEVOTIONAL_ID)

        setContent {
            DailyVachanTheme {
                DailyVachanRoot(
                    pendingDevotionalId = pendingDevotionalId,
                    onConsumePendingDevotional = { pendingDevotionalId = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingDevotionalId = intent.getStringExtra(VoicePlaybackService.EXTRA_DEVOTIONAL_ID)
    }
}

@Composable
private fun DailyVachanRoot(
    pendingDevotionalId: String?,
    onConsumePendingDevotional: () -> Unit
) {
    val auth = Firebase.auth
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthScreen(
            onAuthSuccess = { }
        )
    } else {
        val uid = user.uid
        key(uid) {
            val viewModel: DailyVachanViewModel = viewModel(
                key = uid,
                factory = viewModelFactory {
                    initializer {
                        val app = checkNotNull(this[APPLICATION_KEY]) {
                            "APPLICATION_KEY missing from CreationExtras"
                        }
                        val appCtx = app.applicationContext
                        val databaseId = app.getString(R.string.firestore_database_id)
                        val firestore = FirebaseFirestore.getInstance(databaseId)
                        val dao = DailyVachanDatabase.getInstance(appCtx).devotionalDao()
                        DailyVachanViewModel(
                            appContext = appCtx,
                            currentUserId = uid,
                            devotionalRepo = DevotionalRepository(dao),
                            prefsRepo = UserPreferencesRepository(appCtx),
                            cloudRepo = CloudUserRepository(firestore, auth),
                            aiRepo = BolkarSamadhanRepository()
                        )
                    }
                }
            )

            LaunchedEffect(pendingDevotionalId) {
                val targetId = pendingDevotionalId
                if (!targetId.isNullOrBlank()) {
                    viewModel.openDevotionalById(targetId)
                    onConsumePendingDevotional()
                }
            }

            DailyVachanApp(
                viewModel = viewModel,
                userEmail = user.email.orEmpty(),
                userDisplayName = user.displayName?.takeIf { it.isNotBlank() } ?: "Devotee"
            )
        }
    }
}
