/*
 * Created by Everysight LTD.
 *
 * SwiftUI iOS host for the Maverick AI KMP Compose sample.
 */

import SwiftUI
import UIKit
import ComposeApp

private let everysightDarkBlue = Color(red: 17.0 / 255.0, green: 23.0 / 255.0, blue: 35.0 / 255.0)

/// Wraps the Kotlin Compose view controller for SwiftUI.
struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

/// Starts the iOS host app for the shared Compose sample.
@main
struct iOSApp: App {
    var body: some Scene {
        WindowGroup {
            ZStack {
                everysightDarkBlue.ignoresSafeArea()
                ComposeView()
                    .ignoresSafeArea()
            }
            .background(everysightDarkBlue)
        }
    }
}
