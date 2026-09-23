/*
 * Created by Everysight LTD.
 *
 * The camera lab - one screen that exposes every option of M2AIVisionService.startCapture.
 *
 * Every control is built from the SDK's own preset lists and ranges (M2AIFrameResolution.presets,
 * M2Exposure.presets, M2CompressionQuality.presets, M2Bandwidth.presets and their RANGEs), so what
 * can be picked here is exactly what the API accepts - nothing more. The three capture modes map to
 * the three options classes:
 *
 *   Snap    -> M2StillOptions          one photograph per takePicture()
 *   Stream  -> M2VideoOptions          video: FixedQuality or TargetBandwidth, with a capture rate
 *   Cont.   -> M2ContinuousOptions     photographs back to back
 *
 * Changing anything goes through the same startCapture(options) call; the SDK decides whether the
 * glasses can take the change live or the camera has to restart.
 */

package com.everysight.samples.kmpcompose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.everysight.mav2.sdk.Evs
import com.everysight.mav2.sdk.uikit.data.M2AIFrameResolution
import com.everysight.mav2.sdk.uikit.data.M2Bandwidth
import com.everysight.mav2.sdk.uikit.data.M2CaptureOptions
import com.everysight.mav2.sdk.uikit.data.M2CaptureRate
import com.everysight.mav2.sdk.uikit.data.M2CompressionQuality
import com.everysight.mav2.sdk.uikit.data.M2ContinuousOptions
import com.everysight.mav2.sdk.uikit.data.M2Exposure
import com.everysight.mav2.sdk.uikit.data.M2Position
import com.everysight.mav2.sdk.uikit.data.M2StillOptions
import com.everysight.mav2.sdk.uikit.data.M2VideoOptions
import com.everysight.mav2.sdk.uikit.data.sensorOriginX
import com.everysight.mav2.sdk.uikit.data.sensorOriginY
import com.everysight.mav2.sdk.utils.isRecycled
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/** Crop origins are sent to the glasses in steps of 4 sensor pixels. */
private const val CAMERA_CROP_STEP = 4
/** Chip label for the raw-value slider behind a preset row. */
private const val EXACT = "Exact"

/** Settings groups on the rail; each opens one small card. */
private const val GROUP_FRAME = "Frame"
private const val GROUP_POSITION = "Position"
private const val GROUP_EXPOSURE = "Exposure"
private const val GROUP_QUALITY = "Quality / bandwidth"
private const val GROUP_STREAM = "Stream rate"
private const val GROUP_CONTINUOUS = "Continuous"

private fun fmt2(v: Float): String = ((v * 100).roundToInt() / 100f).toString()

/** One line with every value the options currently hold, for the strip over the preview. */
private fun captureSummary(o: M2CaptureOptions): String {
    val r = o.resolution
    val res = "${r.w}×${r.h}${if (r.decimateX2) " x2" else ""}"
    val pos = when (val p = o.position) {
        is M2Position.At -> "At ${p.sensorX},${p.sensorY}"
        is M2Position.Around -> "Around ${p.sensorX},${p.sensorY}"
        else -> p.toString()
    }
    val exp = when (val e = o.exposure) {
        is M2Exposure.Exact -> "${e.lines} ln · gain ${e.gain}"
        else -> e.toString()
    }
    val mode = when (o) {
        is M2StillOptions -> "Snap · Q ${o.compressionQuality.value}"
        is M2ContinuousOptions -> "Cont · Q ${o.compressionQuality.value}" + if (o.pauseWhenBusy) " · pause when busy" else ""
        is M2VideoOptions.FixedQuality -> "Stream · Q ${o.compressionQuality.value} · ≤${o.captureRate.name.removePrefix("UpTo")} fps"
        is M2VideoOptions.TargetBandwidth -> "Stream · ${o.bandwidth.kibPerSec} KiB/s · ≤${o.captureRate.name.removePrefix("UpTo")} fps"
    }
    return "$mode · $res · $pos · $exp"
}

/** A plain dropdown: the current entry in a pill, the rest in a menu. */
@Composable
private fun <T> ComboBox(
    modifier: Modifier = Modifier,
    entries: Map<T, String>,
    value: Map.Entry<T, String>,
    enabled: Boolean = true,
    onValueChanged: (Map.Entry<T, String>) -> Unit,
) {
    var isExpanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled) { isExpanded = !isExpanded }
                .background(Color.White.copy(alpha = if (enabled) 0.12f else 0.06f), RoundedCornerShape(100))
                .padding(vertical = 9.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(value.value, color = Color.White, fontSize = 13.sp)
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = if (isExpanded) "Collapse" else "Expand", tint = Color.White)
        }
        DropdownMenu(expanded = isExpanded, onDismissRequest = { isExpanded = false }) {
            entries.forEach { entry ->
                DropdownMenuItem(
                    modifier = if (entry == value) Modifier.background(MaterialTheme.colorScheme.primary) else Modifier,
                    text = {
                        Text(
                            entry.value,
                            color = if (entry == value) MaterialTheme.colorScheme.onPrimary else Color.Unspecified,
                            fontWeight = if (entry == value) FontWeight.Bold else FontWeight.Normal,
                        )
                    },
                    onClick = {
                        isExpanded = false
                        onValueChanged(entry)
                    }
                )
            }
        }
    }
}

/** One label + [Switch] row, styled like the rest of the camera settings panel. */
@Composable
private fun CamToggleRow(
    label: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Spacer(Modifier.height(8.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.White, fontSize = 12.sp)
        Switch(checked = checked, enabled = enabled, onCheckedChange = onCheckedChange)
    }
}

/** One labelled row of selectable chips, in the style of the capture-rate picker. */
@Composable
private fun CamChipRow(
    label: String,
    entries: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Spacer(Modifier.height(8.dp))
    Text(label, color = Color.White, fontSize = 12.sp)
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        entries.forEach { entry ->
            val isSelected = entry == selected
            Text(
                text = entry,
                modifier = Modifier
                    .clickable { onSelect(entry) }
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        RoundedCornerShape(4.dp)
                    )
                    .padding(8.dp),
                color = if (isSelected) Color.White else Color.Gray,
                fontSize = 14.sp
            )
        }
    }
}

/**
 * One labelled [Slider] over [range], showing the current value. The range comes from
 * [M2Exposure] and [M2CompressionQuality], so the panel can only produce values the glasses accept.
 */
@Composable
private fun CamSliderRow(
    label: String,
    value: Int,
    range: IntRange,
    enabled: Boolean,
    step: Int = 1,
    /** Runs when the finger lifts - for settings that restart the camera, so a drag is one restart, not one per pixel. */
    onValueChangeFinished: (() -> Unit)? = null,
    onValueChange: (Int) -> Unit
) {
    Spacer(Modifier.height(8.dp))
    val sliderRange = if (range.first == range.last) range.first..(range.first + 1) else range
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.White, fontSize = 12.sp)
        Text("$value", color = Color.Yellow, fontSize = 12.sp)
    }
    Slider(
        value = value.toFloat(),
        onValueChange = {
            val raw = it.roundToInt().coerceIn(range)
            val snapped = range.first + ((raw - range.first) / step) * step
            onValueChange(snapped.coerceIn(range))
        },
        valueRange = sliderRange.first.toFloat()..sliderRange.last.toFloat(),
        onValueChangeFinished = onValueChangeFinished,
        enabled = enabled && range.first != range.last
    )
}

@Composable
private fun CamDiscreteSliderRow(
    label: String,
    value: Int,
    values: List<Int>,
    enabled: Boolean,
    onValueChangeFinished: (() -> Unit)? = null,
    onValueChange: (Int) -> Unit
) {
    if (values.isEmpty()) return
    val selectedIndex = values.indices.minByOrNull { kotlin.math.abs(values[it] - value) } ?: 0
    Spacer(Modifier.height(8.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.White, fontSize = 12.sp)
        Text("${values[selectedIndex]}", color = Color.Yellow, fontSize = 12.sp)
    }
    Slider(
        value = selectedIndex.toFloat(),
        onValueChange = {
            onValueChange(values[it.roundToInt().coerceIn(values.indices)])
        },
        valueRange = 0f..values.lastIndex.toFloat(),
        steps = (values.size - 2).coerceAtLeast(0),
        onValueChangeFinished = onValueChangeFinished,
        enabled = enabled
    )
}

/**
 * The camera lab: every capture option the SDK has, as a control you can drag while frames
 * arrive. Read `updateConfig` below - that is the whole API in one function.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CameraLabScreen(
    modifier: Modifier = Modifier,
    lab: CameraLab,
    onClose: () -> Unit
) {
    // This page is the reference for the capture-options API: every control is built from the
    // SDK's own preset lists and ranges, so what can be picked here is exactly what the API takes.
    var selectedMode by remember { mutableStateOf(0) } // 0: Snap, 1: Stream, 2: Continuous
    var selectedRate by remember { mutableStateOf(M2CaptureRate.UpTo5) }
    var selectedResolution by remember { mutableStateOf(M2AIFrameResolution.RES_960x512_x2) }
    var presetResolution by remember { mutableStateOf(selectedResolution) }
    // Position: one of the API's kinds; At takes a top-left corner, Around a centre point.
    var positionKind by remember { mutableStateOf("Center") }
    val positionKinds = linkedMapOf(
        "Center" to "Center",
        "Top" to "Top (centred)",
        "Bottom" to "Bottom (centred)",
        "At" to "At (top-left X/Y)",
        "Around" to "Around (centre X/Y)",
    )
    var customResolutionEnabled by remember { mutableStateOf(false) }
    var customSensorWidth by remember { mutableStateOf(selectedResolution.sensorWidth) }
    var customSensorHeight by remember { mutableStateOf(selectedResolution.sensorHeight) }
    var customDecimateX2 by remember { mutableStateOf(selectedResolution.decimateX2) }
    var cropX by remember { mutableStateOf(0) }
    var cropY by remember {
        mutableStateOf(
            ((M2AIFrameResolution.SENSOR_HEIGHT - selectedResolution.sensorHeight) / 2)
                .let { it - it % CAMERA_CROP_STEP }
        )
    }
    var isCameraRunning by remember { mutableStateOf(false) }
    // Glasses link: with no glasses there is no camera - the shutter is off and disabled, and a
    // running capture is shown as stopped (the SDK restarts it on its own when they return).
    var glassesReady by remember { mutableStateOf(Evs.glassesService.isReady()) }
    LaunchedEffect(Unit) {
        while (true) {
            val ready = Evs.glassesService.isReady()
            if (ready != glassesReady) glassesReady = ready
            if (!ready && isCameraRunning) isCameraRunning = false
            delay(500)
        }
    }
    var actualFps by remember { mutableStateOf(0f) }
    var actualKBs by remember { mutableStateOf(0f) }
    var openGroup by remember { mutableStateOf<String?>(null) }   // the one settings card shown, or none
    // Presets are the API's named values; "Exact" exposes the raw slider behind each of them.
    var exposurePreset by remember { mutableStateOf("Auto") }
    var exposureLines by remember { mutableStateOf(256) }             // sensor lines, M2Exposure.LINES_RANGE
    var gainRegister by remember { mutableStateOf(32) }               // sensor register, M2Exposure.GAIN_RANGE (16 = x1.0)
    var qualityPreset by remember { mutableStateOf("Standard") }
    var bandwidthPreset by remember { mutableStateOf("Balanced") }
    // The two Exact values are separate: quality and bandwidth are different numbers with
    // different meanings, so switching bitrate mode must not carry one into the other.
    var exactQuality by remember { mutableStateOf(M2CompressionQuality.Standard.value) }
    var exactBandwidth by remember { mutableStateOf(M2Bandwidth.Balanced.kibPerSec) }
    var bitrateMode by remember { mutableStateOf(false) }   // Stream only: a cap is the exception
    // Chip rows come from the API's preset lists, plus "Exact" for the raw slider. The named
    // presets already include each range's maximum (Maximum, Unlimited, Darkest, UpTo30).
    val exposurePresets = listOf("Auto") + M2Exposure.presets.map { it.toString() } + EXACT
    val qualityPresets = M2CompressionQuality.presets.map { it.toString() } + EXACT
    val bandwidthPresets = M2Bandwidth.presets.map { it.toString() } + EXACT
    val qualityRange = M2CompressionQuality.RANGE
    val bandwidthRange = M2Bandwidth.RANGE

    // SDK debug lines carry the per-frame request (crop, quality, bitrate) - the evidence this
    // page exists to produce - and are off by default. On while the page is open.
    DisposableEffect(Unit) {
        Evs.logger.enableDebugLogs(true)
        onDispose { Evs.logger.enableDebugLogs(false) }
    }

    // The settings panel is height-capped and scrolls, so a slider revealed by picking
    // "Exact" can land below the fold. Scroll it into view when it appears.
    val exposureDetailRequester = remember { BringIntoViewRequester() }
    val qualityDetailRequester = remember { BringIntoViewRequester() }
    LaunchedEffect(exposurePreset) {
        if (exposurePreset == EXACT) exposureDetailRequester.bringIntoView()
    }
    LaunchedEffect(qualityPreset, bandwidthPreset, bitrateMode, selectedMode) {
        val exactShown = if (bitrateMode) bandwidthPreset == EXACT else qualityPreset == EXACT
        if (exactShown) qualityDetailRequester.bringIntoView()
    }
    var autoPauseWhenBusy by remember { mutableStateOf(false) }

    val resolutions = M2AIFrameResolution.presets

    // Read the rates the SDK already measures, rather than re-deriving fps here: its window
    // counts the frames that actually arrived, and it is the only place that sees the
    // compressed size of each one.
    // Polled rather than read on each frame, so the readout also falls to 0 when frames stop.
    LaunchedEffect(isCameraRunning) {
        if (!isCameraRunning) {
            actualFps = 0f
            actualKBs = 0f
            return@LaunchedEffect
        }
        while (true) {
            actualFps = Evs.visionService.getReceivedFps()
            actualKBs = Evs.visionService.getReceivedBytesPerSec() / 1024f
            delay(500)
        }
    }

    // Auto-update the options when a control changes.
    fun updateConfig() {
        // A named chip is the preset object itself; "Exact" is the raw value from the slider.
        val exposureSetting: M2Exposure = when (exposurePreset) {
            "Auto" -> M2Exposure.Auto
            EXACT -> M2Exposure.Exact(exposureLines, gainRegister)
            else -> M2Exposure.presets.first { it.toString() == exposurePreset }
        }
        val qualitySetting: M2CompressionQuality =
            M2CompressionQuality.presets.firstOrNull { it.toString() == qualityPreset }
                ?: M2CompressionQuality.Exact(exactQuality)
        val bandwidthSetting: M2Bandwidth =
            M2Bandwidth.presets.firstOrNull { it.toString() == bandwidthPreset }
                ?: M2Bandwidth.Exact(exactBandwidth)
        val positionSetting: M2Position = when (positionKind) {
            "Top" -> M2Position.Top
            "Bottom" -> M2Position.Bottom
            "At" -> M2Position.At(cropX, cropY)
            "Around" -> M2Position.Around(cropX, cropY)
            else -> M2Position.Center
        }
        lab.options = when (selectedMode) {
            // Video: the one mode with a bandwidth cap, and the one with a capture rate.
            1 -> if (bitrateMode) {
                M2VideoOptions.TargetBandwidth(
                    resolution = selectedResolution,
                    bandwidth = bandwidthSetting,
                    exposure = exposureSetting,
                    position = positionSetting,
                    captureRate = selectedRate,
                )
            } else {
                M2VideoOptions.FixedQuality(
                    resolution = selectedResolution,
                    compressionQuality = qualitySetting,
                    exposure = exposureSetting,
                    position = positionSetting,
                    captureRate = selectedRate,
                )
            }

            2 -> M2ContinuousOptions(
                resolution = selectedResolution,
                exposure = exposureSetting,
                compressionQuality = qualitySetting,
                position = positionSetting,
                pauseWhenBusy = autoPauseWhenBusy,
            )

            else -> M2StillOptions(
                resolution = selectedResolution,
                exposure = exposureSetting,
                compressionQuality = qualitySetting,
                position = positionSetting,
            )
        }
        // start() works out for itself whether the change can go out with the next frame or
        // needs the camera restarted, so an edit takes effect immediately either way.
        if (isCameraRunning) lab.start()
    }

    fun alignedCrop(value: Int, max: Int): Int {
        val clamped = value.coerceIn(0, max.coerceAtLeast(0))
        return clamped - clamped % CAMERA_CROP_STEP
    }

    fun syncResolution(resolution: M2AIFrameResolution) {
        selectedResolution = resolution
        customSensorWidth = resolution.sensorWidth
        customSensorHeight = resolution.sensorHeight
        customDecimateX2 = resolution.decimateX2
        cropX = alignedCrop(cropX, M2AIFrameResolution.SENSOR_WIDTH - resolution.sensorWidth)
        cropY = alignedCrop(cropY, M2AIFrameResolution.SENSOR_HEIGHT - resolution.sensorHeight)
        updateConfig()
    }

    fun applyCustomResolution() {
        val scale = if (customDecimateX2) 2 else 1
        syncResolution(
            M2AIFrameResolution(
                w = customSensorWidth / scale,
                h = customSensorHeight / scale,
                decimateX2 = customDecimateX2,
            )
        )
    }

    Box(
        modifier = modifier.fillMaxSize().background(Color.Black)
    ) {
        // Camera Preview Layer
        lab.preview?.let {
            if(!it.isRecycled()) {
                Image(
                    bitmap = it,
                    contentDescription = "Preview",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // UI Overlay. The preview stays uncovered: settings live in one small card at a time,
        // opened from the rail of group buttons on the right.
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
                if (!glassesReady) {
                    Text(
                        text = "Disconnected",
                        modifier = Modifier
                            .background(Color(0xFF3A1111), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        color = Color(0xFFFF8A65),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall
                    )
                } else if (isCameraRunning) {
                    Text(
                        text = "${fmt2(actualFps)} FPS  ·  ${fmt2(actualKBs)} KiB/s",
                        modifier = Modifier
                            .background(Color(0xFF111723), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        color = Color.Yellow,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                Spacer(Modifier.size(48.dp))
            }
            // Status strip: the options the API currently holds, so the cards can stay closed.
            Text(
                text = lab.lastError?.let { "Error: $it" } ?: captureSummary(lab.options),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                color = Color.White,
                fontSize = 11.sp,
            )

            // Middle: free preview on the left, one open card plus the group rail on the right.
            val railGroups = buildList {
                add(GROUP_FRAME to "Res")
                add(GROUP_POSITION to "Pos")
                add(GROUP_EXPOSURE to "Exp")
                add(GROUP_QUALITY to if (bitrateMode) "Rate" else "Qual")
                if (selectedMode == 1) add(GROUP_STREAM to "FPS")
                if (selectedMode == 2) add(GROUP_CONTINUOUS to "Pause")
            }
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f).padding(top = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.Top
            ) {
                val group = openGroup
                if (group != null) {
                    Column(
                        modifier = Modifier
                            .width(300.dp)
                            .heightIn(max = 400.dp)
                            .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { openGroup = null },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(group, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Icon(Icons.Default.Close, contentDescription = "Close $group", tint = Color.White)
                        }
                        when (group) {
                            GROUP_FRAME -> {
                    // Resolution Selector
                    Text("Resolution", color = Color.White, fontSize = 12.sp)
                    Text(
                        text = "Frame ${fmt2(lab.lastFrameEncodedBytes / 1024f)} KiB  ·  " +
                            "${lab.lastFrameWidth}×${lab.lastFrameHeight} decoded " +
                            "${lab.lastFrameWidth * lab.lastFrameHeight * 2 / 1024} KiB",
                        color = Color.Yellow,
                        fontSize = 12.sp
                    )
                    val resolutionOptions = remember(resolutions, customResolutionEnabled, selectedResolution) {
                        linkedMapOf<M2AIFrameResolution, String>().apply {
                            if (customResolutionEnabled && selectedResolution !in resolutions) {
                                put(selectedResolution, "${selectedResolution.w}x${selectedResolution.h} (Custom)")
                            }
                            resolutions.forEach { res ->
                                put(res, "${res.w}x${res.h}${if (res.decimateX2) " (X2)" else ""}")
                            }
                        }
                    }
                    val selectedResEntry = resolutionOptions.entries.find { it.key == selectedResolution }
                        ?: resolutionOptions.entries.first()

                    ComboBox(
                        modifier = Modifier.fillMaxWidth(),
                        entries = resolutionOptions,
                        value = selectedResEntry,
                        enabled = true,
                        onValueChanged = {
                            presetResolution = it.key
                            customResolutionEnabled = false
                            syncResolution(it.key)
                        }
                    )

                    CamToggleRow("Custom resolution", customResolutionEnabled, true) {
                        customResolutionEnabled = it
                        if (it) applyCustomResolution() else syncResolution(presetResolution)
                    }
                    // A size change restarts the camera (the glasses lock the frame size when a
                    // stream starts), so these apply when the finger lifts, not on every pixel.
                    CamDiscreteSliderRow(
                        label = "Sensor width",
                        value = customSensorWidth,
                        values = M2AIFrameResolution.supportedSensorWidths,
                        enabled = customResolutionEnabled,
                        onValueChangeFinished = { applyCustomResolution() },
                    ) {
                        customSensorWidth = it
                    }
                    CamSliderRow(
                        label = "Sensor height",
                        value = customSensorHeight,
                        range = M2AIFrameResolution.MIN_SIDE..M2AIFrameResolution.SENSOR_HEIGHT,
                        enabled = customResolutionEnabled,
                        step = M2AIFrameResolution.HEIGHT_STEP,
                        onValueChangeFinished = { applyCustomResolution() },
                    ) {
                        customSensorHeight = it
                    }
                    CamToggleRow("Decimate X2", customDecimateX2, customResolutionEnabled) {
                        customDecimateX2 = it
                        applyCustomResolution()
                    }
                    Text(
                        "Sensor ${selectedResolution.sensorWidth}×${selectedResolution.sensorHeight}  ·  " +
                            "Delivered ${selectedResolution.w}×${selectedResolution.h}",
                        color = Color.LightGray,
                        fontSize = 11.sp,
                    )

                            }
                            GROUP_POSITION -> {
                    // Position: where on the sensor the frame is read from.
                    Spacer(Modifier.height(8.dp))
                    Text("Position", color = Color.White, fontSize = 12.sp)
                    ComboBox(
                        modifier = Modifier.fillMaxWidth(),
                        entries = positionKinds,
                        value = positionKinds.entries.first { it.key == positionKind },
                        enabled = true,
                        onValueChanged = {
                            positionKind = it.key
                            updateConfig()
                        }
                    )
                    // At: the top-left corner, so the sliders stop where the frame meets the
                    // sensor edge. Around: a centre point anywhere on the sensor; the SDK clamps.
                    if (positionKind == "At" || positionKind == "Around") {
                        val isCorner = positionKind == "At"
                        val xMax = if (isCorner) (M2AIFrameResolution.SENSOR_WIDTH - selectedResolution.sensorWidth).coerceAtLeast(0)
                            else M2AIFrameResolution.SENSOR_WIDTH
                        val yMax = if (isCorner) (M2AIFrameResolution.SENSOR_HEIGHT - selectedResolution.sensorHeight).coerceAtLeast(0)
                            else M2AIFrameResolution.SENSOR_HEIGHT
                        CamSliderRow(
                            label = if (isCorner) "Left X (sensor px)" else "Centre X (sensor px)",
                            value = cropX.coerceIn(0, xMax),
                            range = 0..xMax,
                            enabled = true,
                            step = CAMERA_CROP_STEP,
                        ) {
                            cropX = alignedCrop(it, xMax)
                            updateConfig()
                        }
                        CamSliderRow(
                            label = if (isCorner) "Top Y (sensor px)" else "Centre Y (sensor px)",
                            value = cropY.coerceIn(0, yMax),
                            range = 0..yMax,
                            enabled = true,
                            step = CAMERA_CROP_STEP,
                        ) {
                            cropY = alignedCrop(it, yMax)
                            updateConfig()
                        }
                    }
                    val winW = selectedResolution.sensorWidth
                    val winH = selectedResolution.sensorHeight
                    // Window (crop) size for every position: the frame the glasses deliver, in sensor
                    // pixels, up to the chosen preset's size. The position says where this window sits.
                    val windowMaxW = presetResolution.sensorWidth
                    val windowMaxH = presetResolution.sensorHeight
                    CamDiscreteSliderRow(
                        label = "Window width (sensor px)",
                        value = winW,
                        values = M2AIFrameResolution.supportedSensorWidths.filter { it <= windowMaxW },
                        enabled = true,
                        onValueChangeFinished = { applyCustomResolution() },
                    ) {
                        customSensorWidth = it
                        customSensorHeight = winH
                        customDecimateX2 = selectedResolution.decimateX2
                        customResolutionEnabled = true
                    }
                    CamSliderRow(
                        label = "Window height (sensor px)",
                        value = winH,
                        range = M2AIFrameResolution.MIN_SIDE..windowMaxH,
                        enabled = true,
                        step = M2AIFrameResolution.HEIGHT_STEP,
                        onValueChangeFinished = { applyCustomResolution() },
                    ) {
                        customSensorWidth = winW
                        customSensorHeight = it
                        customDecimateX2 = selectedResolution.decimateX2
                        customResolutionEnabled = true
                    }
                    // What the SDK resolved the position to, after clamping and 4-px alignment.
                    val currentOptions = lab.options
                    Text(
                        "Sensor origin ${currentOptions.sensorOriginX},${currentOptions.sensorOriginY}",
                        color = Color.LightGray,
                        fontSize = 11.sp,
                    )

                            }
                            GROUP_EXPOSURE -> {
                    CamChipRow("Exposure", exposurePresets, exposurePreset) {
                        exposurePreset = it
                        updateConfig()
                    }
                    Column(Modifier.bringIntoViewRequester(exposureDetailRequester)) {
                    if (exposurePreset == EXACT) {
                        CamSliderRow("Exposure (lines)", exposureLines, M2Exposure.LINES_RANGE, true) {
                            exposureLines = it
                            updateConfig()
                        }
                        CamSliderRow("Gain register (x${gainRegister / 16f})", gainRegister, M2Exposure.GAIN_RANGE, true) {
                            gainRegister = it
                            updateConfig()
                        }
                    }
                    }

                            }
                            GROUP_QUALITY -> {
                    // A bandwidth cap exists only on a video stream (M2VideoOptions.TargetBandwidth);
                    // photographs always carry a quality.
                    if (selectedMode == 1) {
                        CamToggleRow("Bitrate mode (KiB/s)", bitrateMode, true) {
                            bitrateMode = it
                            updateConfig()
                        }
                    }
                    if (bitrateMode) {
                        CamChipRow("Bandwidth cap (KiB/s)", bandwidthPresets, bandwidthPreset) { name ->
                            bandwidthPreset = name
                            // A named cap seeds the slider, so Exact starts from the last preset.
                            M2Bandwidth.presets.firstOrNull { it.toString() == name }?.let { exactBandwidth = it.kibPerSec }
                            updateConfig()
                        }
                        if (bandwidthPreset == EXACT) {
                            Column(Modifier.bringIntoViewRequester(qualityDetailRequester)) {
                                CamSliderRow(
                                    "Bandwidth cap (${bandwidthRange.first}–${bandwidthRange.last} KiB/s)",
                                    exactBandwidth.coerceIn(bandwidthRange),
                                    bandwidthRange,
                                    true,
                                    onValueChangeFinished = { updateConfig() },
                                ) {
                                    exactBandwidth = it
                                }
                            }
                        }
                    } else {
                        CamChipRow("Quality", qualityPresets, qualityPreset) { name ->
                            qualityPreset = name
                            M2CompressionQuality.presets.firstOrNull { it.toString() == name }?.let { exactQuality = it.value }
                            updateConfig()
                        }
                        if (qualityPreset == EXACT) {
                            Column(Modifier.bringIntoViewRequester(qualityDetailRequester)) {
                                CamSliderRow(
                                    "Quality (${qualityRange.first}–${qualityRange.last})",
                                    exactQuality.coerceIn(qualityRange),
                                    qualityRange,
                                    true,
                                    onValueChangeFinished = { updateConfig() },
                                ) {
                                    exactQuality = it
                                }
                            }
                        }
                    }
                            }
                            GROUP_STREAM -> {
                    // Capture rate is a stream divider on the glasses; photographs have none.
                    if (selectedMode == 1) {
                        Spacer(Modifier.height(8.dp))
                        Text("Capture rate", color = Color.White, fontSize = 12.sp)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            M2CaptureRate.entries.reversed().forEach { rate ->
                                val isSelected = selectedRate == rate
                                Text(
                                    text = rate.name.removePrefix("UpTo"),
                                    modifier = Modifier
                                        .clickable {
                                            selectedRate = rate
                                            updateConfig()
                                        }
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                            RoundedCornerShape(4.dp)
                                        )
                                        .padding(8.dp),
                                    color = if (isSelected) Color.White else Color.Gray,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                            }
                            GROUP_CONTINUOUS -> {
                    // Auto-pause only exists on the continuous-snap config.
                    if (selectedMode == 2) {
                        CamToggleRow("Auto-pause when busy", autoPauseWhenBusy, true) {
                            autoPauseWhenBusy = it
                            updateConfig()
                        }
                    }

                            }
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                }
                // The rail: one button per settings group; tapping the open one closes it.
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    railGroups.forEach { (key, label) ->
                        val isOpen = openGroup == key
                        Text(
                            text = label,
                            modifier = Modifier
                                .clickable { openGroup = if (isOpen) null else key }
                                .background(
                                    if (isOpen) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.6f),
                                    RoundedCornerShape(6.dp)
                                )
                                .width(52.dp)
                                .padding(vertical = 10.dp),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            // Bottom Controls
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Mode Selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf("Snap", "Stream", "Cont.").forEachIndexed { index, title ->
                        Text(
                            text = title,
                            color = if (selectedMode == index) Color.Yellow else Color.White,
                            fontWeight = if (selectedMode == index) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier
                                .clickable(enabled = glassesReady) {
                                    selectedMode = index
                                    // Only a video stream has a bandwidth cap or a capture rate;
                                    // photographs fall back to the quality controls.
                                    if (index != 1) bitrateMode = false
                                    if ((openGroup == GROUP_STREAM && index != 1) || (openGroup == GROUP_CONTINUOUS && index != 2)) openGroup = null
                                    updateConfig()
                                }
                                .padding(8.dp)
                        )
                    }
                }

                // Shutter Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Stop Button for Snap Mode
                    AnimatedVisibility(
                        visible = isCameraRunning && selectedMode == 0,
                        modifier = Modifier.padding(end = 20.dp)
                    ) {
                        IconButton(
                            onClick = {
                                lab.stop()
                                isCameraRunning = false
                            },
                            enabled = glassesReady,
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                .border(2.dp, Color.White, CircleShape)
                        ) {
                            Icon(
                                Icons.Default.Close, 
                                contentDescription = "Stop Camera",
                                tint = Color.White
                            )
                        }
                    }

                    Box(contentAlignment = Alignment.Center) {
                        // Outer ring
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .border(4.dp, Color.White, CircleShape)
                        )
                        // Inner button
                        FilledIconButton(
                            onClick = {
                                if (isCameraRunning) {
                                    if (selectedMode == 0) {
                                        Evs.visionService.takePicture()
                                    } else {
                                        lab.stop()
                                        isCameraRunning = false
                                    }
                                } else {
                                    updateConfig()
                                    lab.start()
                                    isCameraRunning = true
                                }
                            },
                            enabled = glassesReady,
                            modifier = Modifier.size(60.dp),
                            shape = CircleShape,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = if (isCameraRunning && selectedMode != 0) Color.Red else Color.White,
                                disabledContainerColor = Color.DarkGray,
                            )
                        ) {
                            if (isCameraRunning && selectedMode != 0) {
                                Box(Modifier.size(24.dp).background(Color.White, RoundedCornerShape(4.dp))) // Stop icon style
                            }
                        }
                    }
                    
                    // Spacer to balance layout if needed
                     AnimatedVisibility(
                        visible = isCameraRunning && selectedMode == 0,
                        modifier = Modifier.padding(start = 20.dp)
                    ) {
                        Spacer(Modifier.size(48.dp))
                    }
                }
            }
        }
    }
}
