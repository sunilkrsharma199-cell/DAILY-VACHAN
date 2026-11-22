package com.example.audio

import com.example.data.local.DevotionalEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PlaybackUiState(
    val activeDevotional: DevotionalEntity? = null,
    val isPlaying: Boolean = false,
    val isPaused: Boolean = false,
    val isPreparing: Boolean = false,
    val progressFraction: Float = 0f,
    val elapsedMs: Long = 0L,
    val durationMs: Long = 0L,
    val isOfflineFilePlayback: Boolean = false,
    val errorMessage: String? = null
)

object PlaybackStateHolder {
    private val _state = MutableStateFlow(PlaybackUiState())
    val state: StateFlow<PlaybackUiState> = _state.asStateFlow()

    fun update(transform: (PlaybackUiState) -> PlaybackUiState) {
        _state.value = transform(_state.value)
    }

    fun reset() {
        _state.value = PlaybackUiState()
    }
}
