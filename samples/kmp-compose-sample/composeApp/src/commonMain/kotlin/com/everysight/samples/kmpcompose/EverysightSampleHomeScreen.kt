/*
 * Created by Everysight LTD.
 *
 * Default sample home screen rendered on the glasses HUD. Auto-loaded by the
 * sample as soon as the SDK is initialized. Demonstrates a clean composition
 * of M2RectFilled, M2Line, M2EllipseFilled, and M2Text drawables together with
 * the SDK's animator extensions, and serves as the formal entry-point template
 * SDK consumers can fork for their own welcome screen.
 */

package com.everysight.samples.kmpcompose

import com.everysight.mav2.sdk.Evs
import com.everysight.mav2.sdk.resources.M2FontResource
import com.everysight.mav2.sdk.uikit.animators.M2AnimatorRepeat
import com.everysight.mav2.sdk.uikit.animators.ext.translateXBy
import com.everysight.mav2.sdk.uikit.data.M2Align
import com.everysight.mav2.sdk.uikit.drawables.M2EllipseFilled
import com.everysight.mav2.sdk.uikit.drawables.M2Line
import com.everysight.mav2.sdk.uikit.drawables.M2RectFilled
import com.everysight.mav2.sdk.uikit.drawables.M2Text
import com.everysight.mav2.sdk.uikit.screens.M2Screen
import com.everysight.mav2.sdk.utils.M2Color

/** Default welcome/home screen shown on the glasses after Evs.init. */
class EverysightSampleHomeScreen : M2Screen(width = 420f, height = 300f, tag = "everysight-sample-home") {
    private val mainTitle = "MAVERICK AI"
    private var titleText: M2Text? = null
    private var subtitleText: M2Text? = null
    private var deviceNameText: M2Text? = null

    private var barLeft: M2RectFilled? = null
    private var barRight: M2RectFilled? = null
    private var lineTop: M2Line? = null
    private var lineBottom: M2Line? = null
    private var scanGlowTop: M2RectFilled? = null
    private var scanGlowBottom: M2RectFilled? = null
    private var dots: Array<M2EllipseFilled> = emptyArray()

    override fun onCreate() {
        super.onCreate()
        addHudDecorations()
        addTexts()
        startEntranceAnimations()
    }

    override fun onRelease() {
        titleText = null
        subtitleText = null
        deviceNameText = null
        barLeft = null
        barRight = null
        lineTop = null
        lineBottom = null
        scanGlowTop = null
        scanGlowBottom = null
        dots = emptyArray()
        super.onRelease()
    }

    private fun addHudDecorations() {
        val cx = width / 2f
        val cy = height / 2f
        val barLeft = M2RectFilled("barLeft").apply { setColor(M2Color.PureCyan.withAlpha(170u)) }
        val barRight = M2RectFilled("barRight").apply { setColor(M2Color.PureMagenta.withAlpha(170u)) }
        val lineTop = M2Line("lineTop").apply { setColor(M2Color.PureCyan.withAlpha(70u)); setStyle(1f) }
        val lineBottom = M2Line("lineBottom").apply { setColor(M2Color.PureMagenta.withAlpha(70u)); setStyle(1f) }
        val scanGlowTop = M2RectFilled("scanGlowTop").apply {
            setColor(M2Color.PureCyan.withAlpha(70u))
            setWidthHeight(54f, 2f)
        }
        val scanGlowBottom = M2RectFilled("scanGlowBottom").apply {
            setColor(M2Color.PureMagenta.withAlpha(70u))
            setWidthHeight(54f, 2f)
        }
        val dots = arrayOf(
            M2EllipseFilled("dot-cyan").apply { setColor(M2Color.PureCyan) },
            M2EllipseFilled("dot-magenta").apply { setColor(M2Color.PureMagenta) },
            M2EllipseFilled("dot-orange").apply { setColor(M2Color.Orange) },
            M2EllipseFilled("dot-blue").apply { setColor(M2Color.LightBlue) },
            M2EllipseFilled("dot-green").apply { setColor(M2Color.PureGreen.withAlpha(190u)) },
            M2EllipseFilled("dot-yellow").apply { setColor(M2Color.PureYellow.withAlpha(180u)) }
        )
        this.barLeft = barLeft
        this.barRight = barRight
        this.lineTop = lineTop
        this.lineBottom = lineBottom
        this.scanGlowTop = scanGlowTop
        this.scanGlowBottom = scanGlowBottom
        this.dots = dots

        val titleW = M2FontResource.fontSmall.getMeasuredContentWidth(mainTitle)
        val barW = 46f
        val barH = 3f
        val barGap = 12f
        barLeft.setWidthHeight(barW, barH)
        barLeft.setX(cx - titleW / 2f - barGap - barW)
        barLeft.setY(cy - 58f)
        barRight.setWidthHeight(barW, barH)
        barRight.setX(cx + titleW / 2f + barGap)
        barRight.setY(cy - 58f)

        lineTop.setX(cx - 120f); lineTop.setY(cy - 96f); lineTop.toCoord(240f, 0f)
        lineBottom.setX(cx - 100f); lineBottom.setY(cy + 98f); lineBottom.toCoord(200f, 0f)
        scanGlowTop.setXY(cx - 120f, cy - 97f)
        scanGlowBottom.setXY(cx - 100f, cy + 97f)

        val dotPositions = arrayOf(
            floatArrayOf(cx - 148f, cy - 82f, 4f),
            floatArrayOf(cx + 154f, cy - 62f, 3.5f),
            floatArrayOf(cx - 124f, cy + 102f, 3f),
            floatArrayOf(cx + 128f, cy + 86f, 4.5f),
            floatArrayOf(cx - 176f, cy + 16f, 2.5f),
            floatArrayOf(cx + 170f, cy + 6f, 2.5f)
        )
        dots.forEachIndexed { i, dot ->
            dot.setParams(dotPositions[i][2], dotPositions[i][0], dotPositions[i][1])
        }

        add(lineTop)
        add(lineBottom)
        add(scanGlowTop)
        add(scanGlowBottom)
        add(barLeft)
        add(barRight)
        dots.forEach { add(it) }
    }

    private fun addTexts() {
        val cx = width / 2f
        val titleText = M2Text("titleText").apply {
            setText(mainTitle)
            setFont(M2FontResource.fontSmall)
            setScale(1.1f)
            setColor(M2Color.PureCyan)
            setAlign(M2Align.CenterBoth)
            setXY(cx, height / 2f - 62f)
            addTo(this@EverysightSampleHomeScreen)
        }
        this.titleText = titleText
        val subtitleText = M2Text("subtitleText").apply {
            setText("SDK SAMPLE")
            setFont(M2FontResource.fontSmall)
            setColor(M2Color.White.withAlpha(180u))
            setAlign(M2Align.CenterBoth)
            setXY(cx, titleText.getBottomY() + 10f)
            addTo(this@EverysightSampleHomeScreen)
        }
        this.subtitleText = subtitleText
        val deviceNameText = M2Text("deviceNameText").apply {
            setText(Evs.glassesService.getDeviceName().ifBlank { "--" })
            setScale(0.8f)
            setFont(M2FontResource.fontSmall)
            setColor(M2Color.White.withAlpha(185u))
            setAlign(M2Align.CenterBoth)
            setXY(cx, subtitleText.getBottomY() + 18f)
            addTo(this@EverysightSampleHomeScreen)
        }
        this.deviceNameText = deviceNameText
    }

    private fun startEntranceAnimations() {
        barLeft?.translateXBy(2600, 12f, repeat = M2AnimatorRepeat.RepeatBack, apply = true)?.start()
        barRight?.translateXBy(2900, -12f, repeat = M2AnimatorRepeat.RepeatBack, apply = true)?.start()
        scanGlowTop?.translateXBy(2100, 190f, repeat = M2AnimatorRepeat.RepeatBack, apply = true)?.start()
        scanGlowBottom?.translateXBy(2400, 160f, repeat = M2AnimatorRepeat.RepeatBack, apply = true)?.start()
    }

}
