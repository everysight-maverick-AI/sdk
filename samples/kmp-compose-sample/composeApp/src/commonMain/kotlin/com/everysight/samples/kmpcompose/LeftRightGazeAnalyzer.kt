/*
 * Created by Everysight LTD.
 *
 * Writing an eye-feature analyzer.
 *
 * The SDK does the hard part — it runs the eye camera, finds the pupil and the glint, and
 * hands you a normalized frame. An analyzer is the small piece on top that turns those
 * numbers into something your app can act on. You write it, you own it, and it runs on the
 * SDK's eye thread frame by frame.
 *
 * This one answers the simplest useful question: is the wearer looking left, right, or
 * straight ahead? No calibration, no per-user setup.
 *
 * It is deliberately simpler than a production analyzer. The Mav2Tester app ships a fuller
 * version that also gates on blinks, scores frame quality, and reports the vertical axis;
 * this one keeps to the parts that show the API.
 */

package com.everysight.samples.kmpcompose

import com.everysight.mav2.sdk.services.eyetracker.analyzers.M2AnalysisConclusion
import com.everysight.mav2.sdk.services.eyetracker.analyzers.M2AnalyzerInput
import com.everysight.mav2.sdk.services.eyetracker.analyzers.M2AnalyzerKey
import com.everysight.mav2.sdk.services.eyetracker.analyzers.M2EyeFeatureAnalyzer
import com.everysight.mav2.sdk.services.eyetracker.analyzers.M2EyeFeatureResult
import kotlin.math.max

/** Where the eye is pointed, horizontally. */
enum class M2LookDirection { Left, Center, Right }

/**
 * One frame's answer.
 *
 * [offset] is how far the eye has moved from its own resting point, in normalized units —
 * exposed because it is what makes the classification legible when you are tuning.
 */
class LeftRightResult(
    override val timestamp: Long,
    override val isValid: Boolean,
    override val confidence: Float,
    val direction: M2LookDirection,
    val offset: Float,
) : M2EyeFeatureResult() {

    override val conclusion: M2AnalysisConclusion
        get() = if (!isValid) M2AnalysisConclusion.WarmingUp
        else M2AnalysisConclusion(label = direction.name, detail = "offset ${(offset * 100f).toInt()}%")
}

/**
 * Classifies horizontal gaze without calibration.
 *
 * Three ideas, and they are the whole analyzer:
 *
 *  1. **Use the glint→pupil vector**, not the raw pupil position. The eye camera sits off to
 *     the side, so the raw position moves when the glasses shift on the face. The vector
 *     between the LED reflection and the pupil does not.
 *  2. **Learn the resting point** with a slow average. Nobody's neutral gaze is the camera's
 *     centre, and it drifts as the glasses settle.
 *  3. **Learn how far this wearer actually looks** and threshold on a fraction of that. A
 *     fixed threshold either misses small eyes or triggers constantly on large ones.
 */
class LeftRightGazeAnalyzer : M2EyeFeatureAnalyzer<LeftRightResult>(KEY) {

    companion object {
        /** Identifies this analyzer to the service. Register the same key to replace it. */
        val KEY = M2AnalyzerKey<LeftRightResult>("sample.leftRight")

        /** Smoothing on the raw vector — removes per-frame jitter without visible lag. */
        private const val SMOOTH = 0.6f

        /** Much slower average: this is the resting point, which should not follow a glance. */
        private const val NEUTRAL = 0.995f

        /** How much of the learned excursion counts as "still looking ahead". */
        private const val CENTER_FRACTION = 0.45f

        /** Peaks decay so a single extreme glance does not raise the bar forever. */
        private const val PEAK_DECAY = 0.999f

        /** Floor for the threshold, so noise cannot classify itself as a look. */
        private const val MIN_THRESHOLD = 0.02f
    }

    override val description = "Is the wearer looking left, right, or straight ahead?"

    private var smoothed = Float.NaN
    private var neutral = Float.NaN
    private var peakLeft = 0f
    private var peakRight = 0f

    /** Called once per eye frame, on the SDK's eye thread. Keep it cheap. */
    override fun onFrame(input: M2AnalyzerInput) {
        val frame = input.normalized
        val confidence = input.pupilVisibility.coerceIn(0f, 1f)

        // No usable geometry this frame - the eye is closed, or the pupil was not found.
        if (!frame.isGeometryValid) {
            emit(LeftRightResult(input.timestamp, isValid = false, confidence = confidence, direction = M2LookDirection.Center, offset = 0f))
            return
        }

        // Needs the LED glint. When it is momentarily missing, hold the last answer rather
        // than emitting a wrong one.
        val gaze = frame.gazeVectorNorm ?: return
        val x = gaze.first

        smoothed = if (smoothed.isNaN()) x else SMOOTH * smoothed + (1f - SMOOTH) * x
        neutral = if (neutral.isNaN()) smoothed else NEUTRAL * neutral + (1f - NEUTRAL) * smoothed

        val offset = smoothed - neutral

        // Track how far this wearer's eye actually travels in each direction.
        if (offset >= 0f) peakRight = max(offset, peakRight * PEAK_DECAY)
        else peakLeft = max(-offset, peakLeft * PEAK_DECAY)

        val rightThreshold = max(peakRight * CENTER_FRACTION, MIN_THRESHOLD)
        val leftThreshold = max(peakLeft * CENTER_FRACTION, MIN_THRESHOLD)

        val direction = when {
            offset > rightThreshold -> M2LookDirection.Right
            offset < -leftThreshold -> M2LookDirection.Left
            else -> M2LookDirection.Center
        }

        emit(LeftRightResult(input.timestamp, isValid = true, confidence = confidence, direction = direction, offset = offset))
    }

    /** Called when the service resets the session — forget everything learned so far. */
    override fun onReset() {
        smoothed = Float.NaN
        neutral = Float.NaN
        peakLeft = 0f
        peakRight = 0f
    }
}
