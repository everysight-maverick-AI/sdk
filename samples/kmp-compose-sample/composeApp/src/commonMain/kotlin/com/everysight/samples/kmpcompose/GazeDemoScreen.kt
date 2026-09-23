/*
 * Created by Everysight LTD.
 *
 * Showing an analyzer's output on the HUD.
 *
 * Three markers and two captions, fed by two analyzers at once: [LeftRightGazeAnalyzer]
 * lights a marker, [BlinkAnalyzer] fills in the blink line. This file is only the display,
 * and it is deliberately dumb — all the thinking happens in the analyzers, which are the
 * pieces an app actually writes.
 *
 * Two analyzers on one screen is the point of the layout: the service runs as many as you
 * register, each gets every frame, and neither knows the other exists.
 */

package com.everysight.samples.kmpcompose

import com.everysight.mav2.sdk.uikit.data.M2Align
import com.everysight.mav2.sdk.uikit.drawables.M2EllipseFilled
import com.everysight.mav2.sdk.uikit.drawables.M2Text
import com.everysight.mav2.sdk.uikit.screens.M2Screen
import com.everysight.mav2.sdk.utils.M2Color

/** Left / centre / right markers, with the active one lit. */
class GazeDemoScreen : M2Screen(width = 420f, height = 300f, tag = "sample-gaze") {

    private companion object {
        const val MARKER_R = 16f
        const val MARKER_Y = 140f

        /**
         * Lit and unlit markers.
         *
         * Both are ARGB values rather than one `M2Color` and one `withAlpha` result: mixing
         * the two makes `if (lit) ON else OFF` infer a common supertype that no `setColor`
         * overload accepts.
         */
        val ON = M2Color.PureCyan.argb
        val OFF = M2Color.PureCyan.withAlpha(45u)
    }

    private var left: M2EllipseFilled? = null
    private var center: M2EllipseFilled? = null
    private var right: M2EllipseFilled? = null
    private var caption: M2Text? = null
    private var blinkCaption: M2Text? = null

    override fun onCreate() {
        super.onCreate()

        val left = M2EllipseFilled("gaze-left").apply { setParams(MARKER_R, 90f, MARKER_Y); setColor(OFF) }
        val center = M2EllipseFilled("gaze-center").apply { setParams(MARKER_R, 210f, MARKER_Y); setColor(OFF) }
        val right = M2EllipseFilled("gaze-right").apply { setParams(MARKER_R, 330f, MARKER_Y); setColor(OFF) }
        val caption = M2Text("Waiting for the eye tracker", "gaze-caption").apply {
            setColor(M2Color.White.withAlpha(200u))
            setXY(width / 2f, 210f)
            setAlign(M2Align.CenterHorizontal)
        }
        val blinkCaption = M2Text("", "gaze-blink").apply {
            setColor(M2Color.PureCyan.withAlpha(180u))
            setXY(width / 2f, 240f)
            setAlign(M2Align.CenterHorizontal)
        }

        this.left = left
        this.center = center
        this.right = right
        this.caption = caption
        this.blinkCaption = blinkCaption

        add(left)
        add(center)
        add(right)
        add(caption)
        add(blinkCaption)
    }

    override fun onRelease() {
        left = null
        center = null
        right = null
        caption = null
        blinkCaption = null
        super.onRelease()
    }

    /**
     * Applies one analyzer result.
     *
     * Results arrive on the SDK thread, which is where drawables want to be touched anyway,
     * so there is nothing to marshal here.
     */
    fun show(result: LeftRightResult) {
        if (!result.isValid) {
            left?.setColor(OFF)
            center?.setColor(OFF)
            right?.setColor(OFF)
            caption?.setText("No eye")
            return
        }

        left?.setColor(if (result.direction == M2LookDirection.Left) ON else OFF)
        center?.setColor(if (result.direction == M2LookDirection.Center) ON else OFF)
        right?.setColor(if (result.direction == M2LookDirection.Right) ON else OFF)
        caption?.setText(result.conclusion.label)
    }

    /**
     * Applies one blink result.
     *
     * `blinkDetected` is true for a single frame, so anything that should happen once per
     * blink hangs off it. The steady-state numbers are in the conclusion's detail line.
     */
    fun show(result: BlinkResult) {
        blinkCaption?.setText(
            if (result.eyeState == M2EyeState.Closed) "Eye closed"
            else "${result.blinkCount} blinks · ${result.blinkRatePerMin.toInt()}/min"
        )
    }
}
