package com.example.video

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

object VideoPlayerState {
    val isVideoPlaying = MutableStateFlow(false)
    val isPiPActive = MutableStateFlow(false)
    val pipPlayPauseToggle = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    fun notifyPlayStateChanged(playing: Boolean) {
        isVideoPlaying.value = playing
    }

    fun requestTogglePlayPause() {
        pipPlayPauseToggle.tryEmit(Unit)
    }
}
