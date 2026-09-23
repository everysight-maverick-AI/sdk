/*
 * Created by Everysight LTD.
 *
 * What a shape can be filled with, and what text can do with a fill.
 *
 * A drawable has two independent fill slots:
 *
 *   gradient  — M2GradientFade, M2GradientLinear, M2EdgeFeather   (setFill / removeGradient)
 *   texture   — an image, a sprite, a texture resource            (setFill / removeTexture)
 *
 * They are separate because they are separate instructions to the glasses, and since
 * SDK 0.2.0 they compose: a gradient over a texture fades the picture. Before that the
 * two shared one slot and setting either cleared the other.
 *
 * A note that saves an afternoon: the display is additive and see-through. Black is not
 * drawable - it is simply the absence of light - so a gradient fades TOWARDS transparent
 * and there is no such thing as a dark scrim.
 */

package com.everysight.samples.kmpcompose

import com.everysight.mav2.sdk.resources.M2CacheScope
import com.everysight.mav2.sdk.resources.M2ImageFile
import com.everysight.mav2.sdk.uikit.data.M2Align
import com.everysight.mav2.sdk.uikit.data.M2EdgeFeather
import com.everysight.mav2.sdk.uikit.data.M2GradientFade
import com.everysight.mav2.sdk.uikit.data.M2GradientFill
import com.everysight.mav2.sdk.uikit.data.M2GradientLength
import com.everysight.mav2.sdk.uikit.data.M2GradientLinear
import com.everysight.mav2.sdk.uikit.data.M2TextureFit
import com.everysight.mav2.sdk.uikit.drawables.M2RectFilled
import com.everysight.mav2.sdk.uikit.drawables.M2Text
import com.everysight.mav2.sdk.uikit.screens.M2Screen
import com.everysight.mav2.sdk.utils.M2Color
import com.everysight.mav2.sdk.utils.mat.M2TransformMatrix3x3

/** The fills this screen cycles through. */
enum class FillDemo(val label: String) {
    Fade("Gradient fade"),
    TwoColour("Two-colour gradient"),
    Feather("Edge feather"),
    TextureNative("Texture: Native"),
    TextureStretch("Texture: Stretch"),
    TextureNearest("Texture: nearest-neighbour"),
    TextureFaded("Texture + fade"),
    TextRotated("Text: rotated"),
    TextFaded("Text: faded"),
}

/**
 * One panel, one caption, and a fill swapped underneath them.
 *
 * Each branch below is the whole API for that effect — there is no setup hidden
 * elsewhere in the file.
 */
class FillsDemoScreen : M2Screen(width = 420f, height = 300f, tag = "sample-fills") {

    private companion object {
        const val PANEL_X = 90f
        const val PANEL_Y = 70f
        const val PANEL_W = 240f
        const val PANEL_H = 130f
    }

    private var panel: M2RectFilled? = null
    private var caption: M2Text? = null
    private var sampleText: M2Text? = null

    /**
     * Held across fills so the image uploads once.
     *
     * `CachedAutoReleaseWhenUnused` lets the SDK drop it from the glasses' cache when the
     * last drawable using it goes away, which is the right default for sample content.
     */
    private val picture = M2ImageFile("playground/pic1.jpg", M2CacheScope.CachedAutoReleaseWhenUnused, "fills-pic")

    override fun onCreate() {
        super.onCreate()

        val panel = M2RectFilled("fills-panel").apply {
            setColor(M2Color.PureCyan)
            setWidthHeight(PANEL_W, PANEL_H)
            setXY(PANEL_X, PANEL_Y)
            setCornerRadius(10f)
        }
        val sampleText = M2Text("EVERYSIGHT", "fills-text").apply {
            setColor(M2Color.White)
            setScale(1.6f)
            setXY(width / 2f, 130f)
            setAlign(M2Align.CenterHorizontal)
        }
        val caption = M2Text("Pick a fill", "fills-caption").apply {
            setColor(M2Color.White.withAlpha(200u))
            setXY(width / 2f, 240f)
            setAlign(M2Align.CenterHorizontal)
        }

        this.panel = panel
        this.sampleText = sampleText
        this.caption = caption

        add(panel)
        add(caption)
        // sampleText is added and removed by show() - see showOnly below.
    }

    override fun onRelease() {
        panel = null
        caption = null
        sampleText = null
        super.onRelease()
    }

    /** Applies one fill, clearing whatever the previous one left behind. */
    fun show(demo: FillDemo) {
        val panel = panel ?: return
        val sampleText = sampleText ?: return

        reset(panel, sampleText)

        when (demo) {
            // Fades the shape's own colour out towards one end. `fraction(0.6f)` means the
            // ramp is 60% of the shape's width along the gradient's axis, so it scales with
            // the shape instead of needing a pixel length per size.
            FillDemo.Fade -> panel.setFill(
                M2GradientFade(
                    stop = M2GradientFill.Stop.End,
                    length = M2GradientLength.fraction(0.6f),
                    angleDeg = 0f,
                )
            )

            // Ramps between the shape's colour and a second one. Note the alpha stays solid:
            // a gradient pins the object's alpha opaque, so fade it with the fade above, not
            // by lowering the colour's alpha.
            FillDemo.TwoColour -> panel.setFill(
                M2GradientLinear(
                    stop = M2GradientFill.Stop.Start,
                    length = M2GradientLength.fraction(1f),
                    angleDeg = 0f,
                    otherColor = M2Color.PureMagenta,
                )
            )

            // Softens every edge inwards. This shares the gradient slot with the two above -
            // it is literally the same instruction - so a shape has a feather or a gradient,
            // never both. A feather also drops the corner radius, which is why the panel
            // looks square here.
            FillDemo.Feather -> panel.setFill(M2EdgeFeather(M2GradientLength.pixels(18f)))

            // One texel per pixel. The image is drawn at its own size and the shape is not
            // resized to match, so a smaller image leaves the edge row smeared across the rest.
            FillDemo.TextureNative -> panel.setFill(picture, fit = M2TextureFit.Native)

            // Fills the shape on both axes independently. Since glasses v47 this also works
            // under a transform matrix: the texture fills the shape's rect and the matrix
            // turns that rect.
            FillDemo.TextureStretch -> panel.setFill(picture, fit = M2TextureFit.Stretch)

            // The same stretch with filtering off - hard texel edges instead of a smooth
            // interpolation. Useful for pixel art and for anything with thin hard lines.
            FillDemo.TextureNearest -> panel.setFill(picture, fit = M2TextureFit.Stretch, smooth = false)

            // Both slots at once: the picture fills the shape and the gradient fades it out.
            // Older glasses draw the texture and ignore the fade, so this is safe to send.
            FillDemo.TextureFaded -> {
                panel.setFill(picture, fit = M2TextureFit.Stretch)
                panel.setFill(
                    M2GradientFade(
                        stop = M2GradientFill.Stop.End,
                        length = M2GradientLength.fraction(0.7f),
                        angleDeg = 0f,
                    )
                )
            }

            // Text takes an affine matrix like any other drawable. The rotation pivots on the
            // text's own alignment anchor, not on a corner, so a centred string turns in place.
            FillDemo.TextRotated -> {
                showOnly(sampleText)
                sampleText.setTransformMatrix(
                    M2TransformMatrix3x3.builder().apply { rotate(-12f) }.build()
                )
            }

            // Text also takes a fade, measured against its own line box - so the reach is
            // right without having to know how wide the string rendered.
            FillDemo.TextFaded -> {
                showOnly(sampleText)
                sampleText.setFill(
                    M2GradientFade(
                        stop = M2GradientFill.Stop.End,
                        length = M2GradientLength.fraction(0.8f),
                        angleDeg = 0f,
                    )
                )
            }
        }

        caption?.setText(demo.label)
    }

    /**
     * Returns both drawables to a known state.
     *
     * `removeFill` clears both slots; the two `remove*` calls exist for clearing one and
     * keeping the other.
     */
    private fun reset(panel: M2RectFilled, sampleText: M2Text) {
        panel.removeFill()
        panel.setColor(M2Color.PureCyan)
        panel.setWidthHeight(PANEL_W, PANEL_H)
        panel.setXY(PANEL_X, PANEL_Y)
        panel.setCornerRadius(10f)

        sampleText.removeFill()
        sampleText.setTransformMatrix(null)

        // Back to the shape demos: the panel is on screen, the text is not.
        remove(sampleText)
        add(panel)
    }

    /**
     * Swaps the panel out for the text.
     *
     * Adding and removing is the honest way to hide a drawable here: alpha is not, because a
     * gradient pins an object's alpha opaque and one of these demos sets a gradient.
     */
    private fun showOnly(text: M2Text) {
        panel?.let { remove(it) }
        add(text)
    }
}
