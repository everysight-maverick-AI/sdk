# Maverick AI / AI Pro SDK Release Notes

## 0.1.0 (Alpha) | 11 MAY 2026

Maverick AI / AI Pro SDK - Kotlin Multiplatform SDK for Maverick AI smart glasses.

> **Note:** Maverick AI / AI Pro is currently in **alpha** (SDK version 0.1.0). We are actively improving the SDK and working toward releasing the beta version in the coming months. The API surface will continue to expand, and **backward compatibility is not guaranteed** at this stage - breaking changes may occur between releases.

### Connectivity
- BLE 4.2+ connection management (BLE 5 recommended)
- Connection state machine with automatic reconnection
- WebSocket transport (for Glasses Simulator)

### Rendering
- Screen-based scene graph (`M2Screen`, `M2FullScreen`)
- Drawables: `M2Text`, `M2Rect`, `M2Image`, `M2Path`, `M2Sprite`, and more
- Animators: position, size, color, and more
- Rendering rate control

### LOS (Line of Sight) AR
- Billboard placement (head-locked in world space)
- World-positioned elements
- 3D coordinate projection

### Resources
- Custom font upload (SIF2 format via `font2sif` tool)
- Image upload (8-bit and 16-bit color via `image_convert` tool)
- Texture and sprite management with caching

### AI Vision
- Frame capture for AI/ML workloads (`M2AIVisionService`)
- Configurable resolution and frame rate
- Single-shot and continuous capture modes

### Microphone
- Audio recording in Ogg/Opus format (`M2MicService`)

### Sensors
- Touch gestures (tap, swipe, long press)
- IMU (gyroscope, accelerometer, magnetometer)
- Quaternion orientation
- Proximity and ambient light

### Display
- Brightness control (`M2DisplayService`)
- Auto brightness
- Digital screen position adjustment

### Phone UI
- Compose Multiplatform UI components for phone companion screens
- Real-time glasses display preview on phone
- Preview recording with background video compositing

### Developer Tools
- Glasses Simulator (develop without hardware)
- Developer dispatcher for safe SDK thread interaction
- Persistent preferences storage (`M2PreferencesService`)
- OTA firmware update support (`M2OtaService`)

### Platforms
- Android API 30+ (Kotlin)
- iOS 15+ (Swift via SPM)

### Delivery
- Android: Maven (GitHub Packages)
- iOS: Swift Package Manager
