# kmp-compose-sample

Created by Everysight LTD.

Compose Multiplatform public sample for the Maverick AI SDK. The app is a
compact tour of SDK setup, glasses connection, UIKit drawables, animators,
audio, AIVision, LOS sensors, 3D demos, OTA/display probes, and the SDK preview.

## What This Sample Shows

- Initialize and stop the SDK from Android and iOS KMP hosts.
- Configure glasses, connect, disconnect, and track SDK readiness.
- Add and remove SDK UIKit drawables on a glasses HUD screen.
- Run a simple animator and manage drawable placement in a grid.
- Toggle microphone and AIVision streams and display live rates.
- Toggle LOS sensors and touch input.
- Launch LiveAI-inspired 3D, dice, and 3D picture demos.
- Probe display brightness and OTA availability.
- Show or close the SDK preview from a checkbox near SDK init/deinit.

## Dependency source

Resolves MAV2 SDK and the SDK iOS Gradle plugin from GitHub Packages:

`https://maven.pkg.github.com/everysight-maverick-AI/mav-ai-android-maven`

Provide credentials via `~/.gradle/gradle.properties`:

```properties
gpr.user=YOUR_GITHUB_USERNAME
gpr.key=YOUR_GITHUB_TOKEN_WITH_read_packages
```

The build-system release flow updates this sample to the published version
before running the public sample validation step.

## Build

```bash
./gradlew :composeApp:assemblePhoneDebug
xcodebuild -project iosApp/KmpComposeTestIosApp.xcodeproj -scheme KmpComposeTestIosApp -configuration Debug -destination 'generic/platform=iOS Simulator' build
```

## Code Tour

- `composeApp/src/commonMain/.../App.kt` contains the shared Compose UI and tab
  layout.
- `composeApp/src/commonMain/.../Mav2ComposeController.kt` owns SDK state,
  connection listeners, stream listeners, sensors, HUD screens, and service
  actions.
- `composeApp/src/commonMain/.../LosDemoScreens.kt` contains the 3D object, dice,
  and 3D picture demos adapted for this public sample.
- `composeApp/src/androidMain/.../MainActivity.kt` initializes the SDK on Android
  and provides BLE permission handling.
- `composeApp/src/iosMain/.../MainViewController.kt` hosts the shared Compose UI
  on iOS and resolves resources from app/framework bundles.
