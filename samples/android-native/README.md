# android-native

Created by Everysight LTD.

Android native phone sample for the Maverick AI SDK.

## What This Sample Shows

- Initialize and stop the SDK from an Android `ComponentActivity`.
- Request the BLE permissions required before opening the configure UI.
- Configure glasses, connect, disconnect, and track connection state.
- Install an Android asset resolver for SDK resources.
- Render a simple `M2Screen` HUD with text and shapes on the glasses.

## Build

```bash
./gradlew :app:assembleDebug
```

## Code Tour

- `app/src/main/java/com/everysight/samples/androidnative/MainActivity.kt` contains
  the full phone flow: UI, SDK lifecycle, BLE permissions, resource lookup, and
  the sample HUD screen.
