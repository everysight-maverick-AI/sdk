/*
 * Created by Everysight LTD.
 *
 * The eye as the SDK sees it.
 *
 * Every frame the eye tracker produces an [M2EyeTrackerResult]: two fitted ellipses (iris and
 * pupil), the two eye corners with an eyelid curve through each, and the LED glint. Drawing them
 * is the quickest way to tell a working eye tracker from a silent one, and it is what the
 * analyzers in this sample are reading underneath.
 *
 * Coordinates arrive in eye-camera pixels, so everything here scales by the camera size rather
 * than assuming the view's own pixels.
 *
 * The frame IMAGE is deliberately not drawn. The default driver runs the network on the glasses
 * and sends back this feature vector, not a picture, so `framePixels` is null on that path and a
 * bitmap would only ever be blank.
 */
package com.everysight.samples.kmpcompose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.everysight.mav2.sdk.Evs
import com.everysight.mav2.sdk.services.IM2EyeTrackerEvents
import com.everysight.mav2.sdk.services.eyetracker.data.M2EyeTrackerResult
import com.everysight.mav2.sdk.uikit.data.M2EyeTrackerErrors
import kotlinx.coroutines.launch

/** The eye camera's own resolution - every coordinate in the result is in these pixels. */
private const val EYE_CAMERA_WIDTH = 400f
private const val EYE_CAMERA_HEIGHT = 300f

private val IrisColor = Color(0xFF5CB8E6)
private val PupilColor = Color(0xFF34F27A)
private val CornerColor = Color(0xFFE6A835)
private val EyelidColor = Color(0xFFD4A84B)
private val LedColor = Color(0xFFE05050)
private val PupilCenterColor = Color(0xFFFFFFFF)
private val ViewBackground = Color(0xFF0C1018)
private val MutedText = Color(0xFF8B9DAE)

/**
 * Live drawing of the eye geometry the SDK extracts, or a line of text saying why there is
 * nothing to draw yet.
 */
@Composable
internal fun EyeFeatureView(modifier: Modifier = Modifier) {
    var result by remember { mutableStateOf<M2EyeTrackerResult?>(null) }
    var isOn by remember { mutableStateOf(false) }

    // SDK callbacks arrive on its own thread. Compose state has to be written on the main
    // thread, so every write is marshalled through this scope rather than assigned in place.
    val uiScope = rememberCoroutineScope()

    DisposableEffect(Unit) {
        val listener = object : IM2EyeTrackerEvents {
            override fun onM2EyeTrackerResult(result_: M2EyeTrackerResult) {
                uiScope.launch { result = result_ }
            }

            override fun onEyeTrackerStateChanged(isOn_: Boolean) {
                uiScope.launch {
                    isOn = isOn_
                    if (!isOn_) result = null
                }
            }

            override fun onError(type: M2EyeTrackerErrors, message: String) {
                // Nothing to draw for an error; the analyzers panel reports them.
            }
        }
        Evs.eyeTrackerService.registerListener(listener)
        onDispose { Evs.eyeTrackerService.unregisterListener(listener) }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(EYE_CAMERA_WIDTH / EYE_CAMERA_HEIGHT)
            .clip(RoundedCornerShape(12.dp))
            .background(ViewBackground),
        contentAlignment = Alignment.Center,
    ) {
        val r = result
        // An all-zero ellipse means the extractor found nothing this frame - drawing it would
        // pin every shape to the top-left corner.
        val hasGeometry = r != null &&
            ((r.iris.size >= 5 && (r.iris[0] > 0f || r.iris[1] > 0f)) ||
                (r.pupil.size >= 5 && (r.pupil[0] > 0f || r.pupil[1] > 0f)))

        if (hasGeometry && r != null) {
            Canvas(modifier = Modifier.fillMaxSize().padding(6.dp)) {
                drawEyeFeatures(r, size.width / EYE_CAMERA_WIDTH, size.height / EYE_CAMERA_HEIGHT)
            }
        } else {
            Text(
                text = if (isOn) "Waiting for eye data…" else "Eye tracker off - start the analyzers",
                color = MutedText,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }
}

/** Iris and pupil ellipses, the pupil centre, both corners with their eyelids, and the glint. */
private fun DrawScope.drawEyeFeatures(result: M2EyeTrackerResult, scaleX: Float, scaleY: Float) {
    val stroke = 2f * scaleX

    // iris and pupil are [cx, cy, fullWidth, fullHeight, angleDegrees] - diameters, not radii.
    drawEllipse(result.iris, IrisColor, scaleX, scaleY, stroke)
    drawEllipse(result.pupil, PupilColor, scaleX, scaleY, stroke)

    if (result.pupil.size >= 5 && (result.pupil[0] > 0f || result.pupil[1] > 0f)) {
        drawCircle(
            PupilCenterColor,
            radius = 2.5f * scaleX,
            center = Offset(result.pupil[0] * scaleX, result.pupil[1] * scaleY),
            style = Fill,
        )
    }

    val earX = result.earCorner.first * scaleX
    val earY = result.earCorner.second * scaleY
    val noseX = result.noseCorner.first * scaleX
    val noseY = result.noseCorner.second * scaleY
    if ((earX > 0f || earY > 0f) && (noseX > 0f || noseY > 0f)) {
        drawCircle(CornerColor, radius = 3f * scaleX, center = Offset(earX, earY), style = Fill)
        drawCircle(CornerColor, radius = 3f * scaleX, center = Offset(noseX, noseY), style = Fill)
        drawEyelid(earX, earY, noseX, noseY, result.eyelidTop.first * scaleX, result.eyelidTop.second * scaleY, stroke)
        drawEyelid(earX, earY, noseX, noseY, result.eyelidBottom.first * scaleX, result.eyelidBottom.second * scaleY, stroke)
    }

    // The glint is the reference the gaze vector is measured from, so it is worth seeing.
    result.ledPosition?.let { led ->
        val lx = led.first * scaleX
        val ly = led.second * scaleY
        if (lx > 0f || ly > 0f) {
            drawCircle(LedColor, radius = 3f * scaleX, center = Offset(lx, ly), style = Fill)
            drawCircle(
                LedColor.copy(alpha = 0.5f),
                radius = 6f * scaleX,
                center = Offset(lx, ly),
                style = Stroke(width = 1f * scaleX),
            )
        }
    }
}

private fun DrawScope.drawEllipse(e: FloatArray, color: Color, scaleX: Float, scaleY: Float, stroke: Float) {
    if (e.size < 5 || (e[0] <= 0f && e[1] <= 0f)) return
    val cx = e[0] * scaleX
    val cy = e[1] * scaleY
    val w = e[2] * scaleX
    val h = e[3] * scaleY
    rotate(degrees = e[4], pivot = Offset(cx, cy)) {
        drawOval(color, Offset(cx - w / 2f, cy - h / 2f), Size(w, h), style = Stroke(width = stroke))
    }
}

/** A quadratic through the corners, bent to pass through the eyelid's mid point. */
private fun DrawScope.drawEyelid(
    earX: Float,
    earY: Float,
    noseX: Float,
    noseY: Float,
    midX: Float,
    midY: Float,
    stroke: Float,
) {
    if (midX <= 0f && midY <= 0f) return
    val ctrlX = 2f * midX - (earX + noseX) / 2f
    val ctrlY = 2f * midY - (earY + noseY) / 2f
    val path = Path().apply {
        moveTo(earX, earY)
        quadraticBezierTo(ctrlX, ctrlY, noseX, noseY)
    }
    drawPath(path, EyelidColor, style = Stroke(width = stroke))
}
