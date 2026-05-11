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
import com.everysight.mav2.sdk.resources.CacheScope
import com.everysight.mav2.sdk.resources.M2FontResource
import com.everysight.mav2.sdk.resources.M2ImageFile
import com.everysight.mav2.sdk.services.IM2AIFrame
import com.everysight.mav2.sdk.services.IM2AIVisionEvents
import com.everysight.mav2.sdk.services.IM2GlassesConnectionEvents
import com.everysight.mav2.sdk.services.IM2GlassesSystemEvents
import com.everysight.mav2.sdk.services.IM2MicrophoneEvents
import com.everysight.mav2.sdk.services.IM2SensorsEvents
import com.everysight.mav2.sdk.services.M2AIVisionService
import com.everysight.mav2.sdk.services.M2MicService
import com.everysight.mav2.sdk.services.data.M2Quaternion
import com.everysight.mav2.sdk.uikit.animators.ext.translateXBy
import com.everysight.mav2.sdk.uikit.base.M2Drawable
import com.everysight.mav2.sdk.uikit.data.ConnectionStatus
import com.everysight.mav2.sdk.uikit.data.M2AIFrameResolution
import com.everysight.mav2.sdk.uikit.data.M2AIVisionStreamConfig
import com.everysight.mav2.sdk.uikit.data.M2DeviceType
import com.everysight.mav2.sdk.uikit.data.M2ImuCalibrationState
import com.everysight.mav2.sdk.uikit.data.M2ShowUIOption
import com.everysight.mav2.sdk.uikit.data.SensorsRate
import com.everysight.mav2.sdk.uikit.data.Touch
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.roundToInt

/** Top-level tabs shown in the sample app. */
enum class SanityCategory(val title: String) {
    UIKitAnimators("UIKit + Animators"),
    AudioAiVision("Audio + AIVision"),
    Los("LOS"),
    Services("OTA + Display")
}

/** Buttons available in the UIKit and animator tab. */
enum class UikitAction { AddRect, AddEllipse, AddLine, AddPath, AddText, AddImage, AddAnimated, RemoveLast, ClearAll }

/** Stream and sensor toggles shared by the Audio + AIVision and LOS tabs. */
enum class StreamAction { ToggleMic, ToggleAiVision, ToggleLos, ToggleTouch }

/** LiveAI playground demos exposed from the LOS tab. */
enum class LosDemoAction { Show3dDemo, Show3dPicturesDemo, RemoveLast3dItem, ClearAll3dItems }

/** Service probes shown in the OTA + Display tab. */
enum class ServiceAction { BrightnessUp, BrightnessDown, OtaProbe }

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
        val otaSummary: String = "—"
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
    private var ratePollJob: Job? = null
    private var micBytesAccumulator: Long = 0L
    private var aiVisionBytesAccumulator: Long = 0L
    private var state = UiState()
    private val uiScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    var onStateChanged: (UiState) -> Unit = {}

    private val connectionListener = object : IM2GlassesConnectionEvents {
        override fun onConnectionStatusChanged(status: ConnectionStatus) {
            state = state.copy(
                sdkStatus = when (status) {
                    ConnectionStatus.Ready -> "ready"
                    ConnectionStatus.Connected -> "connected"
                    ConnectionStatus.Connecting -> if (state.isReady) "ready" else "connecting"
                    ConnectionStatus.Disconnected,
                    ConnectionStatus.BluetoothOff,
                    ConnectionStatus.Failed,
                    ConnectionStatus.AuthFailed -> "disconnected"
                },
                isConnected = status == ConnectionStatus.Connected || status == ConnectionStatus.Ready,
                isReady = status == ConnectionStatus.Ready
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
        override fun onTouch(touch: Touch) {
            updateTouchState(touchLabel(touch))
        }
    }

    private val micListener = object : IM2MicrophoneEvents {
        override fun onMicrophoneStateChanged(isOn: Boolean) {
            state = state.copy(streams = state.streams.copy(micOn = isOn))
            hudAudioAiVision?.updateMic(isOn, state.streams.micBytesPerSec)
            emitState()
        }
        override fun onMicrophoneOggOpusFrameReceived(data: ByteArray, offset: Int, size: Int) {
            micBytesAccumulator += size
        }
        override fun onMicrophoneRawReceived(rawData: ByteArray) { /* counted via Ogg path */ }
        override fun onError(type: M2MicService.M2MicServiceErrors, message: String) {
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
        override fun onError(type: M2AIVisionService.M2AIVisionServiceErrors, message: String) {
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
        Evs.showUI(M2ShowUIOption.DefaultConfigure)
        state = state.copy(sdkStatus = "disconnected", isConnected = false, isReady = false)
        refreshConfiguredState()
        emitState()
    }

    fun showAdjust() {
        Evs.showUI(M2ShowUIOption.DefaultAdjust)
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
        }
        emitState()
    }

    private fun removeAllHuds() {
        hudUikit?.let { Evs.screenService.removeScreen(it) }
        hudAudioAiVision?.let { Evs.screenService.removeScreen(it) }
        hudLos?.let { Evs.screenService.removeScreen(it) }
        hudLosDemo?.let { Evs.screenService.removeScreen(it) }
        hudServices?.let { Evs.screenService.removeScreen(it) }
        hudUikit = null; hudAudioAiVision = null; hudLos = null; hudLosDemo = null; hudServices = null
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
                    Evs.micService.openMicrophone(16)
                    state = state.copy(streams = state.streams.copy(micOn = true))
                    hudAudioAiVision?.updateMic(true, 0)
                    "Mic on @ 16 KBs (waits for glasses)"
                }
            }.fold(::setLastAction) { setLastAction("Mic error: ${it.message}") }
            StreamAction.ToggleAiVision -> runCatching {
                val wasOn = Evs.visionService.isCapturing()
                if (wasOn) {
                    Evs.visionService.stopCapture()
                    state = state.copy(streams = state.streams.copy(aiVisionOn = false, aiVisionBytesPerSec = 0, aiVisionFps = 0f))
                    hudAudioAiVision?.updateAiVision(false, 0, 0f)
                    "AIVision off"
                } else {
                    Evs.visionService.startCapture(
                        M2AIVisionStreamConfig(
                            resolution = M2AIFrameResolution.RES_960x512_x2,
                            fpsDiv = M2AIVisionStreamConfig.FpsDividers.Fps5
                        )
                    )
                    state = state.copy(streams = state.streams.copy(aiVisionOn = true))
                    hudAudioAiVision?.updateAiVision(true, 0, 0f)
                    "AIVision streaming 5 fps (waits for glasses)"
                }
            }.fold(::setLastAction) { setLastAction("AIVision error: ${it.message}") }
            StreamAction.ToggleLos -> runCatching {
                val nextOn = !state.streams.losOn
                if (nextOn) {
                    Evs.sensorsService.enableInertialSensors(SensorsRate.Fast)
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
                "Touch ${if (next) "armed" else "disabled"}"
            }.fold(::setLastAction) { setLastAction("Touch error: ${it.message}") }
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
                val available = Evs.otaService.getAvailableVersions().size
                val target = Evs.otaService.getVersionToInstall()?.version?.toString() ?: "none"
                val summary = "$available avail / target=$target"
                state = state.copy(services = state.services.copy(otaSummary = summary))
                hudServices?.updateAll(state.services)
                "OTA $summary"
            }.fold(::setLastAction) { setLastAction("OTA error: ${it.message}") }
        }
    }


    private fun ensureListenerRegistered() {
        if (!listenerRegistered) {
            Evs.glassesService.registerConnectionListener(connectionListener)
            listenerRegistered = true
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
        state = state.copy(services = state.services.copy(brightness = brightness))
        hudServices?.updateAll(state.services)
    }

    private fun startRatePoller() {
        ratePollJob?.cancel()
        ratePollJob = uiScope.launch {
            while (true) {
                delay(1000)
                val mic = micBytesAccumulator.toInt(); micBytesAccumulator = 0L
                val cam = aiVisionBytesAccumulator.toInt(); aiVisionBytesAccumulator = 0L
                val fps = runCatching { Evs.visionService.getFps() }.getOrDefault(0f)
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

private fun touchLabel(touch: Touch): String {
    return when (touch) {
        Touch.Tap -> "tap"
        Touch.LongTap -> "long-tap"
        Touch.Forward -> "forward"
        Touch.Backward -> "backward"
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

    fun addImage() {
        runCatching {
            place(
                M2Image(M2ImageFile("images/apple.png", CacheScope.CachedManualDelete)).apply {
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
        add(M2Text("LOS + Touch").apply { setXY(20f, 18f); setColor(M2Color.White); setScale(0.95f) })
        txtLos.setXY(20f, 70f); add(txtLos)
        txtYpr.setXY(20f, 110f); add(txtYpr)
        txtTouch.setXY(20f, 170f); add(txtTouch)
    }

    override fun onTouch(touch: Touch) {
        super.onTouch(touch)
        val name = when (touch) {
            Touch.Tap -> "tap"
            Touch.LongTap -> "long-tap"
            Touch.Forward -> "forward"
            Touch.Backward -> "backward"
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
        runCatching { txtOta.setText("ota: ${s.otaSummary}") }
    }
}
