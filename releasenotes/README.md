# Maverick AI / AI Pro SDK — Release Notes

Kotlin Multiplatform SDK for Maverick AI smart glasses.

| Version | Released | Stage | In a line |
| --- | --- | --- | --- |
| [**0.2.0**](#020--beta) | 23 September 2026 | Beta | LC3 audio, a new AI Vision capture API, eye-tracker phase 1, H264 video, accelerated animation |
| [0.1.0](#010--alpha) | 11 May 2026 | Alpha | First public release |

> **Backward compatibility is not guaranteed while the SDK is in beta.** Breaking changes may
> occur between releases. Each entry below lists what you have to change.

---

## 0.2.0 — Beta

**Released 23 September 2026.** Second public release, and the first beta.

### At a glance

| Area | What changed |
| --- | --- |
| 🔊 **Audio** | Opus is gone. The microphone is LC3, and a new `M2AudioService` plays and streams audio to the glasses speaker. |
| 📷 **AI Vision** | Capture is described by readable option types instead of config objects. |
| 👁️ **Eye tracker** | Phase 1: the eye features, and an analyzer framework you write against. |
| 🎬 **Video** | Clips are H264. One clip per screen. |
| ✨ **Animation** | A fall, a bounce, a throw or an ease is one instruction the glasses walk by themselves. |
| 🎨 **Rendering** | Reworked gradients, texture fitting, fills on text, rounded corners. |
| ⚠️ **API naming** | Most public types gained the `M2` prefix — the largest source change in this release. |

### Migrating from 0.1.0

**Renamed types.** Mechanical, but your code will not compile until you update them.

| 0.1.0 | 0.2.0 |
| --- | --- |
| `Align`, `AlignV`, `Touch`, `PenShape`, `LineStyle` | `M2Align`, `M2AlignV`, `M2Touch`, `M2PenShape`, `M2LineStyle` |
| `ImageType`, `ResourceType`, `Rotation`, `RoundedCorners`, `AnchorPoint` | `M2ImageType`, `M2ResourceType`, `M2Rotation`, `M2RoundedCorners`, `M2AnchorPoint` |
| `AnimatorType`, `AnimatorRepeat`, `HitTestType`, `RenderingRate` | `M2AnimatorType`, `M2AnimatorRepeat`, `M2HitTestType`, `M2RenderingRate` |
| `ConnectionStatus`, `SensorsRate`, `PowerButton`, `CacheScope` | `M2ConnectionStatus`, `M2SensorsRate`, `M2PowerButton`, `M2CacheScope` |
| `DistanceUnits`, `UnitsType`, `FormattedDistance`, `FormatUtils`, `OTAVersion`, `Platform` | `M2DistanceUnits`, `M2UnitsType`, `M2FormattedDistance`, `M2FormatUtils`, `M2OtaVersion`, `M2Platform` |
| `Evs.showUI()`, `M2ShowUIOption`, `M2StockUiThemeConfig` | `Evs.showAppUI()`, `M2AppUIOption`, `M2AppUIThemeConfig` |
| `M2Clip`, `M2ClipResource`, `M2Mpeg1FileResource` | `M2VideoClip`, `M2VideoClipResource`, `M2VideoFileResource` |
| `M2CacheService`, `fw_version0()` | `M2AppCacheService`, `fwVersion()` |
| `M2LinearGradient`, `M2GradientBlur` | `M2GradientFade` / `M2GradientLinear`, `M2EdgeFeather` |

**Reshaped calls.**

| Was | Now |
| --- | --- |
| `startCapture(M2AIVisionConfig, M2AIFrameParams?)` | `startCapture(options: M2CaptureOptions)` |
| `takePicture(M2AIFrameParams)` | `takePicture()` |
| `openMicrophone(...)` | `openMicrophone(mode: M2MicMode, bitrateKBs: Int)` |
| `M2AIVisionServiceErrors`, `M2MicServiceErrors` | `M2AIVisionService.Errors`, `M2MicService.Errors` |
| `IM2ClipCallback` | `M2VideoClip.IM2ClipCallback` |
| `logger.debug(TAG, "message")` | `logger.debug(TAG) { "message" }` |

### What's new

#### 🔊 Audio

- **The microphone is LC3.** `openMicrophone(mode: M2MicMode, bitrateKBs: Int)`, where
  `M2MicMode` is `Directional` (self) or `Omni`. Frames arrive as one 10 ms LC3 packet,
  mono, 16 kHz.
- **`M2AudioService` drives the glasses speaker.** Open a stream and write PCM, LC3 frames,
  encoded audio or a live URL; play, stop and delete stored sounds; set volume and digital gain.

#### 📷 AI Vision

- Capture is described by options: `M2StillOptions`, `M2ContinuousOptions` and `M2VideoOptions`,
  composed from `M2Position`, `M2Exposure`, `M2CompressionQuality`, `M2Bandwidth` and
  `M2CaptureRate`.
- `takePicture()` takes no arguments. `currentOptions()` and `getReceivedBytesPerSec()` report
  what is running.
- `M2AIFrameResolution` carries the sensor and delivered sizes and the field of view, and can
  say whether a size is supported.
- The **Camera Lab** in `kmp-compose-sample` exercises every option live.

#### 👁️ Eye tracker — phase 1

This release delivers the **eye features** and the **analyzer mechanism**. You receive the eye
geometry the glasses extract, and you write analyzers that turn it into meaning.

| Piece | What it gives you |
| --- | --- |
| `M2EyeTrackerResult` | Iris and pupil ellipses, both eye corners, the eyelids, the LED glint, filtered pupil centre, pupil visibility |
| `M2EyeFeatureAnalyzer` | Your analyzer. Register it with `registerAnalyzer` and the service feeds it every frame |
| `M2AnalyzerInput` | The raw result, a `M2NormalizedFrame` that is safe to compare across people and sessions, and rolling `M2BaselineStats` |
| `M2AnalyzerKey` + `registerAnalyzerFactory` | Analyzers that depend on other analyzers |

`kmp-compose-sample` ships two worked analyzers — left/right gaze and blink — and draws the eye
geometry live.

> **Gaze-to-pixel calibration is not in this release.** Mapping a gaze onto a point of the
> display is the next phase.

#### 🎬 Video

- `M2VideoClip` plays **H264 baseline**, macroblock aligned, around 15 fps.
- **One clip per screen** — adding a second replaces the first. Content is set with
  `setFill(M2VideoClipResource)`; any other fill replaces the clip and pauses it.
- `M2VideoFileResource` reads MP4, MOV and Annex-B. `M2VideoLimits` answers whether a size fits
  in glasses memory and what the link will carry.

#### ✨ Animation

- Accelerated moves the glasses walk by themselves: `animateFall`, `animateBounce`,
  `animateThrow`, `animateQuad`, `easeXTo`, `easeYTo`, with `M2Easing` and `M2Motion`.
- Stock effects: `pulse`, `shake`, `flash`, `slideIn`, `slideOut`, `popIn`.
- `M2Animator.repeatCount(n)`, per-axis matrix scale, and chains that can wait on a block.

#### 🎨 Rendering

- Texture fitting: `M2TextureFit` (`Native`, `Stretch`, `ShapeToImage`) with filtering control.
- Gradients reworked: `M2GradientFade`, `M2GradientLinear`, `M2EdgeFeather`, and a
  `M2GradientLength` in pixels or a fraction of the axis.
- `M2Text` takes a fill and a transform matrix.
- Rounded corners on `M2Rect` and `M2RectOutline`.

#### 📦 Resources

- Hosted font catalog: build a URL for a Roboto by language, weight and pixel size with
  `M2FontUrlBuilder`. `M2ImageResourceUrl` does the same for images.
- `M2AppCacheService` caches downloaded resources on the phone, scoped by `M2CacheScope`.

#### 🖥️ Simulator

- The glasses simulator is now an Electron application, with the dashboard built in.

### Platforms and delivery

| Platform | Requirement | Delivered as |
| --- | --- | --- |
| Android | API 30+ (Kotlin) | Maven, on GitHub Packages |
| iOS | 15+ (Swift) | Swift Package Manager |

---

## 0.1.0 — Alpha

**Released 11 May 2026.** First public release of the Maverick AI / AI Pro SDK.

### What's in it

| Area | Capabilities |
| --- | --- |
| **Connectivity** | BLE 4.2+ connection management (BLE 5 recommended) · connection state machine with automatic reconnection · WebSocket transport for the Glasses Simulator |
| **Rendering** | Screen-based scene graph (`M2Screen`, `M2FullScreen`) · drawables `M2Text`, `M2Rect`, `M2Image`, `M2Path`, `M2Sprite` and more · animators for position, size and colour · rendering rate control |
| **LOS (Line of Sight) AR** | Billboard placement, head-locked in world space · world-positioned elements · 3D coordinate projection |
| **Resources** | Custom font upload (SIF2, via the `font2sif` tool) · image upload in 8-bit and 16-bit colour (via the `image_convert` tool) · texture and sprite management with caching |
| **AI Vision** | Frame capture for AI/ML workloads (`M2AIVisionService`) · configurable resolution and frame rate · single-shot and continuous capture |
| **Microphone** | Audio recording in Ogg/Opus (`M2MicService`) |
| **Sensors** | Touch gestures: tap, swipe, long press · IMU: gyroscope, accelerometer, magnetometer · quaternion orientation · proximity and ambient light |
| **Display** | Brightness control (`M2DisplayService`) · auto brightness · digital screen position adjustment |
| **Phone UI** | Compose Multiplatform components for companion screens · real-time glasses display preview on the phone · preview recording with background video compositing |
| **Developer tools** | Glasses Simulator, to develop without hardware · developer dispatcher for safe SDK thread interaction · persistent preferences (`M2PreferencesService`) · OTA firmware update (`M2OtaService`) |

### Platforms and delivery

| Platform | Requirement | Delivered as |
| --- | --- | --- |
| Android | API 30+ (Kotlin) | Maven, on GitHub Packages |
| iOS | 15+ (Swift) | Swift Package Manager |
