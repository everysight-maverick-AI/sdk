/*
 * Created by Everysight LTD.
 *
 * Video on the glasses — the headline feature of SDK 0.2.0.
 *
 * One drawable, M2VideoClip, plays whatever source you hand it. The four sources are
 * interchangeable from the app's point of view:
 *
 *   M2VideoFileResource         a bundled or downloaded H264 stream
 *   M2RTSPVideoResource         a network camera
 *   M2PhoneCameraVideoResource  this phone's camera, encoded on the fly
 *   M2ScreenMirrorVideoResource this phone's screen
 *
 * The glasses decode H264 baseline and nothing else, and they have a real budget:
 * M2VideoLimits is the published contract, and a source that does not fit is refused
 * at attach time rather than failing somewhere inside the decoder. Read it before
 * re-encoding anything.
 */

package com.everysight.samples.kmpcompose

import com.everysight.mav2.sdk.resources.M2VideoClipResource
import com.everysight.mav2.sdk.resources.M2VideoFileResource
import com.everysight.mav2.sdk.uikit.data.M2Align
import com.everysight.mav2.sdk.uikit.drawables.M2Text
import com.everysight.mav2.sdk.uikit.drawables.M2VideoClip
import com.everysight.mav2.sdk.uikit.screens.M2Screen
import com.everysight.mav2.sdk.utils.M2Color
import com.everysight.mav2.sdk.video.M2PhoneCameraConfig
import com.everysight.mav2.sdk.video.M2PhoneCameraVideoResource
import com.everysight.mav2.sdk.video.M2RTSPVideoResource
import com.everysight.mav2.sdk.video.M2RtspVideoConfig

/** Which source the clip is playing. */
enum class VideoSource(val label: String) {
    Clip("Video clip"),
    PhoneCamera("Phone camera"),
    WeatherCam("Weather cam"),
}

/**
 * A single [M2VideoClip] with a caption, and sources swapped underneath it.
 *
 * The clip is created once. Swapping sources is `setFill(newSource)` — the SDK tears the
 * previous decoder down and hands the slot over, so there is no clip to recreate and no
 * black frame in between.
 */
class VideoDemoScreen : M2Screen(width = 420f, height = 360f, tag = "sample-video") {

    private companion object {
        /**
         * The caption sits ABOVE the picture, and the screen is tall enough for a full-size
         * source underneath it.
         *
         * setFill resizes the drawable to the source's own pixel dimensions, so the layout does
         * not get to pick the picture's size - it has to leave room for a full-size source and
         * put the caption outside that area. A caption placed under a 200px-tall window was
         * covered the moment a 304px-tall source was attached.
         */
        const val CAPTION_Y = 10f
        const val CLIP_TOP = 44f
    }

    private var clip: M2VideoClip? = null
    private var caption: M2Text? = null

    /** The live sources, kept so they can be stopped when the screen goes away. */
    private var camera: M2PhoneCameraVideoResource? = null
    private var rtsp: M2RTSPVideoResource? = null

    override fun onCreate() {
        super.onCreate()

        val clip = M2VideoClip("sample-clip").apply {
            setXY(0f, CLIP_TOP)
        }
        val caption = M2Text("No source", "video-caption").apply {
            setColor(M2Color.White.withAlpha(200u))
            setXY(width / 2f, CAPTION_Y)
            setAlign(M2Align.CenterHorizontal)
        }

        this.clip = clip
        this.caption = caption

        add(clip)
        add(caption)
    }

    override fun onRelease() {
        // Live sources hold an encoder and, for the camera, a hardware handle. Releasing the
        // screen without closing them leaks both.
        closeLiveSources()
        clip = null
        caption = null
        super.onRelease()
    }

    /**
     * Attaches a pre-encoded clip.
     *
     * The bytes are read by the caller because reading a resource is platform work; the
     * SDK only wants the stream. The sample ships a raw Annex-B H264 elementary stream,
     * which [M2VideoFileResource] indexes directly on every platform. Whatever the source,
     * it must already be H264 baseline within [com.everysight.mav2.sdk.video.M2VideoLimits] -
     * the SDK re-wraps, it never transcodes.
     */
    fun playFile(bytes: ByteArray, source: VideoSource) {
        val resource = M2VideoFileResource(bytes)
        closeLiveSources()
        attach(resource, "${source.label} — ${resource.clipWidth}x${resource.clipHeight} @ ${resource.clipFps}fps")
    }

    /** Encodes this phone's camera and streams it to the glasses. */
    fun playPhoneCamera() {
        closeLiveSources()
        val source = M2PhoneCameraVideoResource(M2PhoneCameraConfig())
        camera = source
        // open() starts the capture session; the encoder then feeds the clip on its own.
        source.open()
        attach(source, VideoSource.PhoneCamera.label)
    }

    /** Pulls a public RTSP camera off the internet and re-encodes it for the glasses. */
    fun playRtsp(url: String) {
        closeLiveSources()
        val source = M2RTSPVideoResource(M2RtspVideoConfig(url = url))
        rtsp = source
        source.open()
        attach(source, VideoSource.WeatherCam.label)
    }

    /** Pauses or resumes without detaching the source. */
    fun togglePlayback(): Boolean {
        val clip = clip ?: return false
        if (clip.isPlaying()) clip.pause() else clip.play()
        return clip.isPlaying()
    }

    /**
     * A live source keeps its encoder - and, for the camera, the phone's camera hardware -
     * running until it is closed. Detaching it from the clip does not close it, so switching
     * source has to, or the camera stays lit behind an RTSP stream.
     */
    private fun closeLiveSources() {
        camera?.close()
        rtsp?.close()
        camera = null
        rtsp = null
    }

    /**
     * Attaches a source and centres it under the caption.
     *
     * The centring is re-done on every attach and reads the source's own dimensions, because
     * setFill has just resized the drawable to them - a position chosen for the previous
     * source's size would leave the next one off-centre.
     */
    private fun attach(source: M2VideoClipResource, label: String) {
        val clip = clip ?: return
        clip.setFill(source)
        clip.setXY((width - source.clipWidth) / 2f, CLIP_TOP)
        clip.play()
        caption?.setText(label)
    }
}
