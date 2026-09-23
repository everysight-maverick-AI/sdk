---
name: evs-mav2-sdk-quickstart
description: Generate a runnable Maverick AI SDK hello-world project - Compose Multiplatform (Android + iOS from one codebase, the default) or native Android and/or iOS - wired to the latest published SDK release, with a README covering the sdk.key, the GitHub Packages token, and links to the samples and SDK repos. Use when someone new is starting with the Maverick2 SDK, asks for a starter/template/scaffold/hello-world project, asks "how do I begin", "where do I put sdk.key", "how do I add the SDK to my app", or when onboarding an external developer or partner onto the SDK.
---

# Maverick AI SDK quickstart

Generates a minimal, buildable app that initializes the SDK, pairs and connects to the
glasses, and draws a HUD — plus the README explaining everything the generator cannot do
for the developer (get a key, get a token, pick a signing team).

For an existing app that already consumes the SDK, this is the wrong skill: resource
placement and resolvers are `evs-mav2-sdk-app-integration`, and a review of what an app
got wrong is `evs-mav2-app-review-resources`. To hand someone a document rather than a
project, `evs-mav2-sdk-user-guide` renders the same material as a branded PDF — the two
are meant to be sent together.

## Run it

```bash
python3 "$HOME/.claude/skills/evs-mav2-sdk-quickstart/scripts/new_mav2_project.py" \
  --name HelloMav2 --out ~/dev
```

Windows (PowerShell):

```powershell
py "$HOME\.claude\skills\evs-mav2-sdk-quickstart\scripts\new_mav2_project.py" `
  --name HelloMav2 --out "$HOME\dev"
```

The default is **KMP**: one Compose Multiplatform project whose shared code runs on both
Android and iOS. Use it unless the developer explicitly wants native apps
(`--platform android|ios|both`).

| Flag | |
|---|---|
| `--name` | project name, letters and digits (default `HelloMav2`). Becomes the Gradle project and Xcode target. |
| `--platform` | `kmp` (default, one Compose Multiplatform project) · `android` · `ios` · `both` (native) |
| `--out` | parent directory; the project is created as `<out>/<name>/` |
| `--package` | Android `applicationId` (default `com.example.<name>`) |
| `--bundle-id` | iOS bundle id (default: same as `--package`) |
| `--sdk-version` | pin a version; default is to look up the latest release |
| `--team` | Apple Developer team id, baked into signing |
| `--spm-pin` | native iOS only: `version` (default, `upToNextMinor` from `--sdk-version`) or `branch` to track `main` |
| `--offline` | skip the version lookup; requires `--sdk-version` |
| `--force` | overwrite an existing target directory |

Only the version lookup touches the network (public GitHub releases API, no auth). It
falls back to a baked-in version when offline and says so in the output and the README.

For Codex, use the same command with `.codex` in place of `.claude`.
From an extracted download, run `python3 evs-mav2-sdk-quickstart/scripts/new_mav2_project.py` with the same flags.

## What it produces

KMP (default):

```
HelloMav2/
  README.md
  kmp/
    composeApp/src/commonMain/.../App.kt           the whole SDK flow + the HUD (shared)
    composeApp/src/androidMain/.../MainActivity.kt  Evs.init(context) + BLE permissions
    composeApp/src/iosMain/.../MainViewController.kt
    iosApp/<Name>.xcodeproj                         runs ./gradlew to build ComposeApp.framework
```

The SDK comes from GitHub Packages for **both** platforms - one KMP artifact, no Swift
Package and no extra Gradle plugin (the old `com.everysight.mav2.sdk-ios` plugin is a no-op
since 0.2.0, so the project does not apply it).

Native (`--platform both`):

```
HelloMav2/
  README.md      key placement, token setup, build commands, links, pitfalls
  android/       Gradle project, SDK from GitHub Packages
  ios/           Xcode project, SDK from the public Swift Package
```

Every variant does the same things in the same order: `init` → BLE permission →
`showAppUI(DefaultConfigure)` → `connect` → `addScreen`. The HUD is an `M2Screen` subclass
at the bottom of the main file.

## Tell the developer these three things

The generator writes them into the README, but they are the whole difference between a
project that runs and one that does not.

1. **The key.** Everysight issues a JWT scoped to your application namespace. It is
   **not** needed to initialize — only to connect. `init()` succeeds without it and the
   connection then ends in `M2ConnectionStatus.AuthFailed`, which is why both projects fail
   the *build* when no key is bundled: otherwise nothing surfaces until authentication.
   KMP: `kmp/composeApp/src/androidMain/assets/sdk.key` and `kmp/iosApp/<Name>/sdk.key`.
   Native Android: `app/src/main/assets/sdk.key` (or `app.key`). Native iOS:
   `<Name>/sdk.key`. On iOS it is wired as a bundle resource by filename. A `PUT-SDK-KEY-HERE.txt` sits in each directory.
   Resolution order is `setDeveloperKey()` → `app.key` → `sdk.<serial>.key` → `sdk.key`,
   and the first connect exchanges the key online for a per-glasses certificate.
2. **A GitHub token, for Android and for KMP (both platforms).** GitHub Packages requires authentication even for
   public packages. `gpr.user` / `gpr.key` (a classic PAT with `read:packages`) go in
   `~/.gradle/gradle.properties` — the home directory, not the project. Without it Gradle
   fails with a bare `401 Unauthorized` on the SDK artifact. Only a *native* iOS project needs no token.
3. **The Gradle wrapper is not generated.** Android Studio provisions it on first open;
   from the CLI it is `gradle wrapper --gradle-version <pinned>` once. Shipping a
   `gradle-wrapper.jar` in a skill is not appropriate, so the README says this instead.
   In KMP the **iOS build needs it too** - Xcode's *Compile Kotlin Framework* phase calls
   `./gradlew`, and fails with the exact `gradle wrapper` command when it is missing.

## Windows

The generator runs anywhere Python 3.10+ does (`py` on Windows) and writes LF line endings,
so a project generated on Windows can be moved to a Mac unchanged. On Windows the
developer can build and run **Android** only (`.\gradlew.bat :composeApp:installDebug`,
or `:app:installDebug` for native); the KMP project sets
`kotlin.native.ignoreDisabledTargets=true` so Gradle skips the iOS targets quietly. iOS
needs a Mac with Xcode. The token goes in `%USERPROFILE%\.gradle\gradle.properties`.
Every README command has a PowerShell twin.

## Keeping it current

The pinned toolchain versions live at the top of `scripts/new_mav2_project.py`. They are
what the generated projects were actually built against, not "whatever is newest" — bump
them deliberately.

The KMP pins (`KMP_AGP_VERSION`, `KMP_KOTLIN_VERSION`, `COMPOSE_MP_VERSION`) follow
`sdk/samples/kmp-compose-sample` - the Kotlin version must be one the SDK's KMP artifact can
be consumed by, so bump it together with the sample, not on its own.

`AGP_VERSION` has a hard floor: the SDK pulls `androidx.camera 1.6.0`, which refuses any
AGP below 8.9.1 with a `checkDebugAarMetadata` failure. `MIN_SDK` is the SDK's own floor
and must not be lowered.

After changing a template, regenerate and build every platform before committing - and
**launch** the KMP iOS app once: two of its failure modes (a missing
`CADisableMinimumFrameDurationOnPhone`, an SDK object created before `Evs.init()`) build
cleanly and only crash at runtime.

```bash
# KMP (needs a wrapper, an SDK location, gpr credentials, and a non-empty sdk.key in both places)
./scripts/new_mav2_project.py --name TmpKmp --out /tmp --force
cd /tmp/TmpKmp/kmp && gradle wrapper --gradle-version 8.13 && \
  echo dummy > composeApp/src/androidMain/assets/sdk.key && echo dummy > iosApp/TmpKmp/sdk.key && \
  ./gradlew :composeApp:assembleDebug && \
  xcodebuild -project iosApp/TmpKmp.xcodeproj -scheme TmpKmp \
    -destination 'generic/platform=iOS Simulator' CODE_SIGNING_ALLOWED=NO build

./scripts/new_mav2_project.py --name TmpCheck --platform both --out /tmp --force
# iOS  (needs a non-empty sdk.key to get past the guard)
cd /tmp/TmpCheck/ios && echo dummy > TmpCheck/sdk.key && \
  xcodebuild -project TmpCheck.xcodeproj -scheme TmpCheck \
    -destination 'generic/platform=iOS Simulator' CODE_SIGNING_ALLOWED=NO build
# Android (needs a wrapper, an SDK location, and gpr credentials)
cd /tmp/TmpCheck/android && echo dummy > app/src/main/assets/sdk.key && \
  gradle wrapper --gradle-version 8.13 && ./gradlew :app:assembleDebug
```

## Where the source of truth is

The generated code mirrors `sdk/samples/{kmp-compose-sample,android-native,ios-native}`, which the build
system validates against every SDK release. When the SDK API changes, read the samples
rather than guessing: the iOS surface in particular is Kotlin/Native-flavoured and easy
to get wrong — the listener must subclass `NSObject`, implement **every** protocol member
including `onConfigureDevice`, colours are enum cases (`M2Color.white`, not a companion),
setters take `evsColor:` / `tag:` labels and return the receiver.

| | |
|---|---|
| Samples | https://github.com/everysight-maverick-AI/sdk/tree/main/samples |
| iOS Swift Package | https://github.com/everysight-maverick-AI/mav-ai-ios-spm |
| Android Maven | https://github.com/everysight-maverick-AI/mav-ai-android-maven |
| Docs portal | https://everysight.github.io/maverick-ai-docs |

## iOS build settings that must survive

All are in the generated projects and all cause confusing failures if removed.

- **KMP: the `Compile Kotlin Framework` phase, first** - it runs
  `embedAndSignAppleFrameworkForXcode` + `syncComposeResourcesForIos`. Without it Swift
  cannot `import ComposeApp`, or the SDK UI renders without its assets.
- **KMP: `CADisableMinimumFrameDurationOnPhone = true` in Info.plist** - Compose
  Multiplatform throws at launch without it.

- **Native: `Copy Maverick AI Compose Resources`** build phase — the SDK ships its images and
  fonts inside the framework bundle, where iOS will not look for them. The phase rsyncs
  them up into the `.app`. Remove it and SDK UI renders without assets.
- **`EXCLUDED_ARCHS[sdk=iphonesimulator*] = x86_64`** — the SDK's simulator slice is
  arm64-only, so an Intel slice fails to link.
