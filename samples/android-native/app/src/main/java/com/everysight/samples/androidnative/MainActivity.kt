/*
 * Created by Everysight LTD.
 *
 * ┌──────────────────────────────────────────────────────────────────────────┐
 * │  This sample is the native Android PROJECT CONFIGURATION, and no more.   │
 * │                                                                          │
 * │  It shows how to get the SDK into a plain Android app and reach the      │
 * │  glasses: the Maven dependency, the API key, the resources resolver,     │
 * │  init, runtime BLE permissions, configure, connect, and one HUD screen   │
 * │  to prove the link works.                                                │
 * │                                                                          │
 * │  For what the SDK can DRAW and DO — video, gradients, accelerating       │
 * │  animators, text effects, audio, the eye tracker — read                  │
 * │  `kmp-compose-sample`. That is the one full app, and every feature is    │
 * │  demonstrated there once rather than in three places.                    │
 * └──────────────────────────────────────────────────────────────────────────┘
 *
 * The lifecycle below, in order: initialize, request BLE permissions, configure,
 * connect, add a HUD screen, disconnect cleanly.
 */

package com.everysight.samples.androidnative

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.everysight.mav2.sdk.Evs
import com.everysight.mav2.sdk.init
import com.everysight.mav2.sdk.services.IM2GlassesConnectionEvents
import com.everysight.mav2.sdk.uikit.data.M2ConnectionStatus
import com.everysight.mav2.sdk.uikit.data.M2AppUIOption
import com.everysight.mav2.sdk.uikit.drawables.M2RectFilled
import com.everysight.mav2.sdk.uikit.drawables.M2RectOutline
import com.everysight.mav2.sdk.uikit.drawables.M2Text
import com.everysight.mav2.sdk.uikit.drawables.ext.setDimensions
import com.everysight.mav2.sdk.uikit.screens.M2Screen
import com.everysight.mav2.sdk.utils.M2Color

private object EsBrand {
    val Yellow = Color(0xFFEBEB70)
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
    background = EsBrand.DarkBlue,
    onBackground = EsBrand.White,
    surface = EsBrand.DarkBlue2,
    onSurface = EsBrand.White,
    onSurfaceVariant = EsBrand.BlueGrey
)

/** Main Android activity for the native SDK sample. */
class MainActivity : ComponentActivity() {
    data class UiState(
        val sdkStatus: String = "sdk not ready",
        val configuredLabel: String = "not configured",
        val isConnected: Boolean = false,
        val isReady: Boolean = false,
        val isScreenAdded: Boolean = false
    )

    private var listenerRegistered = false
    private var pendingAfterPermission: (() -> Unit)? = null
    private var hudScreen: SimpleHudScreen? = null
    private var uiState by mutableStateOf(UiState())

    private val blePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grantResults ->
        val granted = grantResults.values.all { it }
        val action = pendingAfterPermission
        pendingAfterPermission = null
        if (granted) {
            action?.invoke()
        } else {
            Toast.makeText(this, "BLE permissions required.", Toast.LENGTH_SHORT).show()
        }
    }

    private val connectionListener = object : IM2GlassesConnectionEvents {
        override fun onConnectionStatusChanged(status: M2ConnectionStatus) {
            Log.i("Mav2Sample", "status=$status")
            val statusLabel = when (status) {
                M2ConnectionStatus.Ready -> "ready"
                M2ConnectionStatus.Connected -> "connected"
                M2ConnectionStatus.Connecting -> if (uiState.isReady) "ready" else "connecting"
                M2ConnectionStatus.Disconnected, M2ConnectionStatus.BluetoothOff, M2ConnectionStatus.Failed, M2ConnectionStatus.AuthFailed -> "disconnected"
            }
            val isConnected = when (status) {
                M2ConnectionStatus.Ready -> true
                M2ConnectionStatus.Connected -> true
                M2ConnectionStatus.Connecting -> false
                M2ConnectionStatus.Disconnected, M2ConnectionStatus.BluetoothOff, M2ConnectionStatus.Failed, M2ConnectionStatus.AuthFailed -> false
            }
            uiState = uiState.copy(
                sdkStatus = statusLabel,
                isConnected = isConnected,
                isReady = M2ConnectionStatus.Ready == status
            )
            refreshConfiguredStateIfInitialized()
        }

        override fun onReady() {
            Log.i("Mav2Sample", "Glasses ready")
            uiState = uiState.copy(sdkStatus = "ready", isConnected = true, isReady = true)
            refreshConfiguredStateIfInitialized()
            // Auto-add the sample HUD as soon as the glasses are ready — this
            // is the same screen the "Add Screen" button would attach, so the
            // user sees the sample HUD immediately on first connect without
            // an extra tap. The button still toggles it off / on after this.
            autoAddSampleHudOnConnect()
        }

        override fun onUnReady() {
            Log.i("Mav2Sample", "Glasses unready/disconnected")
            uiState = uiState.copy(sdkStatus = "disconnected", isConnected = false, isReady = false)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ask at startup, not only when a button first needs the radio. A denied BLUETOOTH_SCAN
        // does not raise anything - the scan simply comes back empty - so a user who never saw
        // the prompt concludes the glasses are broken. The per-action gates below stay, as the
        // backstop for a permission revoked while the app is running.
        ensureBlePermissionsThen { }
        setContent {
            MaterialTheme(colorScheme = EverysightDarkColors) {
                Surface(modifier = Modifier.fillMaxSize(), color = EsBrand.DarkBlue) {
                    SampleScreen(
                        uiState = uiState,
                        onInit = {
                            initSdk()
                            ensureConnectionListenerRegistered()
                            uiState = uiState.copy(sdkStatus = "init ok")
                            refreshConfiguredState()
                        },
                        onConfigure = {
                            ensureBlePermissionsThen {
                                Evs.glassesService.disconnect()
                                Evs.showAppUI(M2AppUIOption.DefaultConfigure)
                                uiState = uiState.copy(sdkStatus = "disconnected", isConnected = false, isReady = false)
                                refreshConfiguredState()
                            }
                        },
                        onShowAdjust = { Evs.showAppUI(M2AppUIOption.DefaultAdjust) },
                        onToggleConnect = ::toggleConnect,
                        onToggleHud = ::toggleHud,
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshConfiguredStateIfInitialized()
    }

    private fun initSdk() {
        Evs.init(applicationContext)
    }

    private fun connectGlasses() {
        ensureConnectionListenerRegistered()
        Evs.glassesService.connect()
        uiState = uiState.copy(sdkStatus = "connecting")
    }

    private fun toggleConnect() {
        if (uiState.isConnected || uiState.isReady) {
            Evs.glassesService.disconnect()
            uiState = uiState.copy(sdkStatus = "disconnected", isConnected = false, isReady = false)
        } else {
            // Connecting reaches the radio just like scanning does, so it needs the same
            // permissions. Skipping this gate is silent on Android 12+: with BLUETOOTH_SCAN
            // denied there is no prompt and no error, only a scan that finds nothing.
            ensureBlePermissionsThen { connectGlasses() }
        }
    }

    private fun toggleHud() {
        if (uiState.isScreenAdded) {
            hudScreen?.let { Evs.screenService.removeScreen(it) }
            uiState = uiState.copy(isScreenAdded = false)
            return
        }
        val screen = hudScreen ?: SimpleHudScreen().also { hudScreen = it }
        Evs.screenService.addScreen(screen)
        uiState = uiState.copy(isScreenAdded = true)
    }

    private fun ensureConnectionListenerRegistered() {
        if (!listenerRegistered) {
            Evs.glassesService.registerConnectionListener(connectionListener)
            listenerRegistered = true
        }
    }

    /**
     * Auto-attaches the SimpleHudScreen (the same screen `toggleHud()` adds)
     * the first time the glasses report Ready, so a freshly-connected
     * operator sees the sample HUD without needing to press Add Screen.
     * Idempotent: re-Ready callbacks (eg. after a transient drop) do not
     * stack another copy of the screen.
     */
    private fun autoAddSampleHudOnConnect() {
        if (!Evs.wasInitialized() || uiState.isScreenAdded) return
        val screen = hudScreen ?: SimpleHudScreen().also { hudScreen = it }
        runCatching { Evs.screenService.addScreen(screen) }
            .onSuccess { uiState = uiState.copy(isScreenAdded = true) }
    }

    private fun refreshConfiguredState() {
        val name = Evs.glassesService.getDeviceName().trim()
        val address = Evs.glassesService.getDeviceAddress().trim()
        val configuredName = name.ifEmpty { address }
        uiState = uiState.copy(
            configuredLabel = configuredName.ifEmpty { "not configured" }
        )
    }

    private fun refreshConfiguredStateIfInitialized() {
        if (Evs.wasInitialized()) {
            refreshConfiguredState()
        }
    }

    private fun ensureBlePermissionsThen(action: () -> Unit) {
        val missing = requiredBlePermissions().filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) { action(); return }
        pendingAfterPermission = action
        blePermissionLauncher.launch(missing.toTypedArray())
    }

    private fun requiredBlePermissions(): List<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            listOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }
}

@Composable
private fun SampleScreen(
    uiState: MainActivity.UiState,
    onInit: () -> Unit,
    onConfigure: () -> Unit,
    onShowAdjust: () -> Unit,
    onToggleConnect: () -> Unit,
    onToggleHud: () -> Unit,
) {
    val isSdkInitialized = Evs.wasInitialized()
    // The background fills the whole window, including behind the status and navigation bars,
    // and safeDrawing then insets the CONTENT. Doing it the other way round leaves unpainted
    // strips at the edges; doing neither - which is what this sample did - put the header under
    // the status bar, because targetSdk 36 draws edge-to-edge and there is no opting out.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(EsBrand.DarkBlue)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            EsHeader(subtitle = "Maverick AI · Android Native Sample")
            EsStatusCard(uiState)
            EsToggleButton(
                if (isSdkInitialized) "SDK initialized" else "Init SDK",
                on = isSdkInitialized,
                enabled = !isSdkInitialized,
                onClick = onInit
            )
            EsGlassCard {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EsSecondaryButton("Configure", onConfigure, Modifier.weight(1f), enabled = isSdkInitialized)
                    EsSecondaryButton("Adjust", onShowAdjust, Modifier.weight(1f), enabled = isSdkInitialized)
                }
                EsSecondaryButton(if (uiState.isConnected || uiState.isReady) "Disconnect" else "Connect", onToggleConnect, enabled = isSdkInitialized)
                EsToggleButton(if (uiState.isScreenAdded) "Remove Screen" else "Add Screen", on = uiState.isScreenAdded, onClick = onToggleHud, enabled = isSdkInitialized)
            }
            Spacer(Modifier.height(8.dp))
            EsFooter()
        }
    }
}

@Composable
private fun EsHeader(subtitle: String) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Image(
            painter = painterResource(R.drawable.everysight_header),
            contentDescription = "Everysight",
            modifier = Modifier.fillMaxWidth().heightIn(max = 56.dp),
            contentScale = ContentScale.Fit,
            alignment = Alignment.CenterStart
        )
        Text(
            text = subtitle.uppercase(),
            color = EsBrand.BlueGrey,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.sp
            )
        )
    }
}

@Composable
private fun EsFooter() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(R.drawable.everysight_footer),
            contentDescription = "Everysight footer",
            modifier = Modifier.heightIn(max = 28.dp),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
private fun EsStatusCard(uiState: MainActivity.UiState) {
    EsGlassCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            EsStatusDot(connected = uiState.isReady || uiState.isConnected)
            Text(
                text = "status: ${uiState.sdkStatus}",
                color = EsBrand.White,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
        }
        Text(
            text = "configured device: ${uiState.configuredLabel}",
            color = EsBrand.BlueGrey,
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
private fun EsGlassCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(EsBrand.DarkBlue2.copy(alpha = 0.62f))
            .border(1.dp, EsBrand.GlassBorder, RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content
    )
}

@Composable
private fun EsPrimaryButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = EsBrand.Yellow,
            contentColor = EsBrand.DarkBlue,
            disabledContainerColor = EsBrand.Yellow,
            disabledContentColor = EsBrand.DarkBlue
        )
    ) {
        Text(label, maxLines = 1, softWrap = false, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold))
    }
}

@Composable
private fun EsSecondaryButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = EsBrand.DarkBlue2.copy(alpha = 0.64f),
            contentColor = EsBrand.White
        )
    ) {
        Text(label, maxLines = 1, softWrap = false, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun EsToggleButton(
    label: String,
    on: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    if (on) EsPrimaryButton(label, onClick, modifier, enabled)
    else EsSecondaryButton(label, onClick, modifier, enabled)
}

/** Simple glasses HUD rendered by the sample when the user taps Add Screen. */
private class SimpleHudScreen : M2Screen(width = 420f, height = 180f, tag = "sample-hud-screen") {
    private val background = M2RectFilled(M2Color.Black).apply {
        setDimensions(0f, 0f, 420f, 180f)
    }
    private val title = M2Text("MAV2 SDK Sample HUD").apply {
        setXY(24f, 24f)
        setColor(M2Color.White)
        setScale(1.1f)
    }
    private val subtitle = M2Text("Regular M2Screen (not full screen)").apply {
        setXY(24f, 54f)
        setColor(M2Color.White)
        setScale(0.82f)
    }
    private val box = M2RectOutline(M2Color.Green).apply {
        setDimensions(16f, 14f, 388f, 144f)
        setStyle(2f)
    }

    override fun onCreate() {
        add(background)
        add(box)
        add(title)
        add(subtitle)
    }
}
