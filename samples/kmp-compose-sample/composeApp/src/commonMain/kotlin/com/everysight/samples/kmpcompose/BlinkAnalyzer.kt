/*
 * Created by Everysight LTD.
 *
 * A second analyzer, to show two running at once.
 *
 * Blinks are a good second example because they need something the gaze analyzer did not:
 * state that survives across frames (a closure starts on one frame and ends on another) and
 * the SDK's own baseline, which learns what this wearer's eye looks like at rest.
 *
 * One honest caveat, worth knowing before you build on it: the eye model does not truly
 * detect eyelids. The aperture is derived from other outputs. The dependable closed/open cue
 * is **pupil visibility** — a shut eye shows no pupil — so that is what this analyzer leads
 * with, and the aperture only refines it.
 */

package com.everysight.samples.kmpcompose

import com.everysight.mav2.sdk.services.eyetracker.analyzers.M2AnalysisConclusion
import com.everysight.mav2.sdk.services.eyetracker.analyzers.M2AnalyzerInput
import com.everysight.mav2.sdk.services.eyetracker.analyzers.M2AnalyzerKey
import com.everysight.mav2.sdk.services.eyetracker.analyzers.M2EyeFeatureAnalyzer
import com.everysight.mav2.sdk.services.eyetracker.analyzers.M2EyeFeatureResult

/** Whether the eye is open or shut this frame. */
enum class M2EyeState { Open, Closed }

/**
 * One frame's eyelid answer.
 *
 * [blinkDetected] is true on the single frame a closure completes — that is the edge to act
 * on. [blinkRatePerMin] is the running rate, which is the number most apps actually want.
 */
class BlinkResult(
    override val timestamp: Long,
    override val isValid: Boolean,
    override val confidence: Float,
    val eyeState: M2EyeState,
    val blinkDetected: Boolean,
    val blinkDurationMs: Long,
    val blinkCount: Int,
    val blinkRatePerMin: Float,
) : M2EyeFeatureResult() {

    override val conclusion: M2AnalysisConclusion
        get() = if (!isValid) M2AnalysisConclusion.WarmingUp
        else M2AnalysisConclusion(
            label = if (eyeState == M2EyeState.Closed) "Closed" else "Open",
            detail = "$blinkCount blinks · ${blinkRatePerMin.toInt()}/min",
        )
}

/**
 * Counts blinks, and reports the rate.
 *
 * A blink is a closure that **ends**. A closure that goes on longer than [MAX_BLINK_MS] is
 * someone shutting their eyes, not blinking, and is deliberately not counted — otherwise the
 * rate climbs while the wearer rests.
 *
 * The open and closed thresholds differ on purpose. With one threshold, a signal sitting near
 * it flickers and every flicker is a false blink; with two, the eye must travel a real
 * distance to change state.
 */
class BlinkAnalyzer : M2EyeFeatureAnalyzer<BlinkResult>(KEY) {

    companion object {
        /** Identifies this analyzer to the service. */
        val KEY = M2AnalyzerKey<BlinkResult>("sample.blink")

        /** Pupil visibility below this means the pupil is gone — the eye is shut. */
        private const val CLOSED_VISIBILITY = 0.2f

        /** And above this it is unambiguously open again. The gap is the hysteresis. */
        private const val OPEN_VISIBILITY = 0.5f

        /** Aperture below this fraction of the wearer's resting aperture also counts as shut. */
        private const val CLOSED_APERTURE_FRACTION = 0.35f

        /** Longer than this is a held closure, not a blink. */
        private const val MAX_BLINK_MS = 500L

        /** The window the rate is measured over. */
        private const val RATE_WINDOW_MS = 60_000L
    }

    override val description = "Counts blinks and reports the rate per minute."

    private var state = M2EyeState.Open
    private var closedAt = 0L
    private var blinkCount = 0
    private var lastBlinkDurationMs = 0L

    /** Timestamps of recent blinks, trimmed to the rate window. */
    private val recentBlinks = ArrayDeque<Long>()

    override fun onFrame(input: M2AnalyzerInput) {
        val visibility = input.pupilVisibility.coerceIn(0f, 1f)
        val baseline = input.baselineStats

        // The aperture is only meaningful once the SDK has learned this wearer's resting value.
        val apertureClosed = baseline?.takeIf { it.isReady }?.let { stats ->
            val resting = stats.restingApertureNorm
            resting > 0f && input.normalized.eyelidApertureNorm < resting * CLOSED_APERTURE_FRACTION
        } ?: false

        // Visibility leads; the aperture can only add a closure, never veto one.
        val looksClosed = visibility < CLOSED_VISIBILITY || apertureClosed
        val looksOpen = visibility > OPEN_VISIBILITY && !apertureClosed

        var blinkDetected = false

        when (state) {
            M2EyeState.Open -> if (looksClosed) {
                state = M2EyeState.Closed
                closedAt = input.timestamp
            }

            M2EyeState.Closed -> if (looksOpen) {
                state = M2EyeState.Open
                val duration = input.timestamp - closedAt
                // Only a closure that ended quickly is a blink.
                if (duration in 1..MAX_BLINK_MS) {
                    blinkDetected = true
                    blinkCount++
                    lastBlinkDurationMs = duration
                    recentBlinks.addLast(input.timestamp)
                }
            }
        }

        // Drop anything that has aged out of the rate window.
        while (recentBlinks.isNotEmpty() && input.timestamp - recentBlinks.first() > RATE_WINDOW_MS) {
            recentBlinks.removeFirst()
        }

        emit(
            BlinkResult(
                timestamp = input.timestamp,
                // A closed eye is a valid answer, so this reports validity on having a signal
                // at all rather than on the geometry being present.
                isValid = true,
                confidence = visibility,
                eyeState = state,
                blinkDetected = blinkDetected,
                blinkDurationMs = lastBlinkDurationMs,
                blinkCount = blinkCount,
                blinkRatePerMin = recentBlinks.size.toFloat(),
            )
        )
    }

    override fun onReset() {
        state = M2EyeState.Open
        closedAt = 0L
        blinkCount = 0
        lastBlinkDurationMs = 0L
        recentBlinks.clear()
    }
}
