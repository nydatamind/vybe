package com.example.vybe.playback

import android.content.Context
import com.example.vybe.core.model.Track
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job

data class IntroOutroCue(
    val introMs: Long,
    val outroMs: Long
)

/**
 * Audio cue manager providing fixed 7-second intro skip and 7-second outro skip.
 */
class AudioAnalysisManager(private val context: Context) {

    companion object {
        const val FIXED_INTRO_MS = 7000L
        const val FIXED_OUTRO_MS = 7000L
        const val MIN_DURATION_FOR_SKIP_MS = 14000L
    }

    fun getCueForDuration(durationMs: Long): IntroOutroCue {
        return if (durationMs == 0L || durationMs > MIN_DURATION_FOR_SKIP_MS) {
            IntroOutroCue(FIXED_INTRO_MS, FIXED_OUTRO_MS)
        } else {
            IntroOutroCue(0L, 0L)
        }
    }

    fun getCachedCue(trackId: String): IntroOutroCue {
        return IntroOutroCue(FIXED_INTRO_MS, FIXED_OUTRO_MS)
    }

    fun analyzeTrack(
        track: Track,
        onComplete: ((IntroOutroCue) -> Unit)? = null
    ): Job {
        val cue = getCueForDuration(track.durationMs)
        onComplete?.invoke(cue)
        return CompletableDeferred<Unit>().apply { complete(Unit) }
    }
}
