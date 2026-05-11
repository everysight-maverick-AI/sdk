# ios-native sample

Created by Everysight LTD.

Pure iOS app sample using **Swift Package Manager** from GitHub.

## What This Sample Shows

- Import the Maverick AI Swift Package from a UIKit app.
- Initialize the SDK, install a resource resolver, and register connection events.
- Configure glasses, connect, disconnect, and refresh user-visible status.
- Render a simple `M2Screen` HUD with text and shapes on the glasses.
- Clean up SDK listeners and screens when deinitializing.

## SDK source

- Package repo: `https://github.com/everysight-maverick-AI/mav-ai-ios-spm.git`
- Product: `Mav2SDK`

## Build (CLI)

```bash
xcodebuild \
  -project Mav2IosNativeSample.xcodeproj \
  -scheme Mav2IosNativeSample \
  -configuration Debug \
  -destination "generic/platform=iOS Simulator" \
  build
```

## Scenario flow in this sample

1. Init SDK
2. Configure simulator/device
3. Register listener + connect
4. Show SDK UI (configure/adjust)
5. Add simple HUD (text + shape)
6. Disconnect + cleanup

## Code Tour

- `Mav2IosNativeSample/ViewController.swift` contains the complete UIKit sample
  flow and the glasses HUD screen.
- `Mav2IosNativeSample/AppDelegate.swift` installs the root view controller.
