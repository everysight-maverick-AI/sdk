/*
 * Created by Everysight LTD.
 *
 * Native UIKit Maverick AI sample. The view controller demonstrates SDK
 * initialization, resource resolution, configuration, connection, and a simple
 * glasses HUD without relying on a higher-level app architecture.
 */

import UIKit
#if canImport(MaverickAI)
import MaverickAI
#endif

private enum EsBrand {
    static let yellow = UIColor(red: 0.922, green: 0.922, blue: 0.439, alpha: 1.0)
    static let darkBlue = UIColor(red: 0.067, green: 0.090, blue: 0.137, alpha: 1.0)
    static let darkBlue2 = UIColor(red: 0.110, green: 0.133, blue: 0.200, alpha: 1.0)
    static let blueGrey = UIColor(red: 0.545, green: 0.616, blue: 0.682, alpha: 1.0)
    static let lightBlue = UIColor(red: 0.651, green: 0.733, blue: 0.792, alpha: 1.0)
    static let glassBorder = UIColor(red: 0.824, green: 0.886, blue: 1.0, alpha: 0.17)
}

/// Main UIKit screen for the native iOS SDK sample.
final class ViewController: UIViewController {
#if canImport(MaverickAI)

    private final class ConnectionListener: NSObject, IM2GlassesConnectionEvents {
        var onStatus: ((ConnectionStatus) -> Void)?
        var onReadyChanged: ((Bool) -> Void)?

        func onConfigureDevice(address: String?, name: String?) {
            print("configured device address=\(address ?? "nil") name=\(name ?? "nil")")
        }

        func onConnectionStatusChanged(status: ConnectionStatus) {
            print("status=\(status)")
            onStatus?(status)
        }

        func onReady() {
            print("ready")
            onReadyChanged?(true)
        }

        func onUnReady() {
            print("unready")
            onReadyChanged?(false)
        }
    }

    /// Simple HUD screen rendered on the glasses when Add Screen is selected.
    private final class HudScreen: M2Screen {
        init() {
            super.init(width: 420, height: 180, tag: "sample-hud-screen")
        }

        override func onCreate() {
            let background = M2RectFilled(color: M2Color.black, tag: nil)
            _ = background.setDimensions(x: 0, y: 0, width: 420, height: 180)

            let title = M2Text(text: "MAV2 SDK Sample HUD", tag: nil)
            _ = title.setXY(x: 24, y: 24)
            _ = title.setColor(evsColor: M2Color.white)
            _ = title.setScale(scale: 1.1)

            let subtitle = M2Text(text: "Regular M2Screen (not full screen)", tag: nil)
            _ = subtitle.setXY(x: 24, y: 54)
            _ = subtitle.setColor(evsColor: M2Color.white)
            _ = subtitle.setScale(scale: 0.82)

            let box = M2RectOutline(color: M2Color.green, tag: nil)
            _ = box.setDimensions(x: 16, y: 14, width: 388, height: 144)

            _ = add(drawable: background)
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
        view.backgroundColor = EsBrand.darkBlue
        overrideUserInterfaceStyle = .dark
        title = "MAV2 iOS Native Sample"

        let scroll = UIScrollView()
        scroll.translatesAutoresizingMaskIntoConstraints = false
        scroll.alwaysBounceVertical = true
        view.addSubview(scroll)

        let stack = UIStackView()
        stack.axis = .vertical
        stack.spacing = 14
        stack.translatesAutoresizingMaskIntoConstraints = false
        scroll.addSubview(stack)
        NSLayoutConstraint.activate([
            scroll.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            scroll.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            scroll.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor),
            scroll.bottomAnchor.constraint(equalTo: view.safeAreaLayoutGuide.bottomAnchor),
            stack.leadingAnchor.constraint(equalTo: scroll.leadingAnchor, constant: 20),
            stack.trailingAnchor.constraint(equalTo: scroll.trailingAnchor, constant: -20),
            stack.topAnchor.constraint(equalTo: scroll.topAnchor, constant: 16),
            stack.bottomAnchor.constraint(equalTo: scroll.bottomAnchor, constant: -16),
            stack.widthAnchor.constraint(equalTo: scroll.widthAnchor, constant: -40),
        ])

        stack.addArrangedSubview(makeHeader(subtitle: "MAVERICK AI · IOS NATIVE SAMPLE"))
        statusLabel.text = "status: sdk not ready"
        statusLabel.textColor = .white
        statusLabel.font = UIFont.systemFont(ofSize: 17, weight: .semibold)
        configuredLabel.text = "configured device: not configured"
        configuredLabel.textColor = EsBrand.blueGrey
        configuredLabel.font = UIFont.systemFont(ofSize: 14)
        configuredLabel.numberOfLines = 0
        stack.addArrangedSubview(makeGlassCard([statusLabel, configuredLabel]))

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
                if ready { self.autoAddSampleHudOnConnect() }
                self.refreshUi()
            }
        }
#endif

        let buttons: [(String, Selector)] = [
            ("Init SDK", #selector(onInit)),
            ("Configure", #selector(onConfigure)),
            ("Adjust", #selector(onShowAdjust)),
            ("Connect", #selector(onToggleConnect)),
            ("Add Screen", #selector(onToggleScreen)),
        ]
        applyButtonStyle(initButton, label: "Init SDK", primary: false)
        initButton.addTarget(self, action: #selector(onInit), for: .touchUpInside)
        stack.addArrangedSubview(initButton)

        let actionsCard = UIStackView()
        actionsCard.axis = .vertical
        actionsCard.spacing = 10
        let row = UIStackView()
        row.axis = .horizontal
        row.distribution = .fillEqually
        row.spacing = 8
        for (label, action) in buttons {
            if action == #selector(onInit) { continue }
            let button: UIButton
            if action == #selector(onToggleConnect) {
                button = connectButton
            } else if action == #selector(onToggleScreen) {
                button = screenButton
            } else {
                button = UIButton(type: .system)
            }
            applyButtonStyle(button, label: label, primary: false)
            button.addTarget(self, action: action, for: .touchUpInside)
            sdkGatedButtons.append(button)
            if action != #selector(onToggleScreen) {
                row.addArrangedSubview(button)
            }
        }
        actionsCard.addArrangedSubview(row)
        actionsCard.addArrangedSubview(screenButton)
        stack.addArrangedSubview(makeGlassCard([actionsCard]))
        stack.addArrangedSubview(makeFooter())
        applySdkGatedEnabled(false)
    }

    private func applySdkGatedEnabled(_ enabled: Bool) {
        for button in sdkGatedButtons {
            button.isEnabled = enabled
            button.alpha = enabled ? 1.0 : 0.4
        }
    }

    private func makeHeader(subtitle: String) -> UIView {
        let container = UIStackView()
        container.axis = .vertical
        container.spacing = 6
        container.alignment = .leading
        let logo = UIImageView(image: UIImage(named: "Everysight_Header"))
        logo.contentMode = .scaleAspectFit
        logo.heightAnchor.constraint(lessThanOrEqualToConstant: 56).isActive = true
        container.addArrangedSubview(logo)
        let subtitleLabel = UILabel()
        subtitleLabel.textColor = EsBrand.blueGrey
        subtitleLabel.font = UIFont.systemFont(ofSize: 11, weight: .semibold)
        subtitleLabel.attributedText = NSAttributedString(string: subtitle, attributes: [.kern: 2.0])
        subtitleLabel.adjustsFontSizeToFitWidth = true
        container.addArrangedSubview(subtitleLabel)
        return container
    }

    private func makeFooter() -> UIView {
        let wrapper = UIView()
        let logo = UIImageView(image: UIImage(named: "Everysight_Footer"))
        logo.translatesAutoresizingMaskIntoConstraints = false
        logo.contentMode = .scaleAspectFit
        wrapper.addSubview(logo)
        NSLayoutConstraint.activate([
            logo.centerXAnchor.constraint(equalTo: wrapper.centerXAnchor),
            logo.topAnchor.constraint(equalTo: wrapper.topAnchor, constant: 4),
            logo.bottomAnchor.constraint(equalTo: wrapper.bottomAnchor, constant: -4),
            logo.heightAnchor.constraint(lessThanOrEqualToConstant: 28),
        ])
        return wrapper
    }

    private func makeGlassCard(_ views: [UIView]) -> UIView {
        let card = UIVisualEffectView(effect: UIBlurEffect(style: .systemUltraThinMaterialDark))
        card.translatesAutoresizingMaskIntoConstraints = false
        card.layer.cornerRadius = 20
        card.clipsToBounds = true
        card.layer.borderColor = EsBrand.glassBorder.cgColor
        card.layer.borderWidth = 1
        card.contentView.backgroundColor = EsBrand.darkBlue2.withAlphaComponent(0.48)
        let inner = UIStackView(arrangedSubviews: views)
        inner.axis = .vertical
        inner.spacing = 10
        inner.translatesAutoresizingMaskIntoConstraints = false
        card.contentView.addSubview(inner)
        NSLayoutConstraint.activate([
            inner.leadingAnchor.constraint(equalTo: card.contentView.leadingAnchor, constant: 16),
            inner.trailingAnchor.constraint(equalTo: card.contentView.trailingAnchor, constant: -16),
            inner.topAnchor.constraint(equalTo: card.contentView.topAnchor, constant: 14),
            inner.bottomAnchor.constraint(equalTo: card.contentView.bottomAnchor, constant: -14),
        ])
        return card
    }

    private func applyButtonStyle(_ button: UIButton, label: String, primary: Bool) {
        if !button.constraints.contains(where: { $0.firstAttribute == .height }) {
            button.heightAnchor.constraint(greaterThanOrEqualToConstant: 44).isActive = true
        }
        button.setTitle(label, for: .normal)
        button.titleLabel?.font = UIFont.systemFont(ofSize: 15, weight: .semibold)
        button.layer.cornerRadius = 12
        button.layer.borderWidth = primary ? 0 : 1
        button.layer.borderColor = EsBrand.glassBorder.cgColor
        if primary {
            button.backgroundColor = EsBrand.yellow
            button.setTitleColor(EsBrand.darkBlue, for: .normal)
        } else {
            button.backgroundColor = EsBrand.darkBlue2.withAlphaComponent(0.64)
            button.setTitleColor(.white, for: .normal)
        }
    }

    @objc private func onInit() {
#if canImport(MaverickAI)
        guard !Evs.shared.wasInitialized() else { return }
        Evs.shared.doInit()
        ensureConnectionListenerRegistered()
        statusText = "init ok"
        refreshConfiguredLabel()
        refreshUi()
#endif
        print("Init SDK requested")
    }

    @objc private func onConfigure() {
#if canImport(MaverickAI)
        Evs.shared.glassesService.disconnect()
        statusText = "disconnected"
        isConnected = false
        isReady = false
        Evs.shared.showUI(option: M2ShowUIOption.companion.DefaultConfigure)
        refreshConfiguredLabel()
        refreshUi()
#endif
        print("Configure requested")
    }

    @objc private func onShowAdjust() {
#if canImport(MaverickAI)
        Evs.shared.showUI(option: M2ShowUIOption.companion.DefaultAdjust)
#endif
        print("Show adjust requested")
    }

    @objc private func onToggleConnect() {
#if canImport(MaverickAI)
        if isConnected || isReady {
            Evs.shared.glassesService.disconnect()
            statusText = "disconnected"
            isConnected = false
            isReady = false
        } else {
            ensureConnectionListenerRegistered()
            Evs.shared.glassesService.connect()
            statusText = "connecting"
        }
        refreshUi()
#endif
        print("Toggle connect requested")
    }

    @objc private func onToggleScreen() {
#if canImport(MaverickAI)
        if isScreenAdded, let hudScreen {
            _ = Evs.shared.screenService.removeScreen(screen: hudScreen)
            isScreenAdded = false
            refreshUi()
            return
        }
        if hudScreen == nil {
            hudScreen = HudScreen()
        }
        if let hudScreen {
            _ = Evs.shared.screenService.addScreen(screen: hudScreen)
        }
        isScreenAdded = true
        refreshUi()
#endif
        print("Toggle screen requested")
    }

#if canImport(MaverickAI)
    private func ensureConnectionListenerRegistered() {
        if !listenerRegistered {
            Evs.shared.glassesService.registerConnectionListener(listener: listener)
            listenerRegistered = true
        }
    }

    private func applyConnectionStatus(_ status: ConnectionStatus) {
        _ = status
        let ready = Evs.shared.glassesService.isReady()
        let connected = Evs.shared.glassesService.isConnected()

        if ready {
            statusText = "ready"
        } else if connected {
            statusText = "connected"
        } else {
            statusText = "disconnected"
        }

        isReady = ready
        isConnected = connected
        if ready { autoAddSampleHudOnConnect() }
        refreshUi()
    }

    /// Auto-attaches HudScreen (the same screen the Add Screen button uses)
    /// the first time the glasses report Ready, so the operator sees the
    /// sample HUD without an extra tap. Idempotent across re-Ready callbacks.
    private func autoAddSampleHudOnConnect() {
        guard Evs.shared.wasInitialized(), !isScreenAdded else { return }
        if hudScreen == nil { hudScreen = HudScreen() }
        if let s = hudScreen {
            _ = Evs.shared.screenService.addScreen(screen: s)
            isScreenAdded = true
        }
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
        applyButtonStyle(initButton, label: isSdkInitialized ? "SDK initialized" : "Init SDK", primary: isSdkInitialized)
        initButton.isEnabled = !isSdkInitialized
        connectButton.setTitle((isConnected || isReady) ? "Disconnect" : "Connect", for: .normal)
        applyButtonStyle(screenButton, label: isScreenAdded ? "Remove Screen" : "Add Screen", primary: isScreenAdded)
        applySdkGatedEnabled(isSdkInitialized)
    }
#endif
}
