/*
 * Created by Everysight LTD.
 *
 * App delegate for the native UIKit Maverick AI sample.
 */

import UIKit

/// Starts the sample app and installs the root view controller.
@main
final class AppDelegate: UIResponder, UIApplicationDelegate {
    var window: UIWindow?

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        let window = UIWindow(frame: UIScreen.main.bounds)
        window.rootViewController = ViewController()
        window.makeKeyAndVisible()
        self.window = window
        return true
    }
}
