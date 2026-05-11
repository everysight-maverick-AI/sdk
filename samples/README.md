# Maverick AI Public Samples

These samples are created by Everysight LTD for developers who want small,
copyable examples of Maverick AI SDK integration.

## Samples

- `android-native` - Android phone sample that initializes the SDK, configures
  glasses, connects, and renders a simple HUD screen.
- `ios-native` - UIKit sample that consumes the iOS Swift Package and renders a
  simple HUD screen.
- `kmp-compose-sample` - Compose Multiplatform sample covering UI kit
  drawables, animators, audio, AIVision, LOS sensors, 3D demos, OTA, display, and
  preview controls.

## Reading The Code

Each sample keeps the important SDK calls close to the UI action that triggers
them. Start with the platform entry point (`MainActivity`, `ViewController`,
`ContentView`, or `MainViewController`) and then follow the controller or HUD
screen class that creates the glasses content.

The source files include an Everysight LTD header and concise comments around
the pieces developers usually copy into their own apps: resource resolvers,
BLE permission gates, connection listeners, sensors listeners, screen creation,
and SDK cleanup.
