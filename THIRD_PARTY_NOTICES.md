# Third-Party Notices

This file contains attribution notices for third-party software included in or used by the Maverick AI / AI Pro SDK.

---

## Apache License 2.0

The following components are licensed under the [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0):

### Kotlin and Kotlin Multiplatform
- Copyright (c) JetBrains s.r.o.
- https://github.com/JetBrains/kotlin

### Jetpack Compose Multiplatform
- Copyright (c) JetBrains s.r.o.
- https://github.com/JetBrains/compose-multiplatform

### kotlinx.coroutines
- Copyright (c) JetBrains s.r.o.
- https://github.com/Kotlin/kotlinx.coroutines

### kotlinx.serialization
- Copyright (c) JetBrains s.r.o.
- https://github.com/Kotlin/kotlinx.serialization

### kotlinx-datetime
- Copyright (c) JetBrains s.r.o.
- https://github.com/Kotlin/kotlinx-datetime

### kotlinx-atomicfu
- Copyright (c) JetBrains s.r.o.
- https://github.com/Kotlin/kotlinx-atomicfu

### Ktor
- Copyright (c) JetBrains s.r.o.
- https://github.com/ktorio/ktor

### AndroidX Libraries
- Copyright (c) The Android Open Source Project
- Includes: Activity Compose, Lifecycle, Compose UI, Compose Foundation, Material 3, CameraX
- https://github.com/androidx/androidx

### AndroidX Media3 (ExoPlayer)
- Copyright (c) The Android Open Source Project
- https://github.com/androidx/media

### Kermit
- Copyright (c) Touchlab
- https://github.com/touchlab/Kermit

### Multiplatform Settings
- Copyright (c) Russell Wolf
- https://github.com/russhwolf/multiplatform-settings

### MaterialKolor
- Copyright (c) Jordan Fisher
- https://github.com/jordond/MaterialKolor

### LiteRT (formerly TensorFlow Lite)
- Copyright (c) Google LLC
- Android only - used for eye tracking model inference
- https://github.com/google-ai-edge/LiteRT

---

## BSD License (3-Clause)

### Opus Codec
- Copyright (c) Xiph.Org Foundation, Skype Limited, Octasic, Jean-Marc Valin, Timothy B. Terriberry, CSIRO, Gregory Maxwell, Mark Borgerding, Erik de Castro Lopo
- Licensed under the Modified (3-Clause) BSD License
- Used for audio encoding/decoding (microphone recording)
- https://opus-codec.org
- https://github.com/xiph/opus

---

## LGPL License

### FFmpeg

FFmpeg is used internally by the SDK to receive RTSP video streams and to decode H.264 for the
phone preview. The SDK does not use FFmpeg to encode video: all video encoding uses the platform
encoders (Android MediaCodec, iOS VideoToolbox).

**FFmpeg libraries - Android and iOS, statically linked (custom minimal build):**
- FFmpeg (LGPL 2.1+): https://ffmpeg.org
- Source code: https://github.com/FFmpeg/FFmpeg
- Libraries: libavcodec, libavformat, libavutil, libswscale
- Custom build includes only: H.264 / HEVC / MJPEG decoders and parsers, RTSP / RTP / SDP
  demuxing and protocols, swscale. No encoders and no GPL components.
- Android: linked into the SDK's `libm2video.so`. iOS: linked into the `MaverickAI` framework.

**ffmpeg-kit - Android only, dynamically linked:**
- ffmpeg-kit (LGPL 3.0): https://github.com/arthenica/ffmpeg-kit
- Android package: `io.github.jamaismagic.ffmpeg:ffmpeg-kit-main-16kb:6.1.7`
- Used only to export phone-preview recordings (ProRes, VP9 WebM); its shared libraries
  (`.so`) are loaded at runtime

---

## Google Play Services

### Google Play Services Location
- Copyright (c) Google LLC
- Android only - used for location services
- Subject to [Google Play Services Terms](https://developers.google.com/terms)
- https://developers.google.com/android/guides/overview

---

# Glasses Firmware Third-Party Notices

The following third-party components are used in the Maverick AI glasses firmware.

---

## Apache License 2.0

The following components are licensed under the [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0):

### Mbed TLS
- Copyright (c) Arm Limited
- https://github.com/Mbed-TLS/mbedtls

### LC3 (Low Complexity Communication Codec)
- https://github.com/google/liblc3

---

## Alif Semiconductor License

### Alif Semiconductor
- Licensed under the Alif Semiconductor Software License Agreement
- https://alifsemi.com/license

---

## BSD License (3-Clause)

### ST (STMicroelectronics)
- Licensed under the [BSD 3-Clause License](https://opensource.org/licenses/BSD-3-Clause)

---

## zlib License

### zlib
- Copyright (c) 1995-2017 Jean-loup Gailly and Mark Adler
- https://www.zlib.net

This software is provided 'as-is', without any express or implied
warranty. In no event will the authors be held liable for any damages
arising from the use of this software.

Permission is granted to anyone to use this software for any purpose,
including commercial applications, and to alter it and redistribute it
freely, subject to the following restrictions:

1. The origin of this software must not be misrepresented; you must not
   claim that you wrote the original software. If you use this software
   in a product, an acknowledgment in the product documentation would be
   appreciated but is not required.
2. Altered source versions must be plainly marked as such, and must not be
   misrepresented as being the original software.
3. This notice may not be removed or altered from any source distribution.

### LodePNG
- Copyright (c) 2005-2025 Lode Vandevenne
- https://lodev.org/lodepng/

This software is provided 'as-is', without any express or implied
warranty. In no event will the authors be held liable for any damages
arising from the use of this software.

Permission is granted to anyone to use this software for any purpose,
including commercial applications, and to alter it and redistribute it
freely, subject to the following restrictions:

1. The origin of this software must not be misrepresented; you must not
   claim that you wrote the original software. If you use this software
   in a product, an acknowledgment in the product documentation would be
   appreciated but is not required.
2. Altered source versions must be plainly marked as such, and must not be
   misrepresented as being the original software.
3. This notice may not be removed or altered from any source distribution.
