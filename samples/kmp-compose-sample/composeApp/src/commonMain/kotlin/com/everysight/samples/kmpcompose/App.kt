/*
 * Created by Everysight LTD.
 *
 * Compose Multiplatform UI for the Maverick AI public sample. The screen keeps
 * SDK setup, connection, HUD controls, streams, LOS demos, and preview controls
 * in one place so developers can trace each user action to the controller.
 */

package com.everysight.samples.kmpcompose

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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.everysight.mav2.sdk.Evs
import com.everysight.mav2.sdk.ui.components.glasses.EvsGlassesInfo
import com.everysight.mav2.sdk.ui.components.views.M2PreviewFloating
import com.everysight.samples.kmpcompose.generated.resources.Res
import com.everysight.samples.kmpcompose.generated.resources.everysight_footer
import com.everysight.samples.kmpcompose.generated.resources.everysight_header_dark
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

private object EsBrand {
    val Yellow = Color(0xFFEBEB70)
    val Yellow2 = Color(0xFFD7D161)
    val DarkBlue = Color(0xFF111723)
    val DarkBlue2 = Color(0xFF1C2233)
    val BlueGrey = Color(0xFF8B9DAE)
    val LightBlue = Color(0xFFA6BBCA)
    val White = Color.White
    val GoodGreen = Color(0xFF34F27A)
    val BadRed = Color(0xFFCF2F2F)
    val GlassBorder = Color(0x2BD2E2FF)
}

private val EverysightDarkColors = darkColorScheme(
    primary = EsBrand.Yellow,
    onPrimary = EsBrand.DarkBlue,
    secondary = EsBrand.LightBlue,
    onSecondary = EsBrand.DarkBlue,
    background = EsBrand.DarkBlue,
    onBackground = EsBrand.White,
    surface = EsBrand.DarkBlue2,
    onSurface = EsBrand.White,
    surfaceVariant = EsBrand.DarkBlue2,
    onSurfaceVariant = EsBrand.BlueGrey,
    outline = EsBrand.DarkBlue2
)

/**
 * Renders the shared sample UI.
 *
 * @param initSdk Initializes the platform SDK and installs platform resources.
 * @param stopSdk Stops the platform SDK and releases native resources.
 * @param ensureBlePermissionsThen Runs a user action after platform BLE permissions are granted.
 */
@Composable
fun App(
    initSdk: () -> Unit,
    ensureBlePermissionsThen: ((() -> Unit) -> Unit),
    resolvedDependencies: List<Pair<String, String>> = emptyList()
) {
    var uiState by remember { mutableStateOf(Mav2ComposeController.UiState()) }
    var showPreview by remember { mutableStateOf(false) }
    var previewSetupPending by remember { mutableStateOf(false) }
    var showVersionInfo by remember { mutableStateOf(false) }
    val appScope = rememberCoroutineScope()
    val controller = remember { Mav2ComposeController() }
    LaunchedEffect(controller) {
        controller.onStateChanged = { nextState -> uiState = nextState }
    }

    MaterialTheme(colorScheme = EverysightDarkColors) {
        val isSdkInitialized = Evs.wasInitialized()
        LaunchedEffect(isSdkInitialized) {
            if (!isSdkInitialized) {
                showPreview = false
                previewSetupPending = false
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(EsBrand.DarkBlue)
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                EsHeader(
                    subtitle = "Maverick AI · KMP Compose Sample",
                    onVersionInfoClick = { showVersionInfo = true }
                )
                EsStatusCard(uiState = uiState)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    EsToggleButton(
                        text = if (isSdkInitialized) "1) SDK initialized" else "1) Init SDK",
                        on = isSdkInitialized,
                        enabled = !isSdkInitialized,
                        onClick = {
                            if (!isSdkInitialized) {
                                initSdk()
                                controller.onInitCompleted()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    EsPreviewToggle(
                        checked = showPreview || previewSetupPending,
                        enabled = isSdkInitialized && !previewSetupPending,
                        onCheckedChange = { checked ->
                            if (checked) {
                                val addedScreen = controller.ensureScreenForPreview()
                                previewSetupPending = true
                                appScope.launch {
                                    if (addedScreen) delay(PREVIEW_SCREEN_SETUP_DELAY_MS)
                                    if (Evs.wasInitialized()) {
                                        Evs.screenService.enablePreview(true)
                                        showPreview = true
                                    }
                                    previewSetupPending = false
                                }
                            } else {
                                previewSetupPending = false
                                showPreview = false
                                if (Evs.wasInitialized()) Evs.screenService.enablePreview(false)
                            }
                        },
                        modifier = Modifier.weight(0.62f)
                    )
                }
                // SDK-provided glasses status card. The composable reads
                // from the SDK's `glassesViewModel`, which is a lateinit
                // property only populated by `Evs.init(...)` — compose it
                // before init and it crashes with
                // UninitializedPropertyAccessException. Gate on
                // `isSdkInitialized` so the card isn't in the tree until
                // the SDK is up.
                if (isSdkInitialized) {
                    EvsGlassesInfo(
                        modifier = Modifier.fillMaxWidth(),
                        isClickable = true,
                        containerColor = EsBrand.DarkBlue2,
                        contentColor = EsBrand.White
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    EsSecondaryButton(
                        text = "Configure",
                        onClick = { ensureBlePermissionsThen { controller.showConfigure() } },
                        modifier = Modifier.width(136.dp),
                        enabled = isSdkInitialized
                    )
                    EsSecondaryButton(
                        text = "Adjust",
                        onClick = { controller.showAdjust() },
                        modifier = Modifier.width(112.dp),
                        enabled = isSdkInitialized
                    )
                    EsSecondaryButton(
                        // Width must fit the longer "Disconnect" label on a
                        // single line — at 136.dp the labelLarge font wrapped
                        // into two lines on smaller phones. The action row
                        // already horizontal-scrolls, so a wider button never
                        // pushes the row off-screen.
                        text = if (uiState.isConnected || uiState.isReady) "Disconnect" else "Connect",
                        onClick = { controller.toggleConnect() },
                        modifier = Modifier.width(168.dp),
                        enabled = isSdkInitialized
                    )
                }
                EsCard {
                    PrimaryScrollableTabRow(
                        selectedTabIndex = uiState.selectedCategory.ordinal,
                        containerColor = Color.Transparent,
                        contentColor = EsBrand.Yellow,
                        edgePadding = 0.dp
                    ) {
                        SanityCategory.entries.forEach { category ->
                            Tab(
                                selected = uiState.selectedCategory == category,
                                onClick = { controller.selectCategory(category) },
                                selectedContentColor = EsBrand.Yellow,
                                unselectedContentColor = EsBrand.BlueGrey,
                                text = {
                                    Text(
                                        category.title,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                }
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    EsToggleButton(
                        text = if (uiState.isScreenAdded)
                            "Remove ${uiState.selectedCategory.title} HUD"
                        else
                            "Show ${uiState.selectedCategory.title} HUD",
                        on = uiState.isScreenAdded,
                        onClick = { controller.toggleScreen() },
                        enabled = isSdkInitialized
                    )
                    Spacer(Modifier.height(12.dp))
                    when (uiState.selectedCategory) {
                        SanityCategory.UIKitAnimators -> UikitPanel(uiState.shapeCount, isSdkInitialized) { controller.runUikitAction(it) }
                        SanityCategory.AudioAiVision -> AudioAiVisionPanel(uiState.streams, isSdkInitialized) { controller.runStreamAction(it) }
                        SanityCategory.Los -> LosPanel(
                            s = uiState.streams,
                            activeLosDemo = uiState.activeLosDemo,
                            enabled = isSdkInitialized,
                            onStreamAction = { controller.runStreamAction(it) },
                            onDemoAction = { controller.runLosDemoAction(it) },
                            onStopDemo = { controller.stopLosDemo() }
                        )
                        SanityCategory.Services -> ServicesPanel(uiState.services, isSdkInitialized) { controller.runServiceAction(it) }
                    }
                }
                EsFooter()
            }
            if (showPreview && isSdkInitialized) {
                M2PreviewFloating(
                    modifier = Modifier.fillMaxWidth(0.8f),
                    offsetFromTopFraction = 0.6f,
                    simTouch = true,
                    onClose = { showPreview = false }
                )
            }
            if (showVersionInfo) {
                EsVersionInfoDialog(
                    resolvedDependencies = resolvedDependencies,
                    onDismiss = { showVersionInfo = false }
                )
            }
        }
    }
}

@Composable
private fun EsVersionInfoDialog(
    resolvedDependencies: List<Pair<String, String>>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Resolved dependency info",
                color = EsBrand.White,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (resolvedDependencies.isEmpty()) {
                    EsKeyValueRow("status", "not available on this platform")
                } else {
                    Text(
                        text = "Values are generated from resolved runtime classpath at build time.",
                        color = EsBrand.BlueGrey,
                        style = MaterialTheme.typography.bodySmall
                    )
                    resolvedDependencies.forEach { (key, value) ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(EsBrand.DarkBlue.copy(alpha = 0.35f))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = key,
                                color = EsBrand.BlueGrey,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = value,
                                color = EsBrand.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.End,
                                modifier = Modifier.fillMaxWidth(),
                                fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = EsBrand.Yellow)
            }
        },
        containerColor = EsBrand.DarkBlue2
    )
}

private const val PREVIEW_SCREEN_SETUP_DELAY_MS = 650L

@Composable
private fun EsHeader(
    subtitle: String,
    onVersionInfoClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Image(
            painter = painterResource(Res.drawable.everysight_header_dark),
            contentDescription = "Everysight",
            modifier = Modifier.fillMaxWidth().heightIn(max = 56.dp),
            contentScale = ContentScale.Fit,
            alignment = Alignment.CenterStart
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = subtitle.uppercase(),
                color = EsBrand.BlueGrey,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 2.sp
                )
            )
            EsInfoCircleButton(onClick = onVersionInfoClick)
        }
    }
}

@Composable
private fun EsInfoCircleButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(RoundedCornerShape(50))
            .border(1.dp, EsBrand.GlassBorder, RoundedCornerShape(50))
            .background(EsBrand.DarkBlue2)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "i",
            color = EsBrand.Yellow,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
        )
    }
}

@Composable
private fun EsFooter() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(Res.drawable.everysight_footer),
            contentDescription = "Everysight footer",
            modifier = Modifier.heightIn(max = 22.dp),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
private fun EsStatusCard(uiState: Mav2ComposeController.UiState) {
    EsCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            EsStatusDot(connected = uiState.isReady || uiState.isConnected)
            Text(
                text = "status: ${uiState.sdkStatus}",
                color = EsBrand.White,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = "configured device: ${uiState.configuredLabel}",
            color = EsBrand.BlueGrey,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = "last action: ${uiState.lastAction}",
            color = EsBrand.LightBlue,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun EsStatusDot(connected: Boolean) {
    Box(
        modifier = Modifier
            .size(10.dp)
            .clip(RoundedCornerShape(50))
            .background(if (connected) EsBrand.GoodGreen else EsBrand.BadRed)
    )
}

@Composable
private fun EsCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(EsBrand.DarkBlue2.copy(alpha = 0.62f))
            .border(1.dp, EsBrand.GlassBorder, RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = { content() }
    )
}

@Composable
private fun EsPrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = EsBrand.Yellow,
            contentColor = EsBrand.DarkBlue
        )
    ) {
        Text(text, maxLines = 1, softWrap = false, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold))
    }
}

@Composable
private fun EsSecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = EsBrand.DarkBlue2,
            contentColor = EsBrand.White
        )
    ) {
        Text(text, maxLines = 1, softWrap = false, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun EsToggleButton(
    text: String,
    on: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    fillWidth: Boolean = true
) {
    if (on) {
        Button(
            onClick = onClick,
            modifier = if (fillWidth) modifier.fillMaxWidth() else modifier,
            enabled = enabled,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = EsBrand.Yellow,
                contentColor = EsBrand.DarkBlue,
                disabledContainerColor = EsBrand.Yellow,
                disabledContentColor = EsBrand.DarkBlue
            )
        ) {
            Text(text, maxLines = 1, softWrap = false, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold))
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = if (fillWidth) modifier.fillMaxWidth() else modifier,
            enabled = enabled,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = EsBrand.DarkBlue2,
                contentColor = EsBrand.White
            )
        ) {
            Text(text, maxLines = 1, softWrap = false, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun EsPreviewToggle(
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(EsBrand.DarkBlue2)
            .border(1.dp, EsBrand.GlassBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Checkbox(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange
        )
        Text(
            text = "Preview",
            color = if (enabled) EsBrand.White else EsBrand.BlueGrey,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
        )
    }
}

@Composable
private fun UikitPanel(shapeCount: Int, enabled: Boolean, onAction: (UikitAction) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        EsMutedLine("Add or remove drawables on the HUD screen")
        EsKeyValueRow("shapes on screen", "$shapeCount")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EsSecondaryButton("Rect", { onAction(UikitAction.AddRect) }, Modifier.weight(1f), enabled = enabled)
            EsSecondaryButton("Ellipse", { onAction(UikitAction.AddEllipse) }, Modifier.weight(1f), enabled = enabled)
            EsSecondaryButton("Line", { onAction(UikitAction.AddLine) }, Modifier.weight(1f), enabled = enabled)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EsSecondaryButton("Path", { onAction(UikitAction.AddPath) }, Modifier.weight(1f), enabled = enabled)
            EsSecondaryButton("Text", { onAction(UikitAction.AddText) }, Modifier.weight(1f), enabled = enabled)
            EsSecondaryButton("Image", { onAction(UikitAction.AddImage) }, Modifier.weight(1f), enabled = enabled)
        }
        EsSecondaryButton("Add Animated Rect", { onAction(UikitAction.AddAnimated) }, Modifier.fillMaxWidth(), enabled = enabled)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EsSecondaryButton("Remove Last", { onAction(UikitAction.RemoveLast) }, Modifier.weight(1f), enabled = enabled)
            EsSecondaryButton("Clear All", { onAction(UikitAction.ClearAll) }, Modifier.weight(1f), enabled = enabled)
        }
    }
}

@Composable
private fun AudioAiVisionPanel(s: Mav2ComposeController.StreamsState, enabled: Boolean, onAction: (StreamAction) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        EsMutedLine("Toggle microphone and AIVision streams, then watch live rates from the glasses")
        EsKeyValueRow("mic", if (s.micOn) "ON · ${formatBytes(s.micBytesPerSec)}/s" else "off")
        EsKeyValueRow("AIVision", if (s.aiVisionOn) "ON · ${formatBytes(s.aiVisionBytesPerSec)}/s · ${formatFps(s.aiVisionFps)} fps" else "off")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EsToggleButton(if (s.micOn) "Mic OFF" else "Mic ON", on = s.micOn, onClick = { onAction(StreamAction.ToggleMic) }, modifier = Modifier.weight(1f), enabled = enabled)
            EsToggleButton(if (s.aiVisionOn) "AIVision OFF" else "AIVision ON", on = s.aiVisionOn, onClick = { onAction(StreamAction.ToggleAiVision) }, modifier = Modifier.weight(1f), enabled = enabled)
        }
    }
}

@Composable
private fun LosPanel(
    s: Mav2ComposeController.StreamsState,
    activeLosDemo: LosDemoAction?,
    enabled: Boolean,
    onStreamAction: (StreamAction) -> Unit,
    onDemoAction: (LosDemoAction) -> Unit,
    onStopDemo: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        EsMutedLine("Toggle LOS sensors, arm touch input, or launch one of the LiveAI playground demos")
        EsKeyValueRow("LOS sensors", if (s.losOn) "ON · ${s.calib}" else "off")
        val ypr = if (s.losOn) "${formatDeg(s.yawDeg)} / ${formatDeg(s.pitchDeg)} / ${formatDeg(s.rollDeg)}" else "-- / -- / --"
        EsKeyValueRow("yaw/pitch/roll", ypr)
        EsKeyValueRow("touch", if (s.touchOn) "armed" else "off")
        EsKeyValueRow("last touch event", s.lastTouch)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EsToggleButton(if (s.losOn) "LOS OFF" else "LOS ON", on = s.losOn, onClick = { onStreamAction(StreamAction.ToggleLos) }, modifier = Modifier.weight(1f), enabled = enabled)
            EsToggleButton(if (s.touchOn) "Touch OFF" else "Touch ON", on = s.touchOn, onClick = { onStreamAction(StreamAction.ToggleTouch) }, modifier = Modifier.weight(1f), enabled = enabled)
        }
        val demoOnClick: (LosDemoAction) -> () -> Unit = { target ->
            { if (activeLosDemo == target) onStopDemo() else onDemoAction(target) }
        }
        // 3D Demo gets its own row so the toggle button can sit at full width
        // — it is the primary tap target. Remove Last / Clear All are the
        // companion controls and live on the row directly below.
        EsToggleButton(
            "3D Demo",
            on = activeLosDemo == LosDemoAction.Show3dDemo,
            onClick = demoOnClick(LosDemoAction.Show3dDemo),
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EsSecondaryButton("Remove Last", { onDemoAction(LosDemoAction.RemoveLast3dItem) }, Modifier.weight(1f), enabled = enabled)
            EsSecondaryButton("Clear All", { onDemoAction(LosDemoAction.ClearAll3dItems) }, Modifier.weight(1f), enabled = enabled)
        }
        EsToggleButton(
            "3D Pics Demo",
            on = activeLosDemo == LosDemoAction.Show3dPicturesDemo,
            onClick = demoOnClick(LosDemoAction.Show3dPicturesDemo),
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled
        )
    }
}

@Composable
private fun ServicesPanel(s: Mav2ComposeController.ServicesState, enabled: Boolean, onAction: (ServiceAction) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        EsMutedLine("Probe OTA and display services")
        EsKeyValueRow("brightness 0–255", "${s.brightness}")
        EsKeyValueRow("ota", s.otaSummary)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EsSecondaryButton("Brightness −", { onAction(ServiceAction.BrightnessDown) }, Modifier.weight(1f), enabled = enabled)
            EsSecondaryButton("Brightness +", { onAction(ServiceAction.BrightnessUp) }, Modifier.weight(1f), enabled = enabled)
        }
        EsSecondaryButton("OTA probe", { onAction(ServiceAction.OtaProbe) }, Modifier.fillMaxWidth(), enabled = enabled)
    }
}

@Composable
private fun EsKeyValueRow(key: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(key, color = EsBrand.BlueGrey, style = MaterialTheme.typography.bodyMedium)
        Text(
            value,
            color = EsBrand.White,
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
        )
    }
}

@Composable
private fun EsMutedLine(text: String) {
    Text(text, color = EsBrand.LightBlue, style = MaterialTheme.typography.bodyMedium)
}
