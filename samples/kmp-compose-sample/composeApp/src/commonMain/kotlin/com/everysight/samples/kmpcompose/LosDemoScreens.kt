/*
 * Created by Everysight LTD.
 *
 * LOS demo screens adapted from the LiveAI playground for the public KMP
 * Compose sample. These examples show how sensor-driven 3D objects and
 * billboard images can be rendered directly on Maverick AI glasses.
 */

package com.everysight.samples.kmpcompose

import com.everysight.mav2.sdk.Evs
import com.everysight.mav2.sdk.resources.M2CacheScope
import com.everysight.mav2.sdk.resources.M2FontResource
import com.everysight.mav2.sdk.resources.M2ImageFile
import com.everysight.mav2.sdk.services.IM2SensorsEvents
import com.everysight.mav2.sdk.services.data.M2Quaternion
import com.everysight.mav2.sdk.uikit.animators.M2AnimatorRepeat
import com.everysight.mav2.sdk.uikit.animators.ext.animateTransformRx
import com.everysight.mav2.sdk.uikit.animators.ext.animateTransformRy
import com.everysight.mav2.sdk.uikit.animators.ext.animateTransformRz
import com.everysight.mav2.sdk.uikit.ar.M2ArFactory3D
import com.everysight.mav2.sdk.uikit.ar.M2ArLight
import com.everysight.mav2.sdk.uikit.base.M2ArScene
import com.everysight.mav2.sdk.uikit.data.M2Align
import com.everysight.mav2.sdk.uikit.data.M2DeviceType
import com.everysight.mav2.sdk.uikit.data.M2ImuCalibrationState
import com.everysight.mav2.sdk.uikit.data.M2Touch
import com.everysight.mav2.sdk.uikit.drawables.M2Image
import com.everysight.mav2.sdk.uikit.drawables.M2Text
import com.everysight.mav2.sdk.uikit.screens.M2FullScreen
import com.everysight.mav2.sdk.uikit.screens.M2RenderingRate
import com.everysight.mav2.sdk.utils.M2Color
import com.everysight.mav2.sdk.utils.M2Colors
import com.everysight.mav2.sdk.utils.mat.M2Mat
import com.everysight.mav2.sdk.utils.mat.M2Mat.TO_RAD
import com.everysight.mav2.sdk.utils.mat.M2TransformMatrix4x4

/** Adds a tap-driven sequence of 3D objects in the user's line of sight. */
internal class Los3dDemoScreen : M2FullScreen("Los3dDemoScreen") {
    private data class ObjectSpec(
        val fileName: String,
        val scale: Float,
        val distance: Float,
        val texturedDirectory: String? = null
    )

    private val light = M2ArLight(floatArrayOf(0f, 0f, 0.5f), "ArLight")
    private val message = M2Text()
    private val loadedItems = ArrayList<M2ArScene>()
    private val objects = listOf(
        ObjectSpec("earth/earth.obj", 0.15f, 100f, texturedDirectory = "obj_files/earth"),
        ObjectSpec("evsLogo.obj", 0.15f, 20f),
        ObjectSpec("pikachu.obj", 0.10f, 8f),
        ObjectSpec("hammer.obj", 0.05f, 8f)
    )
    private var gotQuat = false
    private var objectIndex = 0
    private var lastCalibState: M2ImuCalibrationState? = null

    companion object {
        const val MAX_ITEMS = 4
    }

    init {
        requireSensors()
    }

    override fun onCreate() {
        super.onCreate()
        gotQuat = false
        lastCalibState = null
        setRenderingRate(M2RenderingRate.Fast)
        Evs.sensorsService.registerListener(sensorsEvents)
        Evs.glassesService.enableDevice(M2DeviceType.Touch, true)
        Evs.glassesService.enableDevice(M2DeviceType.Turbo, true)
        add(light)
        message.apply {
            setFont(M2FontResource.fontSmall)
            setText("Waiting for sensors")
            setAlign(M2Align.CenterBoth)
            setColor(M2Color.Green)
            setXY(width / 2f, height / 2f)
            addTo(this@Los3dDemoScreen)
        }
    }

    override fun onRelease() {
        Evs.sensorsService.unregisterListener(sensorsEvents)
        Evs.glassesService.enableDevice(M2DeviceType.Turbo, false)
        gotQuat = false
        lastCalibState = null
        super.onRelease()
    }

    override fun onTouch(touch: M2Touch) {
        super.onTouch(touch)
        if (touch == M2Touch.Tap) addNextObject()
    }

    private fun addNextObject() {
        if (!gotQuat) return
        if (loadedItems.size >= MAX_ITEMS) {
            message.setText("Max $MAX_ITEMS items — remove or clear")
            return
        }
        val spec = objects[objectIndex]
        objectIndex = (objectIndex + 1) % objects.size
        if (spec.texturedDirectory != null) {
            val scene = createTexturedObjectScene(spec)
            add(scene)
            loadedItems += scene
            updateTapMessage()
            return
        }
        val scene = createObjectScene(spec).apply {
            setTransformMatrix(
                M2TransformMatrix4x4.builder()
                    .rotate(5f, 10f, 20f)
                    .translateAhead(spec.distance)
                    .scale(spec.scale)
                    .build()
            )
            setLightSource(light)
            if (getColor() == null) setColor(M2Colors.random())
            animateTransformRx(2000, 180f, M2AnimatorRepeat.RepeatBack).start()
            animateTransformRy(2000, 180f, M2AnimatorRepeat.RepeatBack).start()
            animateTransformRz(2000, 180f, M2AnimatorRepeat.RepeatBack).start()
        }
        add(scene)
        loadedItems += scene
        updateTapMessage()
    }

    fun removeLastItem(): Int {
        val last = loadedItems.removeLastOrNull() ?: return 0
        // Use removeSelf so the M2ArScene detaches itself + its child meshes
        // from the screen's render tree. Calling screen.remove(scene) alone
        // doesn't always tear down the AR scene's mesh children, which is
        // why the on-glasses objects were left visible after Remove Last.
        last.removeSelf()
        updateTapMessage()
        return loadedItems.size
    }

    fun clearAllItems(): Int {
        val removed = loadedItems.size
        loadedItems.toList().forEach { it.removeSelf() }
        loadedItems.clear()
        updateTapMessage()
        return removed
    }

    fun itemCount(): Int = loadedItems.size

    private fun updateTapMessage() {
        message.setText(
            if (loadedItems.size >= MAX_ITEMS) "Max $MAX_ITEMS items — remove or clear"
            else "Tap to add object (${loadedItems.size}/$MAX_ITEMS)"
        )
    }

    private fun createObjectScene(spec: ObjectSpec): M2ArScene {
        return M2ArFactory3D.fromObjFile("obj_files/${spec.fileName}")
    }

    private fun createTexturedObjectScene(spec: ObjectSpec): M2ArScene {
        val directory = spec.texturedDirectory ?: return M2ArScene(spec.fileName)
        return M2ArScene(spec.fileName).apply {
            M2ArFactory3D.fromObjFileSimple(directory).forEach { mesh ->
                mesh.apply {
                    setTransformMatrix(
                        M2TransformMatrix4x4.builder()
                            .translateAhead(spec.distance, verticalRotationDegrees = -3f, horizontalRotationDegrees = 0f)
                            .rotate(0f, 0f, 0f)
                            .scale(spec.scale)
                            .build()
                    )
                }
                    .animateTransformRz(5000L, 360f, M2AnimatorRepeat.Repeat)
                    .start()
                add(mesh)
            }
        }
    }

    private val sensorsEvents = object : IM2SensorsEvents {
        override fun onQuaternion(timestampMs: Long, quaternion: M2Quaternion, calibrationState: M2ImuCalibrationState) {
            if (!gotQuat) gotQuat = true
            if (lastCalibState != calibrationState) {
                message.setText(
                    when (calibrationState) {
                        M2ImuCalibrationState.Required -> "Calibration required"
                        M2ImuCalibrationState.Calibrating -> "Calibrating..."
                        else -> if (loadedItems.size >= MAX_ITEMS)
                            "Max $MAX_ITEMS items — remove or clear"
                        else
                            "Tap to add object (${loadedItems.size}/$MAX_ITEMS)"
                    }
                )
                lastCalibState = calibrationState
            }
        }
    }
}
/** Displays sample photos as line-of-sight billboards around the current view. */
internal class Los3dPicturesDemoScreen : M2FullScreen("Los3dPicturesDemoScreen") {
    private val files = arrayOf(
        M2ImageFile("playground/pic1.jpg", M2CacheScope.CachedAutoReleaseWhenUnused, "Sunset"),
        M2ImageFile("playground/pic2.jpg", M2CacheScope.CachedAutoReleaseWhenUnused, "Valley"),
        M2ImageFile("playground/pic3.jpg", M2CacheScope.CachedAutoReleaseWhenUnused, "Hotel View"),
        M2ImageFile("playground/pic4.jpg", M2CacheScope.CachedAutoReleaseWhenUnused, "Night"),
        M2ImageFile("playground/pic5.jpg", M2CacheScope.CachedAutoReleaseWhenUnused, "Lake"),
        M2ImageFile("playground/pic6.jpg", M2CacheScope.CachedAutoReleaseWhenUnused, "Desert")
    )
    private val pointingLaserEyeCoords = floatArrayOf(0f, 0f, -1f)
    private val pointingLaserWorldCoords = FloatArray(3)
    private var message: M2Text? = null
    private var quat = M2Quaternion()
    private var gotQuat = false
    private var picturesAdded = false
    private var lastCalibState: M2ImuCalibrationState? = null

    init {
        requireSensors()
    }

    override fun onCreate() {
        super.onCreate()
        gotQuat = false
        picturesAdded = false
        lastCalibState = null
        setRenderingRate(M2RenderingRate.Fast)
        Evs.sensorsService.registerListener(sensorsEvents)
        Evs.glassesService.enableDevice(M2DeviceType.Touch, true)
        showMessage("Waiting for sensors")
    }

    override fun onRelease() {
        Evs.sensorsService.unregisterListener(sensorsEvents)
        gotQuat = false
        picturesAdded = false
        lastCalibState = null
        super.onRelease()
    }

    override fun onTouch(touch: M2Touch) {
        super.onTouch(touch)
        if (touch == M2Touch.Tap) {
            if (!gotQuat) return
            if (picturesAdded) {
                removeAll()
                picturesAdded = false
                showMessage("Tap to add 3D pictures")
            } else {
                addPicsAsBillboards()
            }
        }
    }

    private fun showMessage(text: String) {
        message = M2Text().apply {
            setFont(M2FontResource.fontSmall)
            setText(text)
            setAlign(M2Align.CenterBoth)
            setColor(M2Color.Green)
            setXY(width / 2f, height / 2f)
            addTo(this@Los3dPicturesDemoScreen)
        }
    }

    private fun addPicsAsBillboards() {
        message?.removeSelf()
        val deltaDegVertical = 7f
        val deltaDegHorizontal = 5f
        val rotLeft = M2Quaternion().fromRotationVector(angleRad = deltaDegVertical * TO_RAD, axis_x = 0f, axis_y = 1f, axis_z = 0f)
        val rotRight = M2Quaternion().fromRotationVector(angleRad = -deltaDegVertical * TO_RAD, axis_x = 0f, axis_y = 1f, axis_z = 0f)
        val rotUp = M2Quaternion().fromRotationVector(angleRad = deltaDegHorizontal * TO_RAD, axis_x = 1f, axis_y = 0f, axis_z = 0f)
        val euler = quat.toEuler()
        val quatNoRoll = M2Quaternion().fromEuler(euler[0], euler[1], 0f)

        add(M2Image(files[0].apply { optimize() }).asBillboard(quatNoRoll))
        add(M2Image(files[1].apply { optimize() }).asBillboard(M2Quaternion(quatNoRoll).mul(rotLeft)))
        add(M2Image(files[2].apply { optimize() }).asBillboard(M2Quaternion(quatNoRoll).mul(rotRight)))
        add(M2Image(files[3].apply { optimize() }).asBillboard(M2Quaternion(quatNoRoll).mul(rotUp)))
        add(M2Image(files[4].apply { optimize() }).asBillboard(M2Quaternion(quatNoRoll).mul(rotLeft).mul(rotUp)))
        add(M2Image(files[5].apply { optimize() }).asBillboard(M2Quaternion(quatNoRoll).mul(rotRight).mul(rotUp)))
        picturesAdded = true
    }

    private val sensorsEvents = object : IM2SensorsEvents {
        override fun onQuaternion(timestampMs: Long, quaternion: M2Quaternion, calibrationState: M2ImuCalibrationState) {
            if (!gotQuat) gotQuat = true
            quat = quaternion
            M2Mat.eye2world(pointingLaserEyeCoords, quat, pointingLaserWorldCoords)
            if (!picturesAdded && lastCalibState != calibrationState) {
                message?.setText(
                    when (calibrationState) {
                        M2ImuCalibrationState.Required -> "Tap (calibration required)"
                        M2ImuCalibrationState.Calibrating -> "Tap (calibrating...)"
                        else -> "Tap to add 3D pictures"
                    }
                )
                lastCalibState = calibrationState
            }
        }
    }
}
