package com.example.vybe.playback

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SleepTimerManager(
    private val coroutineScope: CoroutineScope,
    private val onTimerFinished: () -> Unit,
    private val onFadeVolume: (Float) -> Unit = {}
) {
    private val _remainingSeconds = MutableStateFlow(0L)
    val remainingSeconds: StateFlow<Long> = _remainingSeconds.asStateFlow()

    private var timerJob: Job? = null
    var isEndOfTrackMode: Boolean = false
        private set

    fun startTimer(minutes: Int) {
        cancelTimer()
        isEndOfTrackMode = false
        if (minutes <= 0) return

        val totalSeconds = minutes * 60L
        _remainingSeconds.value = totalSeconds

        timerJob = coroutineScope.launch(Dispatchers.Default) {
            var current = totalSeconds
            while (isActive && current > 0) {
                delay(1000)
                current--
                _remainingSeconds.value = current

                // Smoothly fade out volume in the last 15 seconds
                if (current in 1..15) {
                    val volumeFactor = current / 15f
                    withContext(Dispatchers.Main) {
                        onFadeVolume(volumeFactor)
                    }
                }
            }
            if (isActive) {
                _remainingSeconds.value = 0L
                withContext(Dispatchers.Main) {
                    onFadeVolume(1.0f)
                    onTimerFinished()
                }
            }
        }
    }

    fun setEndOfTrackMode(enabled: Boolean) {
        cancelTimer()
        isEndOfTrackMode = enabled
    }

    fun onTrackEnded() {
        if (isEndOfTrackMode) {
            isEndOfTrackMode = false
            coroutineScope.launch(Dispatchers.Main) {
                onTimerFinished()
            }
        }
    }

    fun cancelTimer() {
        timerJob?.cancel()
        timerJob = null
        isEndOfTrackMode = false
        _remainingSeconds.value = 0L
        coroutineScope.launch(Dispatchers.Main) {
            onFadeVolume(1.0f)
        }
    }
}
