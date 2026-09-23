/*
 * Created by Everysight LTD.
 *
 * ┌──────────────────────────────────────────────────────────────────────────┐
 * │  This sample is the native iOS PROJECT CONFIGURATION, and nothing more.  │
 * │                                                                          │
 * │  It shows how to get the SDK into a plain UIKit app and reach the        │
 * │  glasses: the SPM dependency, the API key, init, permissions, connect,   │
 * │  and one HUD screen to prove the link works.                             │
 * │                                                                          │
 * │  For what the SDK can DRAW and DO — video, gradients, accelerating       │
 * │  animators, text effects, audio, the eye tracker — read                  │
 * │  `kmp-compose-sample`. That is the one full app, and every feature is    │
 * │  demonstrated there once rather than in three places.                    │
 * │                                                                          │
 * │  The SDK is the same Kotlin Multiplatform binary either way. Nothing     │
 * │  here is iOS-specific except the UIKit around it.                        │
 * └──────────────────────────────────────────────────────────────────────────┘
 *
 * The things a host app must get right, in order:
 *
 *   1. Package.swift / Xcode  — the MaverickAI XCFramework via SPM
 *   2. sdk.key                — in the app bundle, next to your own resources
 *   3. Evs.shared.doInit()    — once, before touching any service
 *   4. Info.plist             — NSBluetoothAlwaysUsageDescription, or connecting fails
 *   5. configure, then connect
 */

import UIKit
#if canImport(MaverickAI)
import MaverickAI
#endif

/// Native iOS configuration reference for the Maverick AI SDK.
final class ViewController: UIViewController {
#if canImport(MaverickAI)

    /**
     * Connection callbacks.
     *
     * These arrive on the SDK's own thread, so anything touching UIKit hops to the main
     * queue — see where these are wired in `viewDidLoad`.
     */
    private final class ConnectionListener: NSObject, IM2GlassesConnectionEvents {
        var onStatus: ((M2ConnectionStatus) -> Void)?
        var onReadyChanged: ((Bool) -> Void)?

        func onConfigureDevice(address: String?, name: String?) {
            print("configured device address=\(address ?? "nil") name=\(name ?? "nil")")
        }

        func onConnectionStatusChanged(status: M2ConnectionStatus) {
            onStatus?(status)
        }

        /// Ready means every SDK service is up. This, not `connected`, is when to draw.
        func onReady() { onReadyChanged?(true) }

        func onUnReady() { onReadyChanged?(false) }
    }

    /**
     * The smallest useful HUD: a box and two lines of text.
     *
     * A screen is a coordinate space on the glasses, not a window on the phone. Drawables
     * are added in `onCreate` and the SDK owns them from there.
     */
    private final class HudScreen: M2Screen {
        init() {
            super.init(width: 420, height: 180, tag: "sample-hud-screen")
        }

        override func onCreate() {
            let title = M2Text(text: "MAVERICK AI", tag: nil)
            _ = title.setXY(x: 24, y: 24)
            _ = title.setColor(evsColor: M2Color.white)
            _ = title.setScale(scale: 1.1)

            let subtitle = M2Text(text: "iOS native — configuration sample", tag: nil)
            _ = subtitle.setXY(x: 24, y: 54)
            _ = subtitle.setColor(evsColor: M2Color.white)
            _ = subtitle.setScale(scale: 0.82)

            let box = M2RectOutline(color: M2Color.green, tag: nil)
            _ = box.setDimensions(x: 16, y: 14, width: 388, height: 144)

            _ = add(drawable: box)
            _ = add(drawable: title)
            _ = add(drawable: subtitle)
        }
    }

    private let listener = ConnectionListener()
    private var listenerRegistered = false
    private var hudScreen: HudScreen?
    private var statusText = "sdk not ready"
    private var isConnected = false
    private var isReady = false
    private var isScreenAdded = false
#endif

    private let statusLabel = UILabel()
    private let configuredLabel = UILabel()
    private let initButton = UIButton(type: .system)
    private let connectButton = UIButton(type: .system)
    private let screenButton = UIButton(type: .system)
    private var sdkGatedButtons: [UIButton] = []

    override func viewDidLoad() {
        super.viewDidLoad()
        title = "Maverick AI — iOS native"
        view.backgroundColor = .systemBackground

        statusLabel.text = "status: sdk not ready"
        statusLabel.font = .systemFont(ofSize: 17, weight: .semibold)
        statusLabel.numberOfLines = 0
        configuredLabel.text = "configured device: not configured"
        configuredLabel.textColor = .secondaryLabel
        configuredLabel.font = .systemFont(ofSize: 14)
        configuredLabel.numberOfLines = 0

#if canImport(MaverickAI)
        listener.onStatus = { [weak self] status in
            DispatchQueue.main.async { self?.applyConnectionStatus(status) }
        }
        listener.onReadyChanged = { [weak self] ready in
            DispatchQueue.main.async {
                guard let self else { return }
                self.isReady = ready
                self.isConnected = ready
                self.statusText = ready ? "ready" : "disconnected"
                // Nothing drawn before Ready survives, so the screen goes on once it is.
                if ready { self.addHudOnce() }
                self.refreshUi()
            }
        }
#endif

        configure(initButton, "1 · Init SDK", #selector(onInit))
        let configureButton = makeButton("2 · Configure glasses", #selector(onConfigure))
        configure(connectButton, "3 · Connect", #selector(onToggleConnect))
        configure(screenButton, "4 · Add HUD screen", #selector(onToggleScreen))
        sdkGatedButtons = [configureButton, connectButton, screenButton]

        let stack = UIStackView(arrangedSubviews: [
            statusLabel,
            configuredLabel,
            initButton,
            configureButton,
            connectButton,
            screenButton,
        ])
        stack.axis = .vertical
        stack.spacing = 14
        stack.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(stack)
        NSLayoutConstraint.activate([
            stack.leadingAnchor.constraint(equalTo: view.layoutMarginsGuide.leadingAnchor),
            stack.trailingAnchor.constraint(equalTo: view.layoutMarginsGuide.trailingAnchor),
            stack.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor, constant: 24),
        ])

        applySdkGatedEnabled(false)
    }

    // ── the four steps ────────────────────────────────────────────────────────

    /// Step 1. Everything else in the SDK requires this to have run.
    @objc private func onInit() {
#if canImport(MaverickAI)
        guard !Evs.shared.wasInitialized() else { return }
        Evs.shared.doInit()
        ensureConnectionListenerRegistered()
        statusText = "init ok"
        refreshUi()
#endif
    }

    /**
     * Step 2. Scan and pick a pair of glasses.
     *
     * The SDK ships this screen, so a host app does not have to write BLE scanning UI. It
     * is also where the Bluetooth permission prompt happens.
     */
    @objc private func onConfigure() {
#if canImport(MaverickAI)
        Evs.shared.glassesService.disconnect()
        isConnected = false
        isReady = false
        statusText = "disconnected"
        Evs.shared.showAppUI(option: M2AppUIOption.companion.DefaultConfigure)
        refreshUi()
#endif
    }

    /// Step 3. Connect to whatever step 2 configured.
    @objc private func onToggleConnect() {
#if canImport(MaverickAI)
        if isConnected || isReady {
            Evs.shared.glassesService.disconnect()
            isConnected = false
            isReady = false
            statusText = "disconnected"
        } else {
            ensureConnectionListenerRegistered()
            Evs.shared.glassesService.connect()
            statusText = "connecting"
        }
        refreshUi()
#endif
    }

    /// Step 4. Put something on the glasses.
    @objc private func onToggleScreen() {
#if canImport(MaverickAI)
        if isScreenAdded, let hudScreen {
            _ = Evs.shared.screenService.removeScreen(screen: hudScreen)
            isScreenAdded = false
            refreshUi()
            return
        }
        addHudOnce()
        refreshUi()
#endif
    }

    // ── plumbing ──────────────────────────────────────────────────────────────

#if canImport(MaverickAI)
    /// Idempotent: Ready can fire more than once, and the button calls this too.
    private func addHudOnce() {
        guard Evs.shared.wasInitialized(), !isScreenAdded else { return }
        if hudScreen == nil { hudScreen = HudScreen() }
        if let hudScreen {
            _ = Evs.shared.screenService.addScreen(screen: hudScreen)
            isScreenAdded = true
        }
    }

    /// Registering twice would deliver every callback twice.
    private func ensureConnectionListenerRegistered() {
        if !listenerRegistered {
            Evs.shared.glassesService.registerConnectionListener(listener: listener)
            listenerRegistered = true
        }
    }

    private func applyConnectionStatus(_ status: M2ConnectionStatus) {
        _ = status
        let ready = Evs.shared.glassesService.isReady()
        let connected = Evs.shared.glassesService.isConnected()
        statusText = ready ? "ready" : (connected ? "connected" : "disconnected")
        isReady = ready
        isConnected = connected
        if ready { addHudOnce() }
        refreshUi()
    }

    private func refreshConfiguredLabel() {
        let name = Evs.shared.glassesService.getDeviceName().trimmingCharacters(in: .whitespacesAndNewlines)
        let address = Evs.shared.glassesService.getDeviceAddress().trimmingCharacters(in: .whitespacesAndNewlines)
        let configuredName = name.isEmpty ? address : name
        configuredLabel.text = configuredName.isEmpty
            ? "configured device: not configured"
            : "configured device: \(configuredName)"
    }

    private func refreshUi() {
        statusLabel.text = "status: \(statusText)"
        let isSdkInitialized = Evs.shared.wasInitialized()
        initButton.setTitle(isSdkInitialized ? "1 · SDK initialized" : "1 · Init SDK", for: .normal)
        initButton.isEnabled = !isSdkInitialized
        connectButton.setTitle((isConnected || isReady) ? "3 · Disconnect" : "3 · Connect", for: .normal)
        screenButton.setTitle(isScreenAdded ? "4 · Remove HUD screen" : "4 · Add HUD screen", for: .normal)
        applySdkGatedEnabled(isSdkInitialized)
        refreshConfiguredLabel()
    }
#else
    private func refreshUi() {}
#endif

    private func applySdkGatedEnabled(_ enabled: Bool) {
        for button in sdkGatedButtons {
            button.isEnabled = enabled
            button.alpha = enabled ? 1.0 : 0.4
        }
    }

    private func makeButton(_ label: String, _ action: Selector) -> UIButton {
        let button = UIButton(type: .system)
        configure(button, label, action)
        return button
    }

    private func configure(_ button: UIButton, _ label: String, _ action: Selector) {
        var config = UIButton.Configuration.bordered()
        config.title = label
        button.configuration = config
        button.addTarget(self, action: action, for: .touchUpInside)
    }
}
