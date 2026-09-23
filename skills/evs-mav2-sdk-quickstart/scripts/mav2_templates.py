"""Created by Everysight LTD.

File templates for new_mav2_project.py. Kept separate so the generator stays readable.

Every template is a plain str.format-style template using {braces}; literal braces in
generated code are doubled. Values come from new_mav2_project.py's Context.
"""

# ── Android ──────────────────────────────────────────────────────────────────

ANDROID_SETTINGS_GRADLE = '''\
pluginManagement {{
    repositories {{
        google()
        mavenCentral()
        gradlePluginPortal()
    }}
}}

dependencyResolutionManagement {{
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {{
        google()
        mavenCentral()
        // Maverick AI SDK. GitHub Packages always requires a token, even for public
        // packages: put gpr.user / gpr.key in ~/.gradle/gradle.properties.
        maven {{
            url = uri("{android_maven_url}")
            credentials {{
                username = providers.gradleProperty("gpr.user").orNull
                password = providers.gradleProperty("gpr.key").orNull
            }}
        }}
    }}
}}

rootProject.name = "{project_slug}"
include(":app")
'''

ANDROID_ROOT_GRADLE = '''\
// Created by Everysight LTD.
plugins {{
    id("com.android.application") version "{agp_version}" apply false
    id("org.jetbrains.kotlin.android") version "{kotlin_version}" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "{kotlin_version}" apply false
}}
'''

ANDROID_GRADLE_PROPERTIES = '''\
org.gradle.jvmargs=-Xmx4g -Dfile.encoding=UTF-8
android.useAndroidX=true
kotlin.code.style=official

# Maverick AI SDK version. Override with -PsdkVersion=X.Y.Z or MAV2_SDK_VERSION.
sdkVersion={sdk_version}
'''

ANDROID_WRAPPER_PROPERTIES = '''\
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\\://services.gradle.org/distributions/gradle-{gradle_version}-bin.zip
networkTimeout=10000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
'''

ANDROID_APP_GRADLE = '''\
// Created by Everysight LTD.
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {{
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}}

val sdkVersion: String = providers.gradleProperty("sdkVersion")
    .orElse(providers.environmentVariable("MAV2_SDK_VERSION"))
    .orNull
    ?: error("sdkVersion is not set. Set it in gradle.properties, pass -PsdkVersion=X.Y.Z, or export MAV2_SDK_VERSION.")

android {{
    namespace = "{package_name}"
    compileSdk = {compile_sdk}

    defaultConfig {{
        applicationId = "{package_name}"
        minSdk = {min_sdk}
        targetSdk = {target_sdk}
        versionCode = 1
        versionName = "1.0"
    }}

    buildFeatures {{
        compose = true
    }}

    compileOptions {{
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }}
}}

kotlin {{
    compilerOptions {{
        jvmTarget.set(JvmTarget.JVM_17)
    }}
}}

// Fail the build with a clear message rather than discovering at connect time that no
// key was bundled. Evs.init() succeeds without a key - it is only needed to connect -
// so a missing key is otherwise easy to miss until authentication fails.
// Accepts either name the SDK looks for as a bundled resource.
val checkSdkKey = tasks.register("checkSdkKey") {{
    val assets = layout.projectDirectory.dir("src/main/assets")
    doLast {{
        val candidates = listOf("app.key", "sdk.key")
        val present = candidates.map {{ assets.file(it).asFile }}.filter {{ it.exists() }}
        if (present.isEmpty()) {{
            error("No Everysight key bundled. Put the key issued to you at " +
                  "app/src/main/assets/sdk.key (or app.key) - see README.md. Without it " +
                  "Evs.init() still succeeds, but connecting ends in AuthFailed.")
        }}
        present.firstOrNull {{ it.length() == 0L }}?.let {{
            error("app/src/main/assets/${{it.name}} is empty. Replace it with the key issued by Everysight.")
        }}
    }}
}}
tasks.named("preBuild") {{ dependsOn(checkSdkKey) }}

dependencies {{
    implementation(platform("androidx.compose:compose-bom:{compose_bom}"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:{activity_compose}")

    // Maverick AI SDK. The debug artifact carries the SDK's own debug UI/logging;
    // release builds must use the release artifact.
    debugImplementation("com.everysight.mav2:maverick-ai-sdk-android-debug:$sdkVersion")
    releaseImplementation("com.everysight.mav2:maverick-ai-sdk-android:$sdkVersion")
}}
'''

ANDROID_MANIFEST = '''\
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <!-- Legacy BLE permissions, needed on API <= 30 only. -->
    <uses-permission
        android:name="android.permission.BLUETOOTH"
        android:maxSdkVersion="30" />
    <uses-permission
        android:name="android.permission.BLUETOOTH_ADMIN"
        android:maxSdkVersion="30" />
    <uses-permission
        android:name="android.permission.ACCESS_FINE_LOCATION"
        android:maxSdkVersion="30" />

    <!-- API 31+ BLE permissions. neverForLocation avoids the location grant. -->
    <uses-permission
        android:name="android.permission.BLUETOOTH_SCAN"
        android:usesPermissionFlags="neverForLocation" />
    <uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />

    <application
        android:allowBackup="true"
        android:label="{app_label}"
        android:supportsRtl="true"
        android:theme="@android:style/Theme.Material.Light.NoActionBar">
        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
'''

ANDROID_MAIN_ACTIVITY = '''\
/*
 * Created by Everysight LTD.
 *
 * Minimal Maverick AI SDK hello world for Android.
 *
 * The flow is deliberately explicit and in one file:
 *   1. Evs.init(context)                       - start the SDK
 *   2. request BLE permissions                 - required before any scan/connect
 *   3. Evs.showAppUI(DefaultConfigure)            - SDK-provided pairing UI
 *   4. Evs.glassesService.connect()            - connect to the configured glasses
 *   5. Evs.screenService.addScreen(screen)     - draw a HUD on the glasses
 *
 * The HUD itself is a M2Screen subclass at the bottom of this file.
 */

package {package_name}

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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

private const val TAG = "{log_tag}"

class MainActivity : ComponentActivity() {{

    private data class UiState(
        val status: String = "not initialized",
        val device: String = "not configured",
        val connected: Boolean = false,
        val hudAdded: Boolean = false,
    )

    private var ui by mutableStateOf(UiState())
    private var hud: HelloHudScreen? = null
    private var listenerRegistered = false
    private var afterPermission: (() -> Unit)? = null

    private val blePermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {{ results ->
        val action = afterPermission
        afterPermission = null
        if (results.values.all {{ it }}) {{
            action?.invoke()
        }} else {{
            Toast.makeText(this, "BLE permissions are required.", Toast.LENGTH_LONG).show()
        }}
    }}

    /**
     * Connection state comes from the SDK, not from your own bookkeeping. Ready means
     * the glasses are connected *and* able to render - only then is addScreen useful.
     */
    private val connectionListener = object : IM2GlassesConnectionEvents {{
        override fun onConnectionStatusChanged(status: M2ConnectionStatus) {{
            Log.i(TAG, "connection status = $status")
            ui = ui.copy(
                status = status.name.lowercase(),
                connected = status == M2ConnectionStatus.Ready || status == M2ConnectionStatus.Connected,
            )
            refreshDevice()
        }}

        override fun onReady() {{
            Log.i(TAG, "glasses ready")
            ui = ui.copy(status = "ready", connected = true)
            refreshDevice()
            addHud()   // show something on the glasses as soon as they can render
        }}

        override fun onUnReady() {{
            Log.i(TAG, "glasses not ready")
            ui = ui.copy(status = "disconnected", connected = false, hudAdded = false)
        }}
    }}

    override fun onCreate(savedInstanceState: Bundle?) {{
        super.onCreate(savedInstanceState)
        setContent {{
            MaterialTheme {{
                Surface(modifier = Modifier.fillMaxSize()) {{
                    HelloScreen(
                        status = ui.status,
                        device = ui.device,
                        connected = ui.connected,
                        hudAdded = ui.hudAdded,
                        initialized = Evs.wasInitialized(),
                        onInit = ::initSdk,
                        onConfigure = ::configureGlasses,
                        onToggleConnect = ::toggleConnect,
                        onToggleHud = ::toggleHud,
                    )
                }}
            }}
        }}
    }}

    override fun onResume() {{
        super.onResume()
        if (Evs.wasInitialized()) refreshDevice()
    }}

    // 1) Start the SDK. Safe to call once; wasInitialized() tells you if it happened.
    private fun initSdk() {{
        Evs.init(applicationContext)
        registerListenerOnce()
        ui = ui.copy(status = "initialized")
        refreshDevice()
    }}

    // 2 + 3) BLE permissions, then the SDK's own pairing UI.
    private fun configureGlasses() = withBlePermissions {{
        Evs.glassesService.disconnect()
        Evs.showAppUI(M2AppUIOption.DefaultConfigure)
        ui = ui.copy(status = "configuring", connected = false, hudAdded = false)
    }}

    // 4) Connect / disconnect the configured glasses.
    private fun toggleConnect() {{
        if (ui.connected) {{
            Evs.glassesService.disconnect()
            ui = ui.copy(status = "disconnected", connected = false, hudAdded = false)
        }} else withBlePermissions {{
            registerListenerOnce()
            Evs.glassesService.connect()
            ui = ui.copy(status = "connecting")
        }}
    }}

    // 5) Add / remove the HUD.
    private fun toggleHud() {{
        if (ui.hudAdded) {{
            hud?.let {{ Evs.screenService.removeScreen(it) }}
            ui = ui.copy(hudAdded = false)
        }} else {{
            addHud()
        }}
    }}

    private fun addHud() {{
        if (!Evs.wasInitialized() || ui.hudAdded) return
        val screen = hud ?: HelloHudScreen().also {{ hud = it }}
        runCatching {{ Evs.screenService.addScreen(screen) }}
            .onSuccess {{ ui = ui.copy(hudAdded = true) }}
            .onFailure {{ Log.w(TAG, "addScreen failed", it) }}
    }}

    private fun registerListenerOnce() {{
        if (!listenerRegistered) {{
            Evs.glassesService.registerConnectionListener(connectionListener)
            listenerRegistered = true
        }}
    }}

    private fun refreshDevice() {{
        val name = Evs.glassesService.getDeviceName().trim()
        val address = Evs.glassesService.getDeviceAddress().trim()
        ui = ui.copy(device = name.ifEmpty {{ address }}.ifEmpty {{ "not configured" }})
    }}

    private fun withBlePermissions(action: () -> Unit) {{
        val needed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {{
            listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        }} else {{
            listOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }}
        val missing = needed.filter {{
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }}
        if (missing.isEmpty()) {{
            action()
        }} else {{
            afterPermission = action
            blePermissions.launch(missing.toTypedArray())
        }}
    }}
}}

@Composable
private fun HelloScreen(
    status: String,
    device: String,
    connected: Boolean,
    hudAdded: Boolean,
    initialized: Boolean,
    onInit: () -> Unit,
    onConfigure: () -> Unit,
    onToggleConnect: () -> Unit,
    onToggleHud: () -> Unit,
) {{
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {{
        Text("{app_label}", style = MaterialTheme.typography.headlineSmall)
        Text("status: $status")
        Text("device: $device")

        Button(onClick = onInit, enabled = !initialized, modifier = Modifier.fillMaxWidth()) {{
            Text(if (initialized) "1) SDK initialized" else "1) Init SDK")
        }}
        Button(onClick = onConfigure, enabled = initialized, modifier = Modifier.fillMaxWidth()) {{
            Text("2) Configure glasses")
        }}
        Button(onClick = onToggleConnect, enabled = initialized, modifier = Modifier.fillMaxWidth()) {{
            Text(if (connected) "3) Disconnect" else "3) Connect")
        }}
        Button(onClick = onToggleHud, enabled = initialized, modifier = Modifier.fillMaxWidth()) {{
            Text(if (hudAdded) "4) Remove HUD" else "4) Add HUD")
        }}
    }}
}}

/**
 * A HUD drawn on the glasses.
 *
 * M2Screen is a retained scene: you build the drawables once in onCreate() and then
 * mutate them (setText / setXY / setColor) instead of redrawing every frame.
 */
private class HelloHudScreen : M2Screen(width = 420f, height = 180f, tag = "hello-hud") {{
    private val background = M2RectFilled(M2Color.Black).apply {{
        setDimensions(0f, 0f, 420f, 180f)
    }}
    private val border = M2RectOutline(M2Color.Green).apply {{
        setDimensions(16f, 14f, 388f, 144f)
        setStyle(2f)
    }}
    private val title = M2Text("Hello, Maverick AI").apply {{
        setXY(32f, 60f)
        setColor(M2Color.White)
        setScale(1.2f)
    }}
    private val subtitle = M2Text("{app_label}").apply {{
        setXY(32f, 96f)
        setColor(M2Color.White)
        setScale(0.8f)
    }}

    override fun onCreate() {{
        add(background)
        add(border)
        add(title)
        add(subtitle)
    }}
}}
'''

ANDROID_KEY_PLACEHOLDER = '''\
Put the Everysight key issued to you in this directory, named:

    sdk.key          general SDK key, any glasses
    app.key          app-scoped key, if that is what you were issued

Full path from the Android project root:

    app/src/main/assets/sdk.key

The SDK reads it from the APK assets the first time a key is needed - which is when you
CONNECT, not when you init. Evs.init() succeeds without a key; the connection then ends
in M2ConnectionStatus.AuthFailed. On first connect the key is exchanged online for a
certificate tied to your app namespace and to that pair of glasses.

The key is a secret and is scoped to an application namespace: a key issued for another
app will not work. Do not commit it to a public repository.

Delete this file once the key is in place.
'''

# ── iOS ──────────────────────────────────────────────────────────────────────

IOS_APP_DELEGATE = '''\
// Created by Everysight LTD.

import UIKit

@main
final class AppDelegate: UIResponder, UIApplicationDelegate {{
    var window: UIWindow?

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {{
        let window = UIWindow(frame: UIScreen.main.bounds)
        window.rootViewController = ViewController()
        window.makeKeyAndVisible()
        self.window = window
        return true
    }}
}}
'''

IOS_VIEW_CONTROLLER = r'''/*
 * Created by Everysight LTD.
 *
 * Minimal Maverick AI SDK hello world for iOS (UIKit).
 *
 * The flow mirrors the Android app:
 *   1. Evs.shared.doInit()                            - start the SDK
 *   2. Evs.shared.showAppUI(DefaultConfigure)            - SDK-provided pairing UI
 *   3. Evs.shared.glassesService.connect()            - connect to configured glasses
 *   4. Evs.shared.screenService.addScreen(screen:)    - draw a HUD on the glasses
 *
 * iOS has no runtime BLE permission request to write: the NSBluetooth* strings in
 * Info.plist are enough and the system prompts on first use.
 *
 * Note the Kotlin/Native calling convention: the SDK is a Kotlin Multiplatform
 * framework, so setters are named `setColor(evsColor:)`, take `tag:` arguments, and
 * return the receiver - hence the `_ =` on each call.
 */

import UIKit
import MaverickAI

final class ViewController: UIViewController {{

    // MARK: - HUD

    /// A HUD drawn on the glasses.
    ///
    /// M2Screen is a retained scene: build the drawables once in onCreate() and then
    /// mutate them, rather than redrawing every frame.
    private final class HelloHudScreen: M2Screen {{
        init() {{
            super.init(width: 420, height: 180, tag: "hello-hud")
        }}

        override func onCreate() {{
            let background = M2RectFilled(color: M2Color.black, tag: nil)
            _ = background.setDimensions(x: 0, y: 0, width: 420, height: 180)

            let border = M2RectOutline(color: M2Color.green, tag: nil)
            _ = border.setDimensions(x: 16, y: 14, width: 388, height: 144)

            let title = M2Text(text: "Hello, Maverick AI", tag: nil)
            _ = title.setXY(x: 32, y: 60)
            _ = title.setColor(evsColor: M2Color.white)
            _ = title.setScale(scale: 1.2)

            let subtitle = M2Text(text: "{app_label}", tag: nil)
            _ = subtitle.setXY(x: 32, y: 96)
            _ = subtitle.setColor(evsColor: M2Color.white)
            _ = subtitle.setScale(scale: 0.8)

            _ = add(drawable: background)
            _ = add(drawable: border)
            _ = add(drawable: title)
            _ = add(drawable: subtitle)
        }}
    }}

    // MARK: - Connection events

    /// Must be an NSObject subclass, and must implement every member of the protocol -
    /// including onConfigureDevice - or Swift reports it as non-conforming.
    private final class ConnectionListener: NSObject, IM2GlassesConnectionEvents {{
        var onAnyChange: (() -> Void)?
        var onReadyToRender: (() -> Void)?

        func onConfigureDevice(address: String?, name: String?) {{
            print("configured device: name=\(name ?? "nil") address=\(address ?? "nil")")
            onAnyChange?()
        }}

        func onConnectionStatusChanged(status: M2ConnectionStatus) {{
            print("connection status: \(status)")
            onAnyChange?()
        }}

        func onReady() {{
            print("glasses ready")
            onAnyChange?()
            onReadyToRender?()
        }}

        func onUnReady() {{
            print("glasses not ready")
            onAnyChange?()
        }}
    }}

    // MARK: - State

    private let statusLabel = UILabel()
    private let deviceLabel = UILabel()
    private let initButton = UIButton(type: .system)
    private let configureButton = UIButton(type: .system)
    private let connectButton = UIButton(type: .system)
    private let hudButton = UIButton(type: .system)

    private let listener = ConnectionListener()
    private var listenerRegistered = false
    private var hud: HelloHudScreen?
    private var isHudAdded = false

    // MARK: - Lifecycle

    override func viewDidLoad() {{
        super.viewDidLoad()
        buildUI()

        // The listener fires on an SDK thread; UIKit work has to hop to main.
        listener.onAnyChange = {{ [weak self] in
            DispatchQueue.main.async {{ self?.render() }}
        }}
        listener.onReadyToRender = {{ [weak self] in
            DispatchQueue.main.async {{ self?.addHud() }}
        }}

        render()
    }}

    deinit {{
        if let hud, isHudAdded {{
            _ = Evs.shared.screenService.removeScreen(screen: hud)
        }}
    }}

    // MARK: - SDK flow

    // 1) Start the SDK. Everything else needs this first.
    @objc private func initSdk() {{
        guard !Evs.shared.wasInitialized() else {{ return }}
        Evs.shared.doInit()
        registerListenerOnce()
        render()
    }}

    // 2) The SDK's own pairing UI. Also where you pick the glasses simulator.
    @objc private func configureGlasses() {{
        Evs.shared.glassesService.disconnect()
        isHudAdded = false
        Evs.shared.showAppUI(option: M2AppUIOption.companion.DefaultConfigure)
        render()
    }}

    // 3) Connect / disconnect. Ask the SDK for state rather than tracking it yourself.
    @objc private func toggleConnect() {{
        if isConnectedOrReady() {{
            Evs.shared.glassesService.disconnect()
            isHudAdded = false
        }} else {{
            registerListenerOnce()
            Evs.shared.glassesService.connect()
        }}
        render()
    }}

    // 4) Add / remove the HUD.
    @objc private func toggleHud() {{
        if isHudAdded, let hud {{
            _ = Evs.shared.screenService.removeScreen(screen: hud)
            isHudAdded = false
            render()
        }} else {{
            addHud()
        }}
    }}

    private func addHud() {{
        guard Evs.shared.wasInitialized(), !isHudAdded else {{ return }}
        let screen = hud ?? HelloHudScreen()
        hud = screen
        _ = Evs.shared.screenService.addScreen(screen: screen)
        isHudAdded = true
        render()
    }}

    private func registerListenerOnce() {{
        guard !listenerRegistered else {{ return }}
        Evs.shared.glassesService.registerConnectionListener(listener: listener)
        listenerRegistered = true
    }}

    private func isConnectedOrReady() -> Bool {{
        guard Evs.shared.wasInitialized() else {{ return false }}
        return Evs.shared.glassesService.isReady() || Evs.shared.glassesService.isConnected()
    }}

    // MARK: - UI

    private func render() {{
        let initialized = Evs.shared.wasInitialized()
        let ready = initialized && Evs.shared.glassesService.isReady()
        let connected = isConnectedOrReady()

        if !initialized {{
            statusLabel.text = "status: not initialized"
        }} else if ready {{
            statusLabel.text = "status: ready"
        }} else if connected {{
            statusLabel.text = "status: connected"
        }} else {{
            statusLabel.text = "status: disconnected"
        }}

        var device = ""
        if initialized {{
            let name = Evs.shared.glassesService.getDeviceName()
                .trimmingCharacters(in: .whitespacesAndNewlines)
            let address = Evs.shared.glassesService.getDeviceAddress()
                .trimmingCharacters(in: .whitespacesAndNewlines)
            device = name.isEmpty ? address : name
        }}
        deviceLabel.text = "device: " + (device.isEmpty ? "not configured" : device)

        initButton.isEnabled = !initialized
        initButton.setTitle(initialized ? "1) SDK initialized" : "1) Init SDK", for: .normal)
        configureButton.isEnabled = initialized
        connectButton.isEnabled = initialized
        connectButton.setTitle(connected ? "3) Disconnect" : "3) Connect", for: .normal)
        hudButton.isEnabled = initialized
        hudButton.setTitle(isHudAdded ? "4) Remove HUD" : "4) Add HUD", for: .normal)
    }}

    private func buildUI() {{
        view.backgroundColor = .systemBackground

        let titleLabel = UILabel()
        titleLabel.text = "{app_label}"
        titleLabel.font = .preferredFont(forTextStyle: .title2)

        for label in [statusLabel, deviceLabel] {{
            label.font = .preferredFont(forTextStyle: .body)
            label.textColor = .secondaryLabel
        }}

        initButton.setTitle("1) Init SDK", for: .normal)
        initButton.addTarget(self, action: #selector(initSdk), for: .touchUpInside)
        configureButton.setTitle("2) Configure glasses", for: .normal)
        configureButton.addTarget(self, action: #selector(configureGlasses), for: .touchUpInside)
        connectButton.setTitle("3) Connect", for: .normal)
        connectButton.addTarget(self, action: #selector(toggleConnect), for: .touchUpInside)
        hudButton.setTitle("4) Add HUD", for: .normal)
        hudButton.addTarget(self, action: #selector(toggleHud), for: .touchUpInside)

        let stack = UIStackView(arrangedSubviews: [
            titleLabel, statusLabel, deviceLabel,
            initButton, configureButton, connectButton, hudButton,
        ])
        stack.axis = .vertical
        stack.spacing = 14
        stack.alignment = .fill
        stack.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(stack)

        NSLayoutConstraint.activate([
            stack.leadingAnchor.constraint(equalTo: view.safeAreaLayoutGuide.leadingAnchor, constant: 24),
            stack.trailingAnchor.constraint(equalTo: view.safeAreaLayoutGuide.trailingAnchor, constant: -24),
            stack.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor, constant: 32),
        ])
    }}
}}
'''

IOS_INFO_PLIST = '''\
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>CFBundleDevelopmentRegion</key>
    <string>en</string>
    <key>CFBundleDisplayName</key>
    <string>{app_label}</string>
    <key>CFBundleExecutable</key>
    <string>$(EXECUTABLE_NAME)</string>
    <key>CFBundleIdentifier</key>
    <string>$(PRODUCT_BUNDLE_IDENTIFIER)</string>
    <key>CFBundleInfoDictionaryVersion</key>
    <string>6.0</string>
    <key>CFBundleName</key>
    <string>$(PRODUCT_NAME)</string>
    <key>CFBundlePackageType</key>
    <string>APPL</string>
    <key>CFBundleShortVersionString</key>
    <string>1.0</string>
    <key>CFBundleVersion</key>
    <string>1</string>
    <key>LSRequiresIPhoneOS</key>
    <true/>
    <key>UILaunchScreen</key>
    <dict/>
    <key>UISupportedInterfaceOrientations</key>
    <array>
        <string>UIInterfaceOrientationPortrait</string>
    </array>

    <!-- Required by the SDK: the system shows these strings when it asks for
         Bluetooth access. Missing keys mean an immediate crash on first BLE use. -->
    <key>NSBluetoothAlwaysUsageDescription</key>
    <string>This app uses Bluetooth to connect to Everysight glasses.</string>
    <key>NSBluetoothPeripheralUsageDescription</key>
    <string>This app uses Bluetooth to connect to Everysight glasses.</string>
    <key>NSLocationWhenInUseUsageDescription</key>
    <string>Location is used while in use to enable Bluetooth device discovery.</string>
</dict>
</plist>
'''

IOS_KEY_PLACEHOLDER = '''\
Put the Everysight key issued to you in this directory, named:

    sdk.key

Full path from the iOS project root:

    {target_name}/sdk.key

It is already wired into the Xcode project as a bundle resource, so once the file is here
it ships inside the .app. The SDK reads it the first time a key is needed - which is when
you CONNECT, not when you init. doInit() succeeds without a key; the connection then ends
in M2ConnectionStatus.AuthFailed. On first connect the key is exchanged online for a
certificate tied to your bundle id and to that pair of glasses.

The Xcode project wires this resource by filename. If you were issued an app.key, either
rename it to sdk.key or add it to the target yourself - the SDK checks app.key first.

The key is a secret and is scoped to an application namespace: a key issued for another
app will not work. Do not commit it to a public repository.

Delete this file once the key is in place.
'''

# ── Compose Multiplatform (KMP) ──────────────────────────────────────────────
#
# One Gradle project with a shared composeApp module (Android + iOS), and an iosApp Xcode
# project that embeds the Kotlin framework. Mirrors sdk/samples/kmp-compose-sample, which
# the build system validates against every SDK release - read it when an API is in doubt.

_KMP_GPR_REPO = '''\
        // Maverick AI SDK. GitHub Packages always requires a
        // token, even for public packages: gpr.user / gpr.key in ~/.gradle/gradle.properties,
        // or GITHUB_TOKEN / GH_TOKEN (with GITHUB_ACTOR) in the environment.
        maven {{
            url = uri("{android_maven_url}")
            val token = providers.gradleProperty("gpr.key")
                .orElse(providers.environmentVariable("GITHUB_TOKEN"))
                .orElse(providers.environmentVariable("GH_TOKEN"))
            val user = providers.gradleProperty("gpr.user")
                .orElse(providers.environmentVariable("GITHUB_ACTOR"))
                .orElse(token.map {{ "x-access-token" }})
            if (token.isPresent) {{
                credentials {{
                    username = user.get()
                    password = token.get()
                }}
            }}
        }}'''

KMP_SETTINGS_GRADLE = '''\
pluginManagement {{
    repositories {{
        google()
        mavenCentral()
        gradlePluginPortal()
    }}
}}

dependencyResolutionManagement {{
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {{
        google()
        mavenCentral()
''' + _KMP_GPR_REPO + '''
    }}
}}

rootProject.name = "{project_slug}"
include(":composeApp")
'''

KMP_ROOT_GRADLE = '''\
// Created by Everysight LTD.
plugins {{
    id("com.android.application") version "{kmp_agp_version}" apply false
    id("org.jetbrains.kotlin.multiplatform") version "{kmp_kotlin_version}" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "{kmp_kotlin_version}" apply false
    id("org.jetbrains.compose") version "{compose_mp_version}" apply false
}}
'''

KMP_GRADLE_PROPERTIES = '''\
# The iOS target is linked by Kotlin/Native in its own daemon, which the Gradle heap below
# does not cover - give it a ceiling of its own, or a framework link can fail with
# OutOfMemoryError on a machine with plenty of memory free.
org.gradle.jvmargs=-Xmx6g -Dfile.encoding=UTF-8 -Dkotlin.daemon.jvm.options\\="-Xmx4096M"
android.useAndroidX=true
kotlin.code.style=official
# iOS targets only build on a Mac. On Windows / Linux, skip them quietly and build Android.
kotlin.native.ignoreDisabledTargets=true

# Maverick AI SDK version. Override with -PsdkVersion=X.Y.Z or MAV2_SDK_VERSION.
sdkVersion={sdk_version}
'''

KMP_APP_GRADLE = '''\
// Created by Everysight LTD.
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {{
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.application")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}}

val sdkVersion: String = providers.gradleProperty("sdkVersion")
    .orElse(providers.environmentVariable("MAV2_SDK_VERSION"))
    .orNull
    ?: error("sdkVersion is not set. Set it in gradle.properties, pass -PsdkVersion=X.Y.Z, or export MAV2_SDK_VERSION.")

kotlin {{
    androidTarget {{
        compilerOptions {{
            jvmTarget.set(JvmTarget.JVM_17)
        }}
    }}
    listOf(iosArm64(), iosSimulatorArm64()).forEach {{ target ->
        target.binaries.framework {{
            // The iOS app imports the shared code as `import ComposeApp`.
            baseName = "ComposeApp"
        }}
    }}

    sourceSets {{
        commonMain.dependencies {{
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation("com.everysight.mav2:maverick-ai-sdk:$sdkVersion")
        }}
        androidMain.dependencies {{
            implementation("androidx.activity:activity-compose:{activity_compose}")
        }}
    }}
}}

android {{
    namespace = "{package_name}"
    compileSdk = {compile_sdk}

    defaultConfig {{
        applicationId = "{package_name}"
        minSdk = {min_sdk}
        targetSdk = {target_sdk}
        versionCode = 1
        versionName = "1.0"
    }}

    buildFeatures {{
        compose = true
    }}

    compileOptions {{
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }}
}}

// Fail the Android build with a clear message rather than discovering at connect time that
// no key was bundled. Evs.init() succeeds without a key - it is only needed to connect.
// The iOS app has the same guard for free: its Xcode project lists sdk.key as a resource,
// so a missing file fails that build.
val checkSdkKey = tasks.register("checkSdkKey") {{
    val assets = layout.projectDirectory.dir("src/androidMain/assets")
    doLast {{
        val present = listOf("app.key", "sdk.key").map {{ assets.file(it).asFile }}.filter {{ it.exists() }}
        if (present.isEmpty()) {{
            error("No Everysight key bundled. Put the key issued to you at " +
                  "composeApp/src/androidMain/assets/sdk.key (or app.key) - see README.md. " +
                  "Without it Evs.init() still succeeds, but connecting ends in AuthFailed.")
        }}
        present.firstOrNull {{ it.length() == 0L }}?.let {{
            error("composeApp/src/androidMain/assets/${{it.name}} is empty. Replace it with the key issued by Everysight.")
        }}
    }}
}}
tasks.matching {{ it.name == "preBuild" }}.configureEach {{ dependsOn(checkSdkKey) }}
'''

KMP_ANDROID_MANIFEST = ANDROID_MANIFEST

KMP_MAIN_ACTIVITY = '''\
/*
 * Created by Everysight LTD.
 *
 * Android host. It owns the two things that are genuinely Android: starting the SDK with a
 * Context, and the runtime Bluetooth permissions. Everything else is in the shared App.kt.
 */

package {package_name}

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.everysight.mav2.sdk.Evs
import com.everysight.mav2.sdk.init

class MainActivity : ComponentActivity() {{
    private var afterPermission: (() -> Unit)? = null

    private val blePermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {{ results ->
        val action = afterPermission
        afterPermission = null
        if (results.values.all {{ it }}) {{
            action?.invoke()
        }} else {{
            Toast.makeText(this, "Bluetooth permissions are required to find the glasses.", Toast.LENGTH_LONG).show()
        }}
    }}

    override fun onCreate(savedInstanceState: Bundle?) {{
        super.onCreate(savedInstanceState)
        setContent {{
            App(
                initSdk = {{ Evs.init(applicationContext) }},
                withBlePermissions = ::withBlePermissions,
            )
        }}
    }}

    // Android 12+ fails a scan without BLUETOOTH_SCAN, and silently: it simply finds nothing.
    // So ask before any action that touches the radio.
    private fun withBlePermissions(action: () -> Unit) {{
        val needed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {{
            listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        }} else {{
            listOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }}
        val missing = needed.filter {{
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }}
        if (missing.isEmpty()) {{
            action()
        }} else {{
            afterPermission = action
            blePermissions.launch(missing.toTypedArray())
        }}
    }}
}}
'''

KMP_MAIN_VIEW_CONTROLLER = '''\
/*
 * Created by Everysight LTD.
 *
 * iOS host: returns the view controller the Swift app shows. iOS has no runtime permission
 * call to make - the system asks for Bluetooth on first use, from the Info.plist strings.
 */

package {package_name}

import androidx.compose.ui.window.ComposeUIViewController
import com.everysight.mav2.sdk.Evs
import com.everysight.mav2.sdk.init
import platform.UIKit.UIViewController

@Suppress("FunctionName", "unused")   // called from Swift as MainViewControllerKt.MainViewController()
fun MainViewController(): UIViewController = ComposeUIViewController {{
    App(
        initSdk = {{ Evs.init() }},
        withBlePermissions = {{ action -> action() }},
    )
}}
'''

KMP_APP = '''\
/*
 * Created by Everysight LTD.
 *
 * {app_label} - a Maverick AI SDK hello world, shared by Android and iOS.
 *
 * The flow is deliberately explicit and in one file:
 *   1. initSdk()                                  - start the SDK (platform-specific, see the hosts)
 *   2. Evs.showAppUI(DefaultConfigure)            - the SDK's own pairing UI
 *   3. Evs.glassesService.connect()               - connect to the configured glasses
 *   4. Evs.screenService.addScreen(screen)        - draw a HUD on the glasses
 *
 * The HUD itself is the M2Screen subclass at the bottom of this file.
 */

package {package_name}

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.everysight.mav2.sdk.Evs
import com.everysight.mav2.sdk.services.IM2GlassesConnectionEvents
import com.everysight.mav2.sdk.uikit.data.M2AppUIOption
import com.everysight.mav2.sdk.uikit.data.M2ConnectionStatus
import com.everysight.mav2.sdk.uikit.drawables.M2RectOutline
import com.everysight.mav2.sdk.uikit.drawables.M2Text
import com.everysight.mav2.sdk.uikit.drawables.ext.setDimensions
import com.everysight.mav2.sdk.uikit.screens.M2Screen
import com.everysight.mav2.sdk.utils.M2Color

/**
 * The whole app.
 *
 * @param initSdk            starts the SDK; Android passes a Context, iOS does not.
 * @param withBlePermissions runs an action once Bluetooth is permitted (Android asks, iOS runs it).
 */
@Composable
fun App(
    initSdk: () -> Unit,
    withBlePermissions: (() -> Unit) -> Unit,
) {{
    var initialized by remember {{ mutableStateOf(Evs.wasInitialized()) }}
    var status by remember {{ mutableStateOf(if (initialized) "initialized" else "not initialized") }}
    var ready by remember {{ mutableStateOf(false) }}
    var hudAdded by remember {{ mutableStateOf(false) }}
    // Created on first use: SDK objects (screens, drawables) need Evs.init() to have run.
    val hud = remember {{ lazy {{ HelloHudScreen() }} }}

    fun addHud() {{
        if (hudAdded) return
        runCatching {{ Evs.screenService.addScreen(hud.value) }}.onSuccess {{ hudAdded = true }}
    }}

    // Connection state comes from the SDK. Ready means connected *and* able to render -
    // only then is addScreen useful, and anything drawn earlier does not survive.
    val connectionListener = remember {{
        object : IM2GlassesConnectionEvents {{
            override fun onConnectionStatusChanged(status: M2ConnectionStatus) {{
                status.name.lowercase().also {{ s -> if (!ready) setStatus(s) }}
            }}
            override fun onReady() {{
                ready = true
                setStatus("ready")
                addHud()   // show something on the glasses as soon as they can render
            }}
            override fun onUnReady() {{
                ready = false
                hudAdded = false
                setStatus("disconnected")
            }}
            private fun setStatus(s: String) {{ status = s }}
        }}
    }}

    DisposableEffect(initialized) {{
        if (initialized) Evs.glassesService.registerConnectionListener(connectionListener)
        onDispose {{ if (initialized) Evs.glassesService.unregisterConnectionListener(connectionListener) }}
    }}

    MaterialTheme {{
        Surface(modifier = Modifier.fillMaxSize()) {{
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {{
                Text("{app_label}", style = MaterialTheme.typography.headlineSmall)
                Text("status: $status")

                // 1) Start the SDK. Nothing else works before this.
                Button(
                    onClick = {{ initSdk(); initialized = true; status = "initialized" }},
                    enabled = !initialized,
                    modifier = Modifier.fillMaxWidth(),
                ) {{ Text(if (initialized) "SDK initialized" else "Init SDK") }}

                // 2) The SDK's own pairing screen - also where you pick the glasses simulator.
                Button(
                    onClick = {{ withBlePermissions {{ Evs.showAppUI(M2AppUIOption.DefaultConfigure) }} }},
                    enabled = initialized,
                    modifier = Modifier.fillMaxWidth(),
                ) {{ Text("Configure glasses") }}

                // 3) Connect / disconnect the configured glasses.
                Button(
                    onClick = {{
                        if (ready) Evs.glassesService.disconnect()
                        else withBlePermissions {{ Evs.glassesService.connect(); status = "connecting" }}
                    }},
                    enabled = initialized,
                    modifier = Modifier.fillMaxWidth(),
                ) {{ Text(if (ready) "Disconnect" else "Connect") }}

                // 4) Add / remove the HUD.
                Button(
                    onClick = {{
                        if (hudAdded) {{ Evs.screenService.removeScreen(hud.value); hudAdded = false }} else addHud()
                    }},
                    enabled = ready,
                    modifier = Modifier.fillMaxWidth(),
                ) {{ Text(if (hudAdded) "Remove HUD" else "Add HUD") }}
            }}
        }}
    }}
}}

/**
 * A HUD drawn on the glasses.
 *
 * M2Screen is a retained scene: build the drawables once in onCreate() and then mutate them
 * (setText / setXY / setColor) rather than redrawing every frame. The display is additive and
 * see-through - black is simply "no light" - so there is no background to fill.
 */
private class HelloHudScreen : M2Screen(width = 420f, height = 180f, tag = "hello-hud") {{
    override fun onCreate() {{
        add(M2RectOutline(M2Color.Green).apply {{
            setDimensions(16f, 14f, 388f, 144f)
            setStyle(2f)
        }})
        add(M2Text("Hello, Maverick AI").apply {{
            setXY(32f, 60f)
            setColor(M2Color.White)
            setScale(1.2f)
        }})
        add(M2Text("{app_label}").apply {{
            setXY(32f, 96f)
            setColor(M2Color.White)
            setScale(0.8f)
        }})
    }}
}}
'''

KMP_IOS_APP = '''\
// Created by Everysight LTD.
//
// SwiftUI host for the shared Compose UI. All the SDK code lives in composeApp.

import SwiftUI
import UIKit
import ComposeApp

struct ComposeView: UIViewControllerRepresentable {{
    func makeUIViewController(context: Context) -> UIViewController {{
        MainViewControllerKt.MainViewController()
    }}

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {{}}
}}

@main
struct {target_name}App: App {{
    var body: some Scene {{
        WindowGroup {{
            ComposeView().ignoresSafeArea()
        }}
    }}
}}
'''

KMP_ANDROID_KEY_PLACEHOLDER = ANDROID_KEY_PLACEHOLDER.replace(
    "app/src/main/assets/sdk.key", "composeApp/src/androidMain/assets/sdk.key"
).replace("the Android project root", "the kmp/ project root")

# Compose Multiplatform refuses to start on iOS without CADisableMinimumFrameDurationOnPhone
# (its plist sanity check throws on the first frame), so the KMP app gets its own plist.
KMP_IOS_INFO_PLIST = IOS_INFO_PLIST.replace(
    '''    <key>LSRequiresIPhoneOS</key>''',
    '''    <!-- Required by Compose Multiplatform: without it the app stops at launch. -->
    <key>CADisableMinimumFrameDurationOnPhone</key>
    <true/>
    <key>LSRequiresIPhoneOS</key>''')
assert KMP_IOS_INFO_PLIST != IOS_INFO_PLIST
