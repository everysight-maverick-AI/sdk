/*
 * Created by Everysight LTD.
 *
 * Controller and glasses HUD examples for the Maverick AI KMP Compose sample.
 * This file intentionally keeps the SDK calls near the UI actions so sample
 * users can see the lifecycle for init, connect, screen rendering, streams,
 * sensors, and service probes without following framework abstractions.
 */

@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.everysight.samples.kmpcompose

import com.everysight.mav2.sdk.Evs
import com.everysight.mav2.sdk.resources.M2AudioResource
import com.everysight.mav2.sdk.resources.M2CacheScope
import com.everysight.mav2.sdk.resources.M2FontResource
import com.everysight.mav2.sdk.resources.M2ImageFile
import com.everysight.mav2.sdk.services.IM2AIFrame
import com.everysight.mav2.sdk.services.IM2AIVisionEvents
import com.everysight.mav2.sdk.services.IM2GlassesConnectionEvents
import com.everysight.mav2.sdk.services.IM2GlassesSystemEvents
import com.everysight.mav2.sdk.services.IM2MicrophoneEvents
import com.everysight.mav2.sdk.services.IM2SensorsEvents
import com.everysight.mav2.sdk.services.M2AIVisionService
import com.everysight.mav2.sdk.services.M2AudioService
import com.everysight.mav2.sdk.services.M2MicService
import com.everysight.mav2.sdk.services.audio.M2AudioStreamError
import com.everysight.mav2.sdk.services.audio.M2AudioStreamInput
import com.everysight.mav2.sdk.services.data.M2Quaternion
import com.everysight.mav2.sdk.uikit.animators.ext.translateXBy
import com.everysight.mav2.sdk.uikit.base.M2Drawable
import com.everysight.mav2.sdk.uikit.data.M2ConnectionStatus
import com.everysight.mav2.sdk.uikit.data.M2AIFrameResolution
import com.everysight.mav2.sdk.uikit.data.M2ContinuousOptions
import com.everysight.mav2.sdk.uikit.data.M2DeviceType
import com.everysight.mav2.sdk.uikit.data.M2ImuCalibrationState
import com.everysight.mav2.sdk.uikit.data.M2AppUIOption
import com.everysight.mav2.sdk.uikit.data.M2SensorsRate
import com.everysight.mav2.sdk.uikit.data.M2Touch
import com.everysight.mav2.sdk.uikit.drawables.M2EllipseFilled
import com.everysight.mav2.sdk.uikit.drawables.M2Image
import com.everysight.mav2.sdk.uikit.drawables.M2Line
import com.everysight.mav2.sdk.uikit.drawables.M2Path
import com.everysight.mav2.sdk.uikit.drawables.M2RectFilled
import com.everysight.mav2.sdk.uikit.drawables.M2RectOutline
import com.everysight.mav2.sdk.uikit.drawables.M2Text
import com.everysight.mav2.sdk.uikit.drawables.ext.setDimensions
import com.everysight.mav2.sdk.uikit.screens.M2Screen
import com.everysight.mav2.sdk.utils.M2Color
import com.everysight.samples.kmpcompose.generated.resources.Res
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.ExperimentalResourceApi
import kotlin.math.PI
import kotlin.math.roundToInt

/** Where the desktop glasses simulator is expected to be listening. */
private const val SIMULATOR_HOST = "127.0.0.1"

/** A public weather camera, so the RTSP path can be tried without standing up a camera. */
private const val WEATHER_CAM_URL = "rtsp://stream.strba.sk:1935/strba/VYHLAD_NAMESTIE.stream"

/** A public ICY radio stream, so the speaker stream path can be tried without a server. */
private const val RADIO_STREAM_URL = "https://listen.181fm.com/181-beatles_128k.mp3"

/**
 * The bundled clip, under composeResources/files/. A raw H264 baseline elementary stream - the one
 * shape every platform reads directly, with no demuxer in the way.
 */
private const val SAMPLE_CLIP_PATH = "files/video/woman1.h264"

/**
 * The bundled sound.
 *
 * [M2AudioResource] resolves this itself through the resources service, so the path is
 * relative to `files/` and the app never reads the bytes - unlike video, where the app does.
 */
private const val SAMPLE_SOUND_PATH = "audio/sample_tone.wav"

/** Top-level tabs shown in the sample app. */
enum class SanityCategory(val title: String) {
    UIKitAnimators("UIKit + Animators"),
    AudioAiVision("Audio + AIVision"),
    Los("LOS"),
    Motion("Motion"),
    Fills("Fills + Text"),
    Video("Video"),
    Speaker("Speaker"),
    Gaze("Eye tracker"),
    Services("Services")
}

/** Buttons available in the UIKit and animator tab. */
enum class UikitAction { AddRect, AddEllipse, AddLine, AddPath, AddText, AddImage, AddAnimated, RemoveLast, ClearAll }

/** Stream and sensor toggles shared by the Audio + AIVision and LOS tabs. */
enum class StreamAction { ToggleMic, ToggleAiVision, ToggleLos, ToggleTouch }

/** LiveAI playground demos exposed from the LOS tab. */
enum class LosDemoAction { Show3dDemo, Show3dPicturesDemo, RemoveLast3dItem, ClearAll3dItems }

/** Video sources and transport controls. */
enum class VideoAction { PlayClip, PlayCamera, PlayWeatherCam, TogglePlayback }

/** The two ways to get sound out of the glasses speaker. */
enum class SpeakerAction { PlaySound, ToggleRadio }

/** Eye-tracker analyzer controls. */
enum class GazeAction { ToggleAnalyzers }

/**
 * Service probes shown in the Services tab.
 *
 * The last one is a whole feature in one call: a connection to the desktop simulator instead
 * of real hardware.
 */
enum class ServiceAction {
    BrightnessUp,
    BrightnessDown,
    OtaProbe,
    ConnectSimulator,
}

/**
 * Owns SDK interactions for the sample UI.
 *
 * The controller keeps UI state immutable and emits a full snapshot after each
 * SDK callback or button action. It is intentionally small enough to copy into
 * a consumer app when developers need a reference for connection, screens,
 * audio, AIVision, sensors, OTA, and display service calls.
 */
class Mav2ComposeController {

    data class StreamsState(
        val micOn: Boolean = false,
        val aiVisionOn: Boolean = false,
        val losOn: Boolean = false,
        val touchOn: Boolean = false,
        val yawDeg: Float = 0f,
        val pitchDeg: Float = 0f,
        val rollDeg: Float = 0f,
        val calib: String = "—",
        val micBytesPerSec: Int = 0,
        val aiVisionBytesPerSec: Int = 0,
        val aiVisionFps: Float = 0f,
        val lastTouch: String = "—"
    )

    data class ServicesState(
        val brightness: Int = 0,
        /** The firmware the connected glasses are running. */
        val glassesFirmware: String = "—",
        /** The newest firmware bundled with the SDK, and whether these glasses should take it. */
        val latestOta: String = "—"
    )

    data class UiState(
        val sdkStatus: String = "sdk not ready",
        val configuredLabel: String = "not configured",
        val isConnected: Boolean = false,
        val isReady: Boolean = false,
        val isScreenAdded: Boolean = false,
        val selectedCategory: SanityCategory = SanityCategory.UIKitAnimators,
        val shapeCount: Int = 0,
        val streams: StreamsState = StreamsState(),
        val services: ServicesState = ServicesState(),
        val activeLosDemo: LosDemoAction? = null,
        /** The radio stream is open, or opening - drives the toggle's on look. */
        val radioOn: Boolean = false,
        val lastAction: String = "Pick a category, then Show HUD"
    )

    private var listenerRegistered = false
    private var sensorsRegistered = false
    private var micRegistered = false
    private var aiVisionRegistered = false
    private var touchListenerRegistered = false
    private var hudUikit: UikitHudScreen? = null
    private var hudAudioAiVision: AudioAiVisionHudScreen? = null
    private var hudLos: LosHudScreen? = null
    private var hudLosDemo: M2Screen? = null
    private var hudServices: ServicesHudScreen? = null
    private var hudMotion: MotionDemoScreen? = null
    private var hudFills: FillsDemoScreen? = null
    private var hudVideo: VideoDemoScreen? = null
    private var hudSpeaker: SpeakerDemoScreen? = null
    private var hudGaze: GazeDemoScreen? = null

    /** The two analyzers, held so they can be unregistered again. */
    private var gazeAnalyzer: LeftRightGazeAnalyzer? = null
    private var blinkAnalyzer: BlinkAnalyzer? = null

    /**
     * Created once and reused. A resource carries its glasses-side cache slot, so a new
     * object per play would upload the same bytes again.
     */
    private var sampleSound: M2AudioResource? = null

    /**
     * Speaker stream state, reported by the SDK rather than assumed by the app.
     *
     * openStream() returning true only means the request was accepted. Whether audio actually
     * reaches the glasses - and whether the URL was reachable at all - arrives here, which is
     * why the sample listens instead of printing an optimistic "playing".
     */
    private val audioStreamListener = object : M2AudioService.IOnAudioStreamEvents {
        override fun onAudioStreamChanged(input: M2AudioStreamInput?) {
            if (input == null) {
                hudSpeaker?.show("Silent")
                state = state.copy(radioOn = false)
            }
        }

        override fun onAudioStreamStarted(input: M2AudioStreamInput) {
            hudSpeaker?.show("Internet radio", "playing")
            setLastAction("Radio playing")
        }

        override fun onAudioStreamError(error: M2AudioStreamError) {
            state = state.copy(radioOn = false)
            hudSpeaker?.show("Radio error", error.code.name)
            setLastAction("Radio error: ${error.code}")
        }

        // ICY metadata: the station names its current track. Free proof that real bytes are
        // flowing, which an open socket on its own is not.
        override fun onStreamMetadata(title: String) {
            hudSpeaker?.showDetail(title)
            setLastAction("Radio: $title")
        }
    }

    private var audioStreamRegistered = false

    /** The font fetched at runtime, once it has arrived. */
    private var ratePollJob: Job? = null
    private var micBytesAccumulator: Long = 0L
    private var aiVisionBytesAccumulator: Long = 0L
    private var state = UiState()
    private val uiScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    var onStateChanged: (UiState) -> Unit = {}

    private val connectionListener = object : IM2GlassesConnectionEvents {
        override fun onConnectionStatusChanged(status: M2ConnectionStatus) {
            state = state.copy(
                sdkStatus = when (status) {
                    M2ConnectionStatus.Ready -> "ready"
                    M2ConnectionStatus.Connected -> "connected"
                    M2ConnectionStatus.Connecting -> if (state.isReady) "ready" else "connecting"
                    M2ConnectionStatus.Disconnected,
                    M2ConnectionStatus.BluetoothOff,
                    M2ConnectionStatus.Failed,
                    M2ConnectionStatus.AuthFailed -> "disconnected"
                },
                isConnected = status == M2ConnectionStatus.Connected || status == M2ConnectionStatus.Ready,
                isReady = status == M2ConnectionStatus.Ready
            )
            emitState()
        }

        override fun onReady() {
            state = state.copy(sdkStatus = "ready", isConnected = true, isReady = true)
            refreshConfiguredState()
            refreshServicesState()
            emitState()
        }

        override fun onUnReady() {
            state = state.copy(sdkStatus = "disconnected", isConnected = false, isReady = false)
            emitState()
        }
    }

    private val sensorsListener = object : IM2SensorsEvents {
        override fun onQuaternion(timestampMs: Long, quaternion: M2Quaternion, calibrationState: M2ImuCalibrationState) {
            val euler = quaternion.toEuler()
            val yaw = (euler[0] * 180f / PI).toFloat()
            val pitch = (euler[1] * 180f / PI).toFloat()
            val roll = (euler[2] * 180f / PI).toFloat()
            state = state.copy(streams = state.streams.copy(yawDeg = yaw, pitchDeg = pitch, rollDeg = roll, calib = calibrationState.name))
            if (state.streams.losOn) {
                hudLos?.updateYpr(true, yaw, pitch, roll, calibrationState.name)
            }
            emitState()
        }
    }

    private val systemListener = object : IM2GlassesSystemEvents {
        override fun onTouch(touch: M2Touch) {
            updateTouchState(touchLabel(touch))
        }
    }

    private val micListener = object : IM2MicrophoneEvents {
        override fun onMicrophoneStateChanged(isOn: Boolean) {
            state = state.copy(streams = state.streams.copy(micOn = isOn))
            hudAudioAiVision?.updateMic(isOn, state.streams.micBytesPerSec)
            emitState()
        }
        override fun onMicrophoneRawReceived(micId: Int, rawData: ByteArray) {
            micBytesAccumulator += rawData.size
        }
        override fun onError(type: M2MicService.Errors, message: String) {
            setLastAction("Mic error: $type — $message")
        }
    }

    private val aiVisionListener = object : IM2AIVisionEvents {
        override fun onCaptureStateChanged(isOn: Boolean) {
            state = state.copy(streams = state.streams.copy(aiVisionOn = isOn))
            hudAudioAiVision?.updateAiVision(isOn, state.streams.aiVisionBytesPerSec, state.streams.aiVisionFps)
            emitState()
        }
        override fun onFrameReceived(frame: IM2AIFrame) {
            try {
                aiVisionBytesAccumulator += frame.toJpegData().size.toLong()
            } finally {
                frame.release()
            }
        }
        override fun onError(type: M2AIVisionService.Errors, message: String) {
            setLastAction("AIVision error: $type — $message")
        }
    }

    fun onInitCompleted() {
        ensureListenerRegistered()
        ensureMicAiVisionSensorsRegistered()
        ensureTouchListenerRegistered()
        addDefaultHomeScreen()
        state = state.copy(sdkStatus = "init ok")
        refreshConfiguredState()
        refreshServicesState()
        emitState()
        startRatePoller()
    }

    private var hudHome: EverysightSampleHomeScreen? = null

    private fun addDefaultHomeScreen() {
        if (hudHome != null) return
        val s = EverysightSampleHomeScreen()
        hudHome = s
        runCatching { Evs.screenService.addScreen(s) }
    }

    fun showConfigure() {
        Evs.glassesService.disconnect()
        Evs.showAppUI(M2AppUIOption.DefaultConfigure)
        state = state.copy(sdkStatus = "disconnected", isConnected = false, isReady = false)
        refreshConfiguredState()
        emitState()
    }

    fun showAdjust() {
        Evs.showAppUI(M2AppUIOption.DefaultAdjust)
        setLastAction("Default adjust UI requested")
    }

    fun toggleConnect() {
        ensureListenerRegistered()
        if (state.isConnected || state.isReady) {
            Evs.glassesService.disconnect()
            state = state.copy(sdkStatus = "disconnected", isConnected = false, isReady = false)
        } else {
            Evs.glassesService.connect()
            state = state.copy(sdkStatus = "connecting")
        }
        emitState()
    }

    fun selectCategory(category: SanityCategory) {
        if (category == state.selectedCategory) return
        val wasShown = state.isScreenAdded
        if (wasShown) removeAllHuds()
        state = state.copy(selectedCategory = category)
        if (wasShown) addCurrentHud() else emitState()
    }

    fun toggleScreen() {
        if (state.isScreenAdded) removeAllHuds() else addCurrentHud()
    }

    fun ensureScreenForPreview(): Boolean {
        if (!Evs.wasInitialized()) {
            setLastAction("Init the SDK first")
            return false
        }
        if (state.isScreenAdded) return false
        addCurrentHud()
        return true
    }

    private fun addCurrentHud() {
        if (!Evs.wasInitialized()) {
            setLastAction("Init the SDK first")
            return
        }
        when (state.selectedCategory) {
            SanityCategory.UIKitAnimators -> {
                val s = UikitHudScreen { count ->
                    state = state.copy(shapeCount = count)
                    emitState()
                }
                hudUikit = s
                Evs.screenService.addScreen(s)
                state = state.copy(isScreenAdded = true, shapeCount = 0, lastAction = "HUD: UIKit shapes")
            }
            SanityCategory.AudioAiVision -> {
                val s = AudioAiVisionHudScreen()
                hudAudioAiVision = s
                Evs.screenService.addScreen(s)
                s.updateMic(state.streams.micOn, state.streams.micBytesPerSec)
                s.updateAiVision(state.streams.aiVisionOn, state.streams.aiVisionBytesPerSec, state.streams.aiVisionFps)
                state = state.copy(isScreenAdded = true, lastAction = "HUD: Audio + AIVision streams")
            }
            SanityCategory.Los -> {
                val s = LosHudScreen { touchName ->
                    state = state.copy(streams = state.streams.copy(lastTouch = touchName))
                    emitState()
                }
                hudLos = s
                Evs.screenService.addScreen(s)
                s.updateYpr(state.streams.losOn, state.streams.yawDeg, state.streams.pitchDeg, state.streams.rollDeg, state.streams.calib)
                s.updateLastTouch(state.streams.lastTouch)
                state = state.copy(isScreenAdded = true, lastAction = "HUD: LOS sensors and touch")
            }
            SanityCategory.Services -> {
                val s = ServicesHudScreen()
                hudServices = s
                Evs.screenService.addScreen(s)
                s.updateAll(state.services)
                state = state.copy(isScreenAdded = true, lastAction = "HUD: OTA + display")
            }
            SanityCategory.Motion -> {
                val s = MotionDemoScreen()
                hudMotion = s
                Evs.screenService.addScreen(s)
                state = state.copy(isScreenAdded = true, lastAction = "HUD: accelerating animators")
            }
            SanityCategory.Fills -> {
                val s = FillsDemoScreen()
                hudFills = s
                Evs.screenService.addScreen(s)
                state = state.copy(isScreenAdded = true, lastAction = "HUD: gradients, textures and text")
            }
            SanityCategory.Video -> {
                val s = VideoDemoScreen()
                hudVideo = s
                Evs.screenService.addScreen(s)
                state = state.copy(isScreenAdded = true, lastAction = "HUD: video")
            }
            SanityCategory.Speaker -> {
                val s = SpeakerDemoScreen()
                hudSpeaker = s
                Evs.screenService.addScreen(s)
                state = state.copy(isScreenAdded = true, lastAction = "HUD: speaker")
            }
            SanityCategory.Gaze -> {
                val s = GazeDemoScreen()
                hudGaze = s
                Evs.screenService.addScreen(s)
                state = state.copy(isScreenAdded = true, lastAction = "HUD: eye-tracker analyzers")
            }
        }
        emitState()
    }

    private fun removeAllHuds() {
        // The analyzers outlive their screen unless they are taken down with it, and a
        // registered analyzer keeps running - and keeps the eye camera busy - with nothing
        // left to draw to.
        stopGazeAnalyzers()
        // Sound is not attached to a screen, so nothing stops the radio when the screen that
        // started it goes away. A sound is finite and can be left to finish; a stream cannot.
        if (Evs.audioService.isStreaming()) runCatching { Evs.audioService.closeStream() }

        hudUikit?.let { Evs.screenService.removeScreen(it) }
        hudAudioAiVision?.let { Evs.screenService.removeScreen(it) }
        hudLos?.let { Evs.screenService.removeScreen(it) }
        hudLosDemo?.let { Evs.screenService.removeScreen(it) }
        hudServices?.let { Evs.screenService.removeScreen(it) }
        hudMotion?.let { Evs.screenService.removeScreen(it) }
        hudFills?.let { Evs.screenService.removeScreen(it) }
        hudVideo?.let { Evs.screenService.removeScreen(it) }
        hudSpeaker?.let { Evs.screenService.removeScreen(it) }
        hudGaze?.let { Evs.screenService.removeScreen(it) }
        hudUikit = null; hudAudioAiVision = null; hudLos = null; hudLosDemo = null; hudServices = null
        hudMotion = null; hudFills = null; hudVideo = null; hudSpeaker = null; hudGaze = null
        state = state.copy(isScreenAdded = false, activeLosDemo = null)
        emitState()
    }

    fun runUikitAction(action: UikitAction) {
        if (state.selectedCategory != SanityCategory.UIKitAnimators) {
            setLastAction("Switch to ${SanityCategory.UIKitAnimators.title} first"); return
        }
        if (!state.isScreenAdded) addCurrentHud()
        val hud = hudUikit ?: return
        when (action) {
            UikitAction.AddRect -> hud.addRect()
            UikitAction.AddEllipse -> hud.addEllipse()
            UikitAction.AddLine -> hud.addLine()
            UikitAction.AddPath -> hud.addPath()
            UikitAction.AddText -> hud.addText()
            UikitAction.AddImage -> hud.addImage()
            UikitAction.AddAnimated -> hud.addAnimatedRect()
            UikitAction.RemoveLast -> hud.removeLast()
            UikitAction.ClearAll -> hud.clearShapes()
        }
        setLastAction("UIKit: ${action.name} — ${hud.count()} shape(s)")
    }

    fun runStreamAction(action: StreamAction) {
        when (action) {
            StreamAction.ToggleMic -> runCatching {
                val wasOn = Evs.micService.isOn()
                if (wasOn) {
                    Evs.micService.closeMicrophone()
                    state = state.copy(streams = state.streams.copy(micOn = false, micBytesPerSec = 0))
                    hudAudioAiVision?.updateMic(false, 0)
                    "Mic off"
                } else {
                    Evs.micService.openMicrophone(bitrateKBs = 16)
                    state = state.copy(streams = state.streams.copy(micOn = true))
                    hudAudioAiVision?.updateMic(true, 0)
                    "Mic on @ 16 KBs (waits for glasses)"
                }
            }.fold(::setLastAction) { setLastAction("Mic error: ${it.message}") }
            StreamAction.ToggleAiVision -> runCatching {
                val wasOn = Evs.visionService.isCameraOpen()
                if (wasOn) {
                    Evs.visionService.stopCapture()
                    state = state.copy(streams = state.streams.copy(aiVisionOn = false, aiVisionBytesPerSec = 0, aiVisionFps = 0f))
                    hudAudioAiVision?.updateAiVision(false, 0, 0f)
                    "AIVision off"
                } else {
                    // 0.2.0 replaced the old stream config with capture OPTIONS: you say what
                    // kind of capture you want - one still, a continuous stream, video - and the
                    // SDK works out the wire settings.
                    Evs.visionService.startCapture(
                        M2ContinuousOptions(resolution = M2AIFrameResolution.RES_960x512_x2)
                    )
                    state = state.copy(streams = state.streams.copy(aiVisionOn = true))
                    hudAudioAiVision?.updateAiVision(true, 0, 0f)
                    "AIVision streaming (waits for glasses)"
                }
            }.fold(::setLastAction) { setLastAction("AIVision error: ${it.message}") }
            StreamAction.ToggleLos -> runCatching {
                val nextOn = !state.streams.losOn
                if (nextOn) {
                    Evs.sensorsService.enableInertialSensors(M2SensorsRate.Fast)
                    state = state.copy(streams = state.streams.copy(losOn = true))
                    hudLos?.updateYpr(true, state.streams.yawDeg, state.streams.pitchDeg, state.streams.rollDeg, state.streams.calib)
                    "LOS sensors on (Fast rate)"
                } else {
                    Evs.sensorsService.disableInertialSensors()
                    state = state.copy(streams = state.streams.copy(losOn = false, calib = "—"))
                    hudLos?.updateYpr(false, 0f, 0f, 0f, "—")
                    "LOS sensors off"
                }
            }.fold(::setLastAction) { setLastAction("LOS error: ${it.message}") }
            StreamAction.ToggleTouch -> runCatching {
                val next = !state.streams.touchOn
                if (next) ensureTouchListenerRegistered()
                Evs.glassesService.enableDevice(M2DeviceType.Touch, next)
                val newLast = if (next) "waiting…" else "—"
                state = state.copy(streams = state.streams.copy(touchOn = next, lastTouch = newLast))
                hudLos?.updateLastTouch(newLast)
                "M2Touch ${if (next) "armed" else "disabled"}"
            }.fold(::setLastAction) { setLastAction("M2Touch error: ${it.message}") }
        }
        emitState()
    }

    fun runLosDemoAction(action: LosDemoAction) {
        if (!Evs.wasInitialized()) {
            setLastAction("Init the SDK first")
            return
        }
        when (action) {
            LosDemoAction.RemoveLast3dItem -> {
                val active = hudLosDemo as? Los3dDemoScreen
                if (active == null) {
                    setLastAction("3D demo not active")
                } else {
                    val remaining = active.removeLastItem()
                    setLastAction("3D demo: removed last (${remaining}/${Los3dDemoScreen.MAX_ITEMS})")
                }
                emitState()
                return
            }
            LosDemoAction.ClearAll3dItems -> {
                val active = hudLosDemo as? Los3dDemoScreen
                if (active == null) {
                    setLastAction("3D demo not active")
                } else {
                    val removed = active.clearAllItems()
                    setLastAction("3D demo: cleared $removed item(s)")
                }
                emitState()
                return
            }
            else -> Unit
        }
        removeAllHuds()
        val screen = when (action) {
            LosDemoAction.Show3dDemo -> Los3dDemoScreen()
            LosDemoAction.Show3dPicturesDemo -> Los3dPicturesDemoScreen()
            else -> return
        }
        hudLosDemo = screen
        Evs.screenService.addScreen(screen)
        state = state.copy(
            isScreenAdded = true,
            selectedCategory = SanityCategory.Los,
            activeLosDemo = action,
            lastAction = when (action) {
                LosDemoAction.Show3dDemo -> "LOS demo: 3D objects (up to ${Los3dDemoScreen.MAX_ITEMS})"
                LosDemoAction.Show3dPicturesDemo -> "LOS demo: 3D pictures"
                else -> state.lastAction
            }
        )
        emitState()
    }

    fun stopLosDemo() {
        val active = hudLosDemo
        if (active != null) {
            Evs.screenService.removeScreen(active)
            hudLosDemo = null
        }
        state = state.copy(
            isScreenAdded = false,
            activeLosDemo = null,
            lastAction = "LOS demo stopped"
        )
        emitState()
    }

    fun runServiceAction(action: ServiceAction) {
        when (action) {
            ServiceAction.BrightnessUp -> runCatching {
                val next = (Evs.displayService.getBrightness() + 30).coerceAtMost(255)
                Evs.displayService.setBrightness(next); refreshServicesState(); "Brightness=$next"
            }.fold(::setLastAction) { setLastAction("Display error: ${it.message}") }
            ServiceAction.BrightnessDown -> runCatching {
                val next = (Evs.displayService.getBrightness() - 30).coerceAtLeast(0)
                Evs.displayService.setBrightness(next); refreshServicesState(); "Brightness=$next"
            }.fold(::setLastAction) { setLastAction("Display error: ${it.message}") }
            ServiceAction.OtaProbe -> runCatching {
                refreshServicesState()
                "Glasses ${state.services.glassesFirmware} | latest OTA ${state.services.latestOta}"
            }.fold(::setLastAction) { setLastAction("OTA error: ${it.message}") }

            // The desktop simulator speaks the same wire protocol over a WebSocket, so
            // everything else in this app is unchanged - only the transport differs.
            ServiceAction.ConnectSimulator -> runCatching {
                Evs.glassesService.setConfiguredSimulator(SIMULATOR_HOST)
                "Connecting to the simulator at $SIMULATOR_HOST"
            }.fold(::setLastAction) { setLastAction("Simulator error: ${it.message}") }
        }
    }

    /**
     * Plays one of the motion recipes.
     *
     * Each is a single instruction that the glasses then walk on their own - see
     * [MotionDemoScreen].
     */
    fun runMotionAction(demo: MotionDemo) {
        if (state.selectedCategory != SanityCategory.Motion) {
            setLastAction("Switch to ${SanityCategory.Motion.title} first"); return
        }
        if (!state.isScreenAdded) addCurrentHud()
        hudMotion?.run(demo)
        setLastAction("Motion: ${demo.label}")
    }

    /** Applies one fill to the demo panel - see [FillsDemoScreen]. */
    fun runFillAction(demo: FillDemo) {
        if (state.selectedCategory != SanityCategory.Fills) {
            setLastAction("Switch to ${SanityCategory.Fills.title} first"); return
        }
        if (!state.isScreenAdded) addCurrentHud()
        hudFills?.show(demo)
        setLastAction("Fill: ${demo.label}")
    }

    /**
     * Video sources.
     *
     * Reading the bundled clip is platform work and can be slow, so it happens off the SDK
     * thread and only the finished bytes come back.
     */
    fun runVideoAction(action: VideoAction) {
        if (state.selectedCategory != SanityCategory.Video) {
            setLastAction("Switch to ${SanityCategory.Video.title} first"); return
        }
        if (!state.isScreenAdded) addCurrentHud()
        val hud = hudVideo ?: return
        when (action) {
            VideoAction.PlayClip -> playBundledClip(hud, SAMPLE_CLIP_PATH, VideoSource.Clip)
            VideoAction.PlayCamera -> runCatching {
                hud.playPhoneCamera(); "Video: phone camera"
            }.fold(::setLastAction) { setLastAction("Camera error: ${it.message}") }
            VideoAction.PlayWeatherCam -> runCatching {
                hud.playRtsp(WEATHER_CAM_URL); "Video: weather cam"
            }.fold(::setLastAction) { setLastAction("RTSP error: ${it.message}") }
            VideoAction.TogglePlayback -> {
                val playing = hud.togglePlayback()
                setLastAction(if (playing) "Video: playing" else "Video: paused")
            }
        }
    }

    private fun playBundledClip(hud: VideoDemoScreen, path: String, source: VideoSource) {
        setLastAction("Loading ${source.label}...")
        uiScope.launch {
            runCatching { readClipBytes(path) }
                .onSuccess { bytes ->
                    // The constructor is where a clip is accepted or rejected - it indexes the
                    // stream and checks the profile and NAL sizes - so it is inside the catch.
                    runCatching { hud.playFile(bytes, source) }
                        .fold({ setLastAction("Video: ${source.label}") }) {
                            setLastAction("Video rejected: ${it.message}")
                        }
                }
                .onFailure { setLastAction("Video error: ${it.message}") }
        }
    }

    /**
     * The glasses speaker.
     *
     * The two buttons are deliberately asymmetric: a sound is fire-and-forget, while a stream
     * is a resource the app owns until it closes it. Only one stream exists at a time, which
     * is why the radio button is a toggle rather than a play.
     */
    fun runSpeakerAction(action: SpeakerAction) {
        if (state.selectedCategory != SanityCategory.Speaker) {
            setLastAction("Switch to ${SanityCategory.Speaker.title} first"); return
        }
        if (!state.isScreenAdded) addCurrentHud()
        ensureAudioStreamListenerRegistered()
        when (action) {
            // The resource is uploaded to the glasses on first play and cached there, so
            // playing it again costs one small command and no transfer. That is also why it
            // is held in a field - a new object per play would upload the same bytes again.
            SpeakerAction.PlaySound -> runCatching {
                val sound = sampleSound ?: M2AudioResource(SAMPLE_SOUND_PATH).also { sampleSound = it }
                if (Evs.audioService.playSound(sound)) {
                    hudSpeaker?.show("Bundled sound", SAMPLE_SOUND_PATH)
                    "Playing $SAMPLE_SOUND_PATH"
                } else {
                    // The first press usually lands here: the bytes are still being uploaded.
                    "Sound not ready yet - press again"
                }
            }.fold(::setLastAction) { setLastAction("Audio error: ${it.message}") }

            // The SDK owns the socket and the MP3 decoder for a Url stream, so there is
            // nothing to feed - open it and the glasses start playing.
            SpeakerAction.ToggleRadio -> runCatching {
                if (Evs.audioService.isStreaming()) {
                    Evs.audioService.closeStream()
                    hudSpeaker?.show("Silent")
                    state = state.copy(radioOn = false)
                    "Radio stopped"
                } else {
                    hudSpeaker?.show("Internet radio", "connecting...")
                    if (Evs.audioService.openStream(M2AudioStreamInput.Url(RADIO_STREAM_URL))) {
                        // On as soon as the request is accepted, so the button flips at once;
                        // an unreachable URL flips it back through onAudioStreamError.
                        state = state.copy(radioOn = true)
                        "Radio opening: $RADIO_STREAM_URL"
                    } else {
                        "Radio failed: ${Evs.audioService.getLastStreamError()?.code}"
                    }
                }
            }.fold(::setLastAction) { setLastAction("Radio error: ${it.message}") }
        }
    }

    /**
     * Starts or stops the two eye-feature analyzers.
     *
     * Registering is the whole wiring: the service feeds every analyzer every frame and
     * delivers results on the SDK thread. Unregistering is not optional - an analyzer nobody
     * unregisters keeps consuming frames after its screen is gone.
     */
    fun runGazeAction(action: GazeAction) {
        if (state.selectedCategory != SanityCategory.Gaze) {
            setLastAction("Switch to ${SanityCategory.Gaze.title} first"); return
        }
        if (!state.isScreenAdded) addCurrentHud()
        when (action) {
            GazeAction.ToggleAnalyzers -> {
                if (gazeAnalyzer != null) {
                    stopGazeAnalyzers()
                    setLastAction("Eye analyzers stopped")
                } else runCatching {
                    // Registering an analyzer only subscribes it to the frames; the eye camera
                    // still has to be turned on, or nothing is ever delivered.
                    Evs.eyeTrackerService.enableEyeTracker()

                    val gaze = LeftRightGazeAnalyzer()
                    gaze.setResultListener { result -> hudGaze?.show(result) }
                    Evs.eyeTrackerService.registerAnalyzer(gaze)
                    gazeAnalyzer = gaze

                    val blink = BlinkAnalyzer()
                    blink.setResultListener { result -> hudGaze?.show(result) }
                    Evs.eyeTrackerService.registerAnalyzer(blink)
                    blinkAnalyzer = blink

                    "Eye analyzers running: left/right + blink"
                }.fold(::setLastAction) { setLastAction("Eye tracker error: ${it.message}") }
            }
        }
    }

    /**
     * Reads a bundled clip.
     *
     * Compose resources are the standard place for sample content, and reading them is a
     * suspend call - which is why the video action launches a coroutine rather than blocking
     * the SDK thread on file I/O.
     */
    @OptIn(ExperimentalResourceApi::class)
    private suspend fun readClipBytes(path: String): ByteArray =
        withContext(Dispatchers.Default) { Res.readBytes(path) }

    private fun stopGazeAnalyzers() {
        gazeAnalyzer?.let { runCatching { Evs.eyeTrackerService.unregisterAnalyzer(it) } }
        blinkAnalyzer?.let { runCatching { Evs.eyeTrackerService.unregisterAnalyzer(it) } }
        gazeAnalyzer = null
        blinkAnalyzer = null
        // Leaving it enabled keeps the eye camera running for nobody.
        runCatching { Evs.eyeTrackerService.disableEyeTracker() }
    }


    private fun ensureListenerRegistered() {
        if (!listenerRegistered) {
            Evs.glassesService.registerConnectionListener(connectionListener)
            listenerRegistered = true
        }
    }

    private fun ensureAudioStreamListenerRegistered() {
        if (!audioStreamRegistered) {
            Evs.audioService.registerStreamListener(audioStreamListener)
            audioStreamRegistered = true
        }
    }

    private fun ensureMicAiVisionSensorsRegistered() {
        if (!sensorsRegistered) { Evs.sensorsService.registerListener(sensorsListener); sensorsRegistered = true }
        if (!micRegistered) { Evs.micService.registerListener(micListener); micRegistered = true }
        if (!aiVisionRegistered) { Evs.visionService.registerListener(aiVisionListener); aiVisionRegistered = true }
    }

    private fun ensureTouchListenerRegistered() {
        if (!touchListenerRegistered) {
            Evs.glassesService.registerSystemListener(systemListener)
            touchListenerRegistered = true
        }
    }

    private fun updateTouchState(label: String) {
        state = state.copy(streams = state.streams.copy(lastTouch = label))
        hudLos?.updateLastTouch(label)
        emitState()
    }

    private fun refreshConfiguredState() {
        val name = Evs.glassesService.getDeviceName().trim()
        val address = Evs.glassesService.getDeviceAddress().trim()
        val configuredName = name.ifEmpty { address }
        state = state.copy(configuredLabel = configuredName.ifEmpty { "not configured" })
    }

    private fun refreshServicesState() {
        if (!Evs.wasInitialized()) return
        val brightness = runCatching { Evs.displayService.getBrightness() }.getOrDefault(0)
        // fwVersion() reads 0 until the glasses have reported their system status.
        val firmware = runCatching { Evs.glassesService.fwVersion() }.getOrDefault("0")
            .let { if (it == "0") "—" else "v$it" }
        // getAvailableVersions() is everything bundled with the SDK; getVersionToInstall() is the
        // newest of those these glasses may take, or null when they are already current.
        val latestOta = runCatching {
            val latest = Evs.otaService.getAvailableVersions().maxByOrNull { it.version }
            val toInstall = Evs.otaService.getVersionToInstall()
            when {
                latest == null -> "none bundled"
                toInstall != null -> "v${toInstall.version} (${toInstall.releaseDate}) - ready to install"
                else -> "v${latest.version} (${latest.releaseDate}) - up to date"
            }
        }.getOrDefault("—")
        state = state.copy(
            services = state.services.copy(brightness = brightness, glassesFirmware = firmware, latestOta = latestOta)
        )
        hudServices?.updateAll(state.services)
    }

    private fun startRatePoller() {
        ratePollJob?.cancel()
        ratePollJob = uiScope.launch {
            while (true) {
                delay(1000)
                val mic = micBytesAccumulator.toInt(); micBytesAccumulator = 0L
                val cam = aiVisionBytesAccumulator.toInt(); aiVisionBytesAccumulator = 0L
                val fps = runCatching { Evs.visionService.getReceivedFps() }.getOrDefault(0f)
                state = state.copy(streams = state.streams.copy(micBytesPerSec = mic, aiVisionBytesPerSec = cam, aiVisionFps = fps))
                hudAudioAiVision?.updateMic(state.streams.micOn, mic)
                hudAudioAiVision?.updateAiVision(state.streams.aiVisionOn, cam, fps)
                hudLos?.updateYpr(state.streams.losOn, state.streams.yawDeg, state.streams.pitchDeg, state.streams.rollDeg, state.streams.calib)
                emitState()
            }
        }
    }

    private fun setLastAction(message: String) {
        state = state.copy(lastAction = message)
        emitState()
    }

    private fun emitState() {
        val nextState = state
        uiScope.launch { onStateChanged(nextState) }
    }
}

internal fun formatDeg(v: Float): String = "${v.roundToInt()}°"

private fun touchLabel(touch: M2Touch): String {
    return when (touch) {
        M2Touch.Tap -> "tap"
        M2Touch.LongTap -> "long-tap"
        M2Touch.Forward -> "forward"
        M2Touch.Backward -> "backward"
        M2Touch.Down -> "down"
        M2Touch.Up -> "up"
    }
}

internal fun formatFps(v: Float): String {
    val scaled = (v * 10f).roundToInt()
    val whole = scaled / 10
    val frac = (if (scaled < 0) -scaled else scaled) % 10
    return "$whole.${frac}"
}

internal fun formatBytes(bytes: Int): String = when {
    bytes >= 1_000_000 -> "${formatFps(bytes / 1_000_000f)} MB"
    bytes >= 1_000 -> "${formatFps(bytes / 1_000f)} kB"
    else -> "$bytes B"
}

// ─── M2Screen subclasses (rendered on the glasses) ──────────────────────

private const val HUD_WIDTH = 540f
private const val HUD_HEIGHT = 280f
private const val UIKIT_IMAGE_SIZE = 64f
private const val UIKIT_GRID_START_X = 30f
private const val UIKIT_GRID_START_Y = 70f
private const val UIKIT_GRID_STEP = 70f

internal class UikitHudScreen(private val onCount: (Int) -> Unit) :
    M2Screen(width = HUD_WIDTH, height = HUD_HEIGHT, tag = "sanity-uikit-hud") {

    private val shapes = ArrayList<M2Drawable>()
    private var nextX = UIKIT_GRID_START_X
    private var nextY = UIKIT_GRID_START_Y

    override fun onCreate() {
        add(M2RectFilled(M2Color.Black).apply { setDimensions(0f, 0f, HUD_WIDTH, HUD_HEIGHT) })
        add(M2RectOutline(M2Color.White).apply { setDimensions(8f, 8f, HUD_WIDTH - 16f, HUD_HEIGHT - 16f); setStyle(2f) })
        add(M2Text("UIKit + Animators").apply { setXY(20f, 18f); setColor(M2Color.White); setScale(0.95f) })
        add(M2Text("Use phone buttons to add/remove shapes").apply { setXY(20f, 46f); setColor(M2Color.LightBlue); setScale(0.7f) })
    }

    fun count(): Int = shapes.size

    fun clearShapes() {
        shapes.toList().forEach { it.removeSelf() }
        shapes.clear()
        resetNextPosition()
        onCount(0)
    }

    fun removeLast() {
        shapes.removeLastOrNull()?.removeSelf() ?: return
        resetNextPosition()
        repeat(shapes.size) { advanceNextPosition() }
        onCount(shapes.size)
    }

    private fun place(d: M2Drawable) {
        add(d)
        shapes.add(d)
        advanceNextPosition()
        onCount(shapes.size)
    }

    private fun resetNextPosition() {
        nextX = UIKIT_GRID_START_X
        nextY = UIKIT_GRID_START_Y
    }

    private fun advanceNextPosition() {
        nextX += UIKIT_GRID_STEP
        if (nextX > HUD_WIDTH - 80f) {
            nextX = UIKIT_GRID_START_X
            nextY += UIKIT_GRID_STEP
        }
        if (nextY > HUD_HEIGHT - 60f) {
            nextY = UIKIT_GRID_START_Y
        }
    }

    fun addRect() = place(M2RectFilled(M2Color.Yellow).apply { setDimensions(nextX, nextY, 50f, 36f) })
    fun addEllipse() = place(M2EllipseFilled().apply { setColor(M2Color.Cyan); setParams(20f, nextX + 20f, nextY + 20f) })
    fun addLine() = place(M2Line().apply { setXY(nextX, nextY); setColor(M2Color.Green); setStyle(3f); toCoord(60f, 30f) })
    fun addPath() = place(
        M2Path().apply {
            setColor(M2Color.Orange); setStyle(2f)
            add(0f, 24f); add(20f, 0f); add(40f, 24f); add(60f, 0f); add(60f, 32f)
            shrink()
            setXY(nextX, nextY)
        }
    )
    fun addText() = place(M2Text("T${shapes.size}").apply { setXY(nextX, nextY); setColor(M2Color.White); setFont(M2FontResource.fontSmall) })

    /** Alternates the two logo colours, so repeated taps show more than one cached image. */
    private var nextImageIsYellow = true

    fun addImage() {
        runCatching {
            val file = if (nextImageIsYellow) "images/everysight_yellow.png" else "images/everysight_white.png"
            nextImageIsYellow = !nextImageIsYellow
            place(
                M2Image(M2ImageFile(file, M2CacheScope.CachedManualDelete)).apply {
                    setDimensions(nextX, nextY, UIKIT_IMAGE_SIZE, UIKIT_IMAGE_SIZE)
                }
            )
        }
    }

    fun addAnimatedRect() {
        val r = M2RectFilled(M2Color.Blue).apply { setDimensions(nextX, nextY, 50f, 36f) }
        place(r)
        runCatching { r.translateXBy(1500, 80f, apply = true).start() }
    }
}

internal class AudioAiVisionHudScreen :
    M2Screen(width = HUD_WIDTH, height = HUD_HEIGHT, tag = "sanity-audio-aivision-hud") {

    private val txtMic = M2Text("mic: off").apply { setColor(M2Color.White) }
    private val txtCam = M2Text("AIVision: off").apply { setColor(M2Color.White) }

    override fun onCreate() {
        add(M2RectFilled(M2Color.Black).apply { setDimensions(0f, 0f, HUD_WIDTH, HUD_HEIGHT) })
        add(M2RectOutline(M2Color.White).apply { setDimensions(8f, 8f, HUD_WIDTH - 16f, HUD_HEIGHT - 16f); setStyle(2f) })
        add(M2Text("Audio + AIVision").apply { setXY(20f, 18f); setColor(M2Color.White); setScale(0.95f) })
        txtMic.setXY(20f, 70f); add(txtMic)
        txtCam.setXY(20f, 110f); add(txtCam)
    }

    fun updateMic(on: Boolean, bytesPerSec: Int) {
        val txt = if (on) "mic: ON ${formatBytes(bytesPerSec)}/s" else "mic: off"
        runCatching { txtMic.setText(txt) }
    }

    fun updateAiVision(on: Boolean, bytesPerSec: Int, fps: Float) {
        val txt = if (on) "AIVision: ON ${formatBytes(bytesPerSec)}/s ${formatFps(fps)}fps" else "AIVision: off"
        runCatching { txtCam.setText(txt) }
    }
}

internal class LosHudScreen(private val onTouchEvent: (String) -> Unit) :
    M2Screen(width = HUD_WIDTH, height = HUD_HEIGHT, tag = "sanity-los-hud") {

    private val txtLos = M2Text("LOS: off").apply { setColor(M2Color.Cyan) }
    private val txtYpr = M2Text("Y --  P --  R --").apply { setColor(M2Color.Yellow) }
    private val txtTouch = M2Text("touch: —").apply { setColor(M2Color.Green) }

    override fun onCreate() {
        add(M2RectFilled(M2Color.Black).apply { setDimensions(0f, 0f, HUD_WIDTH, HUD_HEIGHT) })
        add(M2RectOutline(M2Color.White).apply { setDimensions(8f, 8f, HUD_WIDTH - 16f, HUD_HEIGHT - 16f); setStyle(2f) })
        add(M2Text("LOS + M2Touch").apply { setXY(20f, 18f); setColor(M2Color.White); setScale(0.95f) })
        txtLos.setXY(20f, 70f); add(txtLos)
        txtYpr.setXY(20f, 110f); add(txtYpr)
        txtTouch.setXY(20f, 170f); add(txtTouch)
    }

    override fun onTouch(touch: M2Touch) {
        super.onTouch(touch)
        val name = when (touch) {
            M2Touch.Tap -> "tap"
            M2Touch.LongTap -> "long-tap"
            M2Touch.Forward -> "forward"
            M2Touch.Backward -> "backward"
            M2Touch.Down -> "down"
            M2Touch.Up -> "up"
        }
        runCatching { txtTouch.setText("touch: $name") }
        onTouchEvent(name)
    }

    fun updateYpr(losOn: Boolean, yaw: Float, pitch: Float, roll: Float, calib: String) {
        runCatching { txtLos.setText(if (losOn) "LOS: ON $calib" else "LOS: off") }
        runCatching {
            val text = if (losOn) "Y ${formatDeg(yaw)}  P ${formatDeg(pitch)}  R ${formatDeg(roll)}" else "Y --  P --  R --"
            txtYpr.setText(text)
        }
    }

    fun updateLastTouch(label: String) {
        runCatching { txtTouch.setText("touch: $label") }
    }
}

internal class ServicesHudScreen :
    M2Screen(width = HUD_WIDTH, height = HUD_HEIGHT, tag = "sanity-services-hud") {

    private val txtBrightness = M2Text("brightness: --").apply { setColor(M2Color.White) }
    private val txtOta = M2Text("ota: --").apply { setColor(M2Color.Cyan) }

    override fun onCreate() {
        add(M2RectFilled(M2Color.Black).apply { setDimensions(0f, 0f, HUD_WIDTH, HUD_HEIGHT) })
        add(M2RectOutline(M2Color.White).apply { setDimensions(8f, 8f, HUD_WIDTH - 16f, HUD_HEIGHT - 16f); setStyle(2f) })
        add(M2Text("OTA + Display").apply { setXY(20f, 18f); setColor(M2Color.White); setScale(0.95f) })
        txtBrightness.setXY(20f, 56f); add(txtBrightness)
        txtOta.setXY(20f, 106f); add(txtOta)
    }

    fun updateAll(s: Mav2ComposeController.ServicesState) {
        runCatching { txtBrightness.setText("brightness: ${s.brightness}") }
        runCatching { txtOta.setText("glasses ${s.glassesFirmware}   latest ota ${s.latestOta.substringBefore(' ')}") }
    }
}
