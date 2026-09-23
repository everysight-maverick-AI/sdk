/*
 * Created by Everysight LTD.
 *
 * The state behind the camera lab: the options in force, the last frame as a bitmap, and the
 * numbers the readout shows. This is the smallest complete client of M2AIVisionService:
 *
 *   registerListener(events)   frames and errors arrive here
 *   startCapture(options)      begin, and also change anything later
 *   takePicture()              one photograph, when the options are M2StillOptions
 *   stopCapture()              done
 *
 * Frames are pooled by the SDK: the bitmap handed to onFrameReceived is reused for the next
 * frame, so the lab takes an owned copy for the preview and releases the frame straight away.
 */

package com.everysight.samples.kmpcompose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import com.everysight.mav2.sdk.Evs
import com.everysight.mav2.sdk.services.IM2AIFrame
import com.everysight.mav2.sdk.services.IM2AIVisionEvents
import com.everysight.mav2.sdk.services.M2AIVisionService
import com.everysight.mav2.sdk.uikit.data.M2AIFrameResolution
import com.everysight.mav2.sdk.uikit.data.M2CaptureOptions
import com.everysight.mav2.sdk.uikit.data.M2StillOptions
import com.everysight.mav2.sdk.utils.recycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** An owned copy of a pooled frame bitmap, safe to keep after the frame is released. */
expect fun ownedCopyOf(image: ImageBitmap): ImageBitmap

class CameraLab {
    /** What capture is asked to run with. The screen rewrites this on every control change. */
    var options: M2CaptureOptions by mutableStateOf(M2StillOptions(M2AIFrameResolution.RES_960x512_x2))
    /** The most recent frame, decoded, as an owned bitmap. */
    var preview: ImageBitmap? by mutableStateOf(null)
    var lastFrameEncodedBytes by mutableStateOf(0)
    var lastFrameWidth by mutableStateOf(0)
    var lastFrameHeight by mutableStateOf(0)
    var lastError: String? by mutableStateOf(null)

    private val mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var registered = false

    private val events = object : IM2AIVisionEvents {
        override fun onCaptureStateChanged(isOn: Boolean) {
            if (isOn) mainScope.launch { lastError = null }
        }

        override fun onFrameReceived(frame: IM2AIFrame) {
            val encoded = frame.getEncodedSize()
            val w = frame.getWidth()
            val h = frame.getHeight()
            // Copy first, release second: the pooled bitmap belongs to the SDK.
            val copy = ownedCopyOf(frame.getImageBitmap())
            frame.release()
            mainScope.launch {
                lastFrameEncodedBytes = encoded
                lastFrameWidth = w
                lastFrameHeight = h
                val previous = preview
                preview = copy
                previous?.recycle()
            }
        }

        override fun onError(type: M2AIVisionService.Errors, message: String) {
            mainScope.launch { lastError = "$type - $message" }
        }
    }

    /** Starts capture with [options], or applies them to a running capture - one call does both. */
    fun start() {
        if (!registered) {
            Evs.visionService.registerListener(events)
            registered = true
        }
        Evs.visionService.startCapture(options)
    }

    fun stop() {
        Evs.visionService.stopCapture()
        if (registered) {
            Evs.visionService.unregisterListener(events)
            registered = false
        }
        mainScope.launch {
            preview?.recycle()
            preview = null
        }
    }
}
