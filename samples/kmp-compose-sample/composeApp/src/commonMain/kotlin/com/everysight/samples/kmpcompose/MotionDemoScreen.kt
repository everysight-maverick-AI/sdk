/*
 * Created by Everysight LTD.
 *
 * Accelerating animators — the glasses-side motion added in SDK 0.2.0.
 *
 * Every animation on this screen is ONE instruction. The phone sends it once and
 * the glasses walk the curve themselves, so nothing is transmitted while it plays.
 * That is the whole point of these calls: a linear animator plus a phone-side timer
 * costs a packet per step and drifts; this costs a packet per gesture and does not.
 *
 * Requires glasses v47 or newer. On older firmware the animation fails at start()
 * with a version message and nothing is sent.
 */

package com.everysight.samples.kmpcompose

import com.everysight.mav2.sdk.uikit.animators.M2Animator
import com.everysight.mav2.sdk.uikit.animators.ext.M2Easing
import com.everysight.mav2.sdk.uikit.animators.ext.animateBounce
import com.everysight.mav2.sdk.uikit.animators.ext.animateFall
import com.everysight.mav2.sdk.uikit.animators.ext.animateThrow
import com.everysight.mav2.sdk.uikit.animators.ext.easeXTo
import com.everysight.mav2.sdk.uikit.data.M2Align
import com.everysight.mav2.sdk.uikit.drawables.M2EllipseFilled
import com.everysight.mav2.sdk.uikit.drawables.M2RectFilled
import com.everysight.mav2.sdk.uikit.drawables.M2Text
import com.everysight.mav2.sdk.uikit.screens.M2Screen
import com.everysight.mav2.sdk.utils.M2Color

/** Which motion the screen is currently demonstrating. */
enum class MotionDemo(val label: String) {
    Fall("Fall"),
    Bounce("Bounce"),
    Throw("Throw"),
    EaseIn("Ease in"),
    EaseOut("Ease out"),
}

/**
 * One ball, one floor, and a caption naming the call that moved it.
 *
 * The floor is drawn where the animations are aimed, so "the ball stops on the rail"
 * is something you can see rather than something you have to take on trust.
 */
class MotionDemoScreen : M2Screen(width = 420f, height = 300f, tag = "sample-motion") {

    private companion object {
        /** Ball diameter, and the left/right lanes the horizontal eases run between. */
        const val BALL_R = 9f
        const val LANE_LEFT = 60f
        const val LANE_RIGHT = 330f

        /** The floor the vertical animations are aimed at. */
        const val FLOOR_Y = 232f
        const val START_Y = 70f
    }

    private var ball: M2EllipseFilled? = null
    private var floor: M2RectFilled? = null
    private var caption: M2Text? = null

    /** Whatever is currently running, so the next demo can stop it before re-arming. */
    private var running: List<M2Animator> = emptyList()

    override fun onCreate() {
        super.onCreate()

        val floor = M2RectFilled("motion-floor").apply {
            setColor(M2Color.PureCyan.withAlpha(90u))
            setWidthHeight(300f, 2f)
            setXY(60f, FLOOR_Y + BALL_R)
        }
        val ball = M2EllipseFilled("motion-ball").apply {
            setColor(M2Color.PureCyan)
            setParams(BALL_R, LANE_LEFT, START_Y)
        }
        val caption = M2Text("Pick a motion", "motion-caption").apply {
            setColor(M2Color.White.withAlpha(200u))
            setXY(width / 2f, 264f)
            setAlign(M2Align.CenterHorizontal)
        }

        this.floor = floor
        this.ball = ball
        this.caption = caption

        add(floor)
        add(ball)
        add(caption)
    }

    override fun onRelease() {
        running.forEach { it.stop() }
        running = emptyList()
        ball = null
        floor = null
        caption = null
        super.onRelease()
    }

    /**
     * Runs one motion from a known starting point.
     *
     * Each branch is deliberately a single call. The ball is repositioned first because
     * these animators describe a curve from wherever the drawable currently is - they do
     * not rewind it for you. `apply = true` keeps the SDK-side position in step with the
     * glasses when a motion stops; without it the SDK still thinks the ball is at its start,
     * so the reposition below would be a no-op and the glasses would keep the ball where the
     * last motion left it.
     */
    fun run(demo: MotionDemo) {
        val ball = ball ?: return

        // Every demo re-arms from the same corner, so the curves are comparable. Bounce in
        // particular never ends on its own, so stopping the previous run is not optional.
        running.forEach { it.stop() }
        ball.setXY(LANE_LEFT, START_Y)

        running = when (demo) {
            // One instruction for the whole drop. The ball lands on FLOOR_Y and stops there:
            // the limit is the rail the value falls onto, and the animator then drops off the
            // glasses' list by itself.
            MotionDemo.Fall -> listOf(ball.animateFall(toY = FLOOR_Y, apply = true).also { it.start() })

            // The same fall, except the glasses reflect the arrival speed instead of stopping.
            // That reflection is lossless, so this bounces at a constant height forever - it
            // needs an explicit stop, which is what switching demos does above.
            MotionDemo.Bounce -> listOf(ball.animateBounce(toY = FLOOR_Y, apply = true).also { it.start() })

            // Two animators leaving together: a quadratic fall and a linear sideways leg timed
            // to last exactly as long, so they land together. Returns both.
            MotionDemo.Throw -> ball.animateThrow(dx = 240f, toY = FLOOR_Y, apply = true).onEach { it.start() }

            // Starts from rest and speeds up into the destination.
            MotionDemo.EaseIn -> {
                ball.setY(FLOOR_Y)
                listOf(ball.easeXTo(900, toX = LANE_RIGHT, easing = M2Easing.In, apply = true).also { it.start() })
            }

            // Leaves at speed and slows to a halt on the destination.
            MotionDemo.EaseOut -> {
                ball.setY(FLOOR_Y)
                listOf(ball.easeXTo(900, toX = LANE_RIGHT, easing = M2Easing.Out, apply = true).also { it.start() })
            }
        }

        caption?.setText(demo.label)
    }
}
