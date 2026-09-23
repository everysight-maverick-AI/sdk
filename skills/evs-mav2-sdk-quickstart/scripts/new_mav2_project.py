#!/usr/bin/env python3
"""Created by Everysight LTD.

Generate a Maverick AI SDK "hello world" project - Compose Multiplatform by default, or
native Android and/or iOS - wired to the latest published SDK release, with a README
covering the sdk.key and everything else a new developer has to do by hand.

    ./new_mav2_project.py --name HelloMav2 --out ~/dev                     # KMP (default)
    ./new_mav2_project.py --name HelloMav2 --platform both --out ~/dev     # native Android + iOS

What it produces (nothing is downloaded except the version lookup):

    HelloMav2/
      README.md            what to do next, where sdk.key goes, links
      kmp/                 --platform kmp: one Gradle project (Android + iOS) + iosApp Xcode project
      android/             --platform android|both: Gradle project, SDK from GitHub Packages
      ios/                 --platform ios|both: Xcode project, SDK from the public Swift Package

The generator never writes an sdk.key: it drops a placeholder note in the exact
directory the key belongs in, because a wrong or fake key fails at init() in a way
that is hard to diagnose.

Requires: python3 only. Building the result needs Android Studio / JDK 17 for Android
and Xcode 15+ for iOS.
"""

from __future__ import annotations

import argparse
import json
import os
import re
import shutil
import stat
import sys
import urllib.error
import urllib.request
from dataclasses import dataclass
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import mav2_pbxproj as pbx  # noqa: E402
import mav2_templates as tpl  # noqa: E402

# ── where the SDK comes from ─────────────────────────────────────────────────
IOS_SPM_REPO = "everysight-maverick-AI/mav-ai-ios-spm"
IOS_SPM_URL = f"https://github.com/{IOS_SPM_REPO}.git"
ANDROID_MAVEN_REPO = "everysight-maverick-AI/mav-ai-android-maven"
ANDROID_MAVEN_URL = f"https://maven.pkg.github.com/{ANDROID_MAVEN_REPO}"
SAMPLES_REPO_URL = "https://github.com/everysight-maverick-AI/sdk/tree/main/samples"

# Used only when the version lookup cannot reach GitHub and no --sdk-version is given.
# The templates use the 0.2.0 API names (M2ConnectionStatus, M2AppUIOption, showAppUI), so
# the fallback must be at least 0.2.0.
FALLBACK_SDK_VERSION = "0.2.0"

# Toolchain pins. Bump deliberately; these are what the generated projects are tested
# against, not "whatever is newest".
AGP_VERSION = "8.12.0"     # >= 8.9.1: the SDK pulls androidx.camera 1.6.0
KOTLIN_VERSION = "2.2.20"
GRADLE_VERSION = "8.13"    # AGP 8.12 needs >= 8.13
COMPOSE_BOM = "2025.05.00"
ACTIVITY_COMPOSE = "1.10.1"
COMPILE_SDK = 36
TARGET_SDK = 36
MIN_SDK = 30          # the SDK's own floor; do not lower it
IOS_DEPLOYMENT_TARGET = "16.0"

# Compose Multiplatform pins - the ones sdk/samples/kmp-compose-sample builds with.
KMP_AGP_VERSION = "8.12.3"
KMP_KOTLIN_VERSION = "2.3.20"
COMPOSE_MP_VERSION = "1.10.3"


class Fail(SystemExit):
    def __init__(self, message: str) -> None:
        super().__init__(f"error: {message}")


@dataclass
class Context:
    name: str            # HelloMav2
    project_slug: str    # hello-mav2
    app_label: str       # human-readable app name
    package_name: str    # com.example.hellomav2
    bundle_id: str       # com.example.hellomav2
    target_name: str     # HelloMav2 (Xcode target / iOS source dir)
    log_tag: str
    sdk_version: str
    team: str
    spm_pin: str

    def android_values(self) -> dict:
        return dict(
            project_slug=self.project_slug,
            app_label=self.app_label,
            package_name=self.package_name,
            log_tag=self.log_tag,
            sdk_version=self.sdk_version,
            android_maven_url=ANDROID_MAVEN_URL,
            agp_version=AGP_VERSION,
            kotlin_version=KOTLIN_VERSION,
            gradle_version=GRADLE_VERSION,
            compose_bom=COMPOSE_BOM,
            activity_compose=ACTIVITY_COMPOSE,
            compile_sdk=COMPILE_SDK,
            target_sdk=TARGET_SDK,
            min_sdk=MIN_SDK,
        )

    def kmp_values(self) -> dict:
        return dict(
            self.android_values(),
            target_name=self.target_name,
            kmp_agp_version=KMP_AGP_VERSION,
            kmp_kotlin_version=KMP_KOTLIN_VERSION,
            compose_mp_version=COMPOSE_MP_VERSION,
        )

    def ios_values(self) -> dict:
        return dict(
            app_label=self.app_label,
            target_name=self.target_name,
            bundle_id=self.bundle_id,
        )


# ── helpers ──────────────────────────────────────────────────────────────────

def write(path: Path, content: str, executable: bool = False) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    # LF on every OS: a project generated on Windows must still build on the Mac (Xcode, bash).
    path.write_text(content, encoding="utf-8", newline="\n")
    if executable:
        path.chmod(path.stat().st_mode | stat.S_IXUSR | stat.S_IXGRP | stat.S_IXOTH)


def slugify(name: str) -> str:
    s = re.sub(r"(?<!^)(?=[A-Z])", "-", name)
    s = re.sub(r"[^A-Za-z0-9]+", "-", s).strip("-").lower()
    return s or "mav2-app"


def default_package(name: str) -> str:
    suffix = re.sub(r"[^a-z0-9]", "", name.lower()) or "mav2app"
    return f"com.example.{suffix}"


def validate_package(pkg: str, flag: str) -> str:
    if not re.fullmatch(r"[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)+", pkg):
        raise Fail(
            f"{flag} must be a reverse-DNS identifier of lowercase segments, "
            f"e.g. com.example.hello - got {pkg!r}"
        )
    if pkg.split(".")[-1] in {"class", "package", "int", "new", "object", "val", "var"}:
        raise Fail(f"{flag}: {pkg.split('.')[-1]!r} is a reserved word in Kotlin/Java")
    return pkg


def validate_name(name: str) -> str:
    if not re.fullmatch(r"[A-Za-z][A-Za-z0-9]*", name):
        raise Fail(
            "--name must start with a letter and contain only letters and digits "
            "(it becomes an Xcode target and a Gradle project name)"
        )
    return name


def latest_sdk_version(timeout: float = 8.0) -> tuple[str, str]:
    """Latest published SDK version.

    The iOS Swift Package repo is public and its release tag is the SDK version; the
    Android Maven packages are published from the same release, so one lookup is
    enough. Returns (version, source) where source explains where it came from.
    """
    url = f"https://api.github.com/repos/{IOS_SPM_REPO}/releases/latest"
    req = urllib.request.Request(url, headers={
        "Accept": "application/vnd.github+json",
        "User-Agent": "evs-mav2-sdk-quickstart",
    })
    try:
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            tag = json.load(resp).get("tag_name") or ""
    except (urllib.error.URLError, TimeoutError, json.JSONDecodeError, OSError) as exc:
        return FALLBACK_SDK_VERSION, f"offline fallback ({type(exc).__name__}); pass --sdk-version to override"
    version = tag.lstrip("vV")
    if not re.fullmatch(r"\d+\.\d+\.\d+([-.].+)?", version):
        return FALLBACK_SDK_VERSION, f"unexpected release tag {tag!r}; using fallback"
    return version, f"latest release of {IOS_SPM_REPO} ({tag})"


# ── Android ──────────────────────────────────────────────────────────────────

def generate_android(root: Path, ctx: Context) -> None:
    v = ctx.android_values()
    write(root / "settings.gradle.kts", tpl.ANDROID_SETTINGS_GRADLE.format(**v))
    write(root / "build.gradle.kts", tpl.ANDROID_ROOT_GRADLE.format(**v))
    write(root / "gradle.properties", tpl.ANDROID_GRADLE_PROPERTIES.format(**v))
    write(root / "gradle/wrapper/gradle-wrapper.properties",
          tpl.ANDROID_WRAPPER_PROPERTIES.format(**v))
    write(root / "app/build.gradle.kts", tpl.ANDROID_APP_GRADLE.format(**v))
    write(root / "app/src/main/AndroidManifest.xml", tpl.ANDROID_MANIFEST.format(**v))

    pkg_path = root / "app/src/main/java" / Path(*ctx.package_name.split("."))
    write(pkg_path / "MainActivity.kt", tpl.ANDROID_MAIN_ACTIVITY.format(**v))

    assets = root / "app/src/main/assets"
    write(assets / "PUT-SDK-KEY-HERE.txt", tpl.ANDROID_KEY_PLACEHOLDER)
    write(assets / ".gitkeep", "")

    write(root / ".gitignore", "\n".join([
        ".gradle/", "build/", "local.properties", ".idea/", "*.iml", ".DS_Store",
        "", "# Never commit the SDK key.", "app/src/main/assets/sdk.key", "",
    ]))


# ── iOS ──────────────────────────────────────────────────────────────────────

def generate_ios(root: Path, ctx: Context) -> None:
    v = ctx.ios_values()
    src = root / ctx.target_name
    write(src / "AppDelegate.swift", tpl.IOS_APP_DELEGATE.format(**v))
    write(src / "ViewController.swift", tpl.IOS_VIEW_CONTROLLER.format(**v))
    write(src / "Info.plist", tpl.IOS_INFO_PLIST.format(**v))
    write(src / "PUT-SDK-KEY-HERE.txt",
          tpl.IOS_KEY_PLACEHOLDER.format(target_name=ctx.target_name))

    # A version pin is the default: a moving branch turns an unrelated SDK change
    # into a broken build. Override with --spm-pin branch to track main.
    if ctx.spm_pin == "version":
        requirement = (
            "\t\t\t\tkind = upToNextMinorVersion;\n"
            f"\t\t\t\tminimumVersion = {ctx.sdk_version};\n"
        )
    else:
        requirement = "\t\t\t\tbranch = main;\n\t\t\t\tkind = branch;\n"
    team_line = f"\t\t\t\tDEVELOPMENT_TEAM = {ctx.team};\n" if ctx.team else ""

    proj = root / f"{ctx.target_name}.xcodeproj"
    write(proj / "project.pbxproj", pbx.PBXPROJ.format(
        target_name=ctx.target_name,
        bundle_id=ctx.bundle_id,
        spm_url=IOS_SPM_URL,
        spm_requirement=requirement,
        deployment_target=IOS_DEPLOYMENT_TARGET,
        development_team_line=team_line,
    ))
    write(proj / f"xcshareddata/xcschemes/{ctx.target_name}.xcscheme",
          pbx.XCSCHEME.format(target_name=ctx.target_name))

    write(root / ".gitignore", "\n".join([
        "build/", ".build/", "DerivedData/", "*.xcuserstate",
        "xcuserdata/", ".DS_Store",
        "", "# Never commit the SDK key.", f"{ctx.target_name}/sdk.key", "",
    ]))


# ── Compose Multiplatform ────────────────────────────────────────────────────

def generate_kmp(root: Path, ctx: Context) -> None:
    v = ctx.kmp_values()
    write(root / "settings.gradle.kts", tpl.KMP_SETTINGS_GRADLE.format(**v))
    write(root / "build.gradle.kts", tpl.KMP_ROOT_GRADLE.format(**v))
    write(root / "gradle.properties", tpl.KMP_GRADLE_PROPERTIES.format(**v))
    write(root / "gradle/wrapper/gradle-wrapper.properties",
          tpl.ANDROID_WRAPPER_PROPERTIES.format(**v))

    app = root / "composeApp"
    write(app / "build.gradle.kts", tpl.KMP_APP_GRADLE.format(**v))
    pkg = Path(*ctx.package_name.split("."))
    write(app / "src/commonMain/kotlin" / pkg / "App.kt", tpl.KMP_APP.format(**v))
    write(app / "src/androidMain/AndroidManifest.xml", tpl.KMP_ANDROID_MANIFEST.format(**v))
    write(app / "src/androidMain/kotlin" / pkg / "MainActivity.kt", tpl.KMP_MAIN_ACTIVITY.format(**v))
    write(app / "src/iosMain/kotlin" / pkg / "MainViewController.kt",
          tpl.KMP_MAIN_VIEW_CONTROLLER.format(**v))
    assets = app / "src/androidMain/assets"
    write(assets / "PUT-SDK-KEY-HERE.txt", tpl.KMP_ANDROID_KEY_PLACEHOLDER)
    write(assets / ".gitkeep", "")

    ios = root / "iosApp"
    src = ios / ctx.target_name
    write(src / "iOSApp.swift", tpl.KMP_IOS_APP.format(**v))
    write(src / "Info.plist", tpl.KMP_IOS_INFO_PLIST.format(**ctx.ios_values()))
    write(src / "PUT-SDK-KEY-HERE.txt", tpl.IOS_KEY_PLACEHOLDER.format(target_name=ctx.target_name))
    team_line = f"\t\t\t\tDEVELOPMENT_TEAM = {ctx.team};\n" if ctx.team else ""
    proj = ios / f"{ctx.target_name}.xcodeproj"
    write(proj / "project.pbxproj", pbx.KMP_PBXPROJ.format(
        target_name=ctx.target_name,
        bundle_id=ctx.bundle_id,
        deployment_target=IOS_DEPLOYMENT_TARGET,
        development_team_line=team_line,
        gradle_version=GRADLE_VERSION,
    ))
    write(proj / f"xcshareddata/xcschemes/{ctx.target_name}.xcscheme",
          pbx.XCSCHEME.format(target_name=ctx.target_name))

    write(root / ".gitignore", "\n".join([
        ".gradle/", ".kotlin/", "build/", "local.properties", ".idea/", "*.iml", ".DS_Store",
        "iosApp/build/", "DerivedData/", "xcuserdata/", "*.xcuserstate",
        "", "# Never commit the SDK key.",
        "composeApp/src/androidMain/assets/sdk.key", "composeApp/src/androidMain/assets/app.key",
        f"iosApp/{ctx.target_name}/sdk.key", "",
    ]))


# ── README ───────────────────────────────────────────────────────────────────

def generate_readme_kmp(root: Path, ctx: Context, version_source: str) -> None:
    L: list[str] = []
    a = L.append
    pkg_dir = ctx.package_name.replace(".", "/")

    a(f"# {ctx.name}")
    a("")
    a("A Maverick AI SDK hello world in Compose Multiplatform - one shared codebase for Android")
    a("and iOS - generated by `evs-mav2-sdk-quickstart`.")
    a("")
    a(f"- SDK version: **{ctx.sdk_version}** ({version_source})")
    a("- Platforms: **Android + iOS**, shared UI and SDK code in `composeApp/src/commonMain`")
    a("")
    a("The app does four things, in this order: init the SDK, open the SDK's pairing UI,")
    a("connect, then draw a HUD on the glasses. All of it is in one shared file, `App.kt`.")
    a("")
    a("---")
    a("")
    a("## 1. Get your SDK key")
    a("")
    a("Everysight issues a key file - a JWT scoped to your application namespace. The SDK")
    a("does **not** need it to initialize; it needs it to **connect**. `init()` succeeds")
    a("without a key and the connection then ends in `M2ConnectionStatus.AuthFailed`.")
    a("On first connect the key is exchanged online for a certificate tied to your app and to")
    a("that pair of glasses, so that call needs network.")
    a("")
    a("| Platform | Put `sdk.key` here |")
    a("|---|---|")
    a("| Android | `kmp/composeApp/src/androidMain/assets/sdk.key` |")
    a(f"| iOS | `kmp/iosApp/{ctx.target_name}/sdk.key` |")
    a("")
    a("It is the same file for both. Each directory has a `PUT-SDK-KEY-HERE.txt` reminder, and")
    a("both builds **fail** while the key is missing, so it cannot be forgotten silently. The")
    a("key is a secret; the generated `.gitignore` already excludes it.")
    a("")
    a(f"- Android `applicationId`: `{ctx.package_name}`")
    a(f"- iOS `PRODUCT_BUNDLE_IDENTIFIER`: `{ctx.bundle_id}`")
    a("")
    a("Keep these ids, or ask for a key that matches your own - a key only works for the")
    a("namespace it was issued for.")
    a("")
    a("---")
    a("")
    a("## 2. One-time setup")
    a("")
    a("### GitHub Packages token")
    a("")
    a("The SDK is published to GitHub Packages, which requires a token **even though the")
    a("package is public** - and in KMP both platforms build through Gradle, so both need it.")
    a("Create a classic personal access token with `read:packages` and put it in your home")
    a("directory's Gradle properties, never the project: `~/.gradle/gradle.properties` on macOS")
    a("and Linux, `%USERPROFILE%\\.gradle\\gradle.properties` on Windows.")
    a("")
    a("```properties")
    a("gpr.user=<your-github-username>")
    a("gpr.key=<token-with-read:packages>")
    a("```")
    a("")
    a("### Gradle wrapper")
    a("")
    a("The wrapper is not generated. Android Studio provisions it on first open; otherwise run")
    a("this once. **The iOS build needs it too** - Xcode compiles the shared code by calling")
    a("`./gradlew`, and says so if it is missing.")
    a("")
    a("```bash")
    a("cd kmp")
    a(f"gradle wrapper --gradle-version {GRADLE_VERSION}")
    a("```")
    a("")
    a("Windows (PowerShell) - the same command:")
    a("")
    a("```powershell")
    a("cd kmp")
    a(f"gradle wrapper --gradle-version {GRADLE_VERSION}")
    a("```")
    a("")
    a("---")
    a("")
    a("## 3. Build and run")
    a("")
    a("### Android")
    a("")
    a("Open `kmp/` in Android Studio and run `composeApp`, or from the CLI. Outside Android")
    a("Studio, point Gradle at your Android SDK first (`local.properties`: `sdk.dir=...`, or")
    a("`ANDROID_HOME`).")
    a("")
    a("```bash")
    a("cd kmp")
    a("./gradlew :composeApp:installDebug")
    a("```")
    a("")
    a("```powershell")
    a("cd kmp")
    a(".\\gradlew.bat :composeApp:installDebug   # Windows")
    a("```")
    a("")
    a("On Windows and Linux Gradle builds the Android app only; the iOS targets are skipped")
    a("(`kotlin.native.ignoreDisabledTargets=true` keeps that quiet).")
    a("")
    a("### iOS")
    a("")
    a("iOS needs a Mac with Xcode. A project generated on Windows can be copied to a Mac as is.")
    a("")
    a(f"Open `kmp/iosApp/{ctx.target_name}.xcodeproj` in Xcode and press Run. The first build")
    a("takes a few minutes: the *Compile Kotlin Framework* phase builds the shared code through")
    a("Gradle. From the CLI:")
    a("")
    a("```bash")
    a("cd kmp/iosApp")
    a(f"xcodebuild -project {ctx.target_name}.xcodeproj -scheme {ctx.target_name} \\")
    a('  -destination "generic/platform=iOS Simulator" build')
    a("```")
    a("")
    if not ctx.team:
        a("Signing is Automatic with no team: pick yours under Signing & Capabilities before")
        a("running on a phone, or regenerate with `--team <TEAM_ID>`. The simulator needs none.")
        a("")
    a("Requires JDK 17 and Xcode 15+.")
    a("")
    a("### Things that are easy to get wrong")
    a("")
    a("- **Keep the *Compile Kotlin Framework* build phase first.** It builds `ComposeApp.framework`")
    a("  and copies the SDK's resources into the app (`syncComposeResourcesForIos`); without it")
    a("  Swift cannot `import ComposeApp`, or the SDK UI renders without its assets.")
    a("- **Keep `EXCLUDED_ARCHS[sdk=iphonesimulator*] = x86_64`.** The SDK's simulator slice is")
    a("  arm64-only.")
    a("- **Keep the `NSBluetooth*` keys in `Info.plist`.** iOS terminates the app on first BLE use")
    a("  without them. There is no runtime permission call to write on iOS; Android asks in")
    a("  `MainActivity`, before scanning, because a denied scan fails silently - it just finds")
    a("  nothing.")
    a("")
    a("---")
    a("")
    a("## 4. Running it")
    a("")
    a("1. **Init SDK** - nothing works before this.")
    a("2. **Configure glasses** - the SDK's own pairing screen. Grant Bluetooth when asked. This is")
    a("   also where you pick the glasses simulator if you have no glasses.")
    a("3. **Connect** - watch the status. `ready` means connected *and* able to render.")
    a("4. The HUD appears on `ready`; **Add/Remove HUD** toggles it.")
    a("")
    a("---")
    a("")
    a("## 5. Where things are")
    a("")
    a("```")
    a("kmp/")
    a("  settings.gradle.kts                          the GitHub Packages repository")
    a(f"  gradle.properties                            sdkVersion={ctx.sdk_version}")
    a("  composeApp/build.gradle.kts                  targets + the SDK dependency")
    a(f"  composeApp/src/commonMain/kotlin/{pkg_dir}/App.kt          the whole SDK flow + the HUD")
    a(f"  composeApp/src/androidMain/kotlin/{pkg_dir}/MainActivity.kt   init + BLE permissions")
    a(f"  composeApp/src/iosMain/kotlin/{pkg_dir}/MainViewController.kt the iOS entry point")
    a("  composeApp/src/androidMain/assets/           <- sdk.key (Android)")
    a(f"  iosApp/{ctx.target_name}.xcodeproj/")
    a(f"  iosApp/{ctx.target_name}/                    <- sdk.key (iOS)")
    a("```")
    a("")
    a("Change the SDK version with `-PsdkVersion=X.Y.Z`, `MAV2_SDK_VERSION`, or `gradle.properties`.")
    a("")
    a("## Next")
    a("")
    a("| | |")
    a("|---|---|")
    a(f"| Full KMP sample (UI kit, animation, audio, AI vision, sensors, video) | {SAMPLES_REPO_URL}/kmp-compose-sample |")
    a("| Developer portal and API reference | https://everysight.github.io/maverick-ai-docs |")
    a("| SDK documentation MCP (for AI assistants) | https://everysight.github.io/maverick-ai-docs/getting-started/vibe-coding/ |")
    a("")
    a("`M2Screen` is a retained scene, not an immediate-mode canvas: build the drawables once in")
    a("`onCreate()` and then mutate them. Do not rebuild the screen per frame.")
    a("")
    write(root / "README.md", "\n".join(L))


def generate_readme(root: Path, ctx: Context, platforms: set[str], version_source: str) -> None:
    android = "android" in platforms
    ios = "ios" in platforms
    L: list[str] = []
    a = L.append

    a(f"# {ctx.name}")
    a("")
    a("A Maverick AI SDK hello world, generated by `evs-mav2-sdk-quickstart`.")
    a("")
    a(f"- SDK version: **{ctx.sdk_version}** ({version_source})")
    a(f"- Platforms: **{', '.join(sorted(platforms))}**")
    a("")
    a("Each app does the same five things, in this order: init the SDK, get BLE")
    a("permission, open the SDK's pairing UI, connect, then draw a HUD on the glasses.")
    a("")
    a("---")
    a("")
    a("## 1. Get your SDK key")
    a("")
    a("Everysight issues a key file - a JWT scoped to your application namespace. The SDK")
    a("does **not** need it to initialize; it needs it to **connect**. `init()` succeeds")
    a("without a key and the connection then ends in `M2ConnectionStatus.AuthFailed`, so a")
    a("successful init proves nothing about the key.")
    a("")
    a("On first connect the key is exchanged online for a certificate tied to your app and")
    a("to that pair of glasses, so that call needs network. The SDK checks, in order: a key")
    a("you set with `Evs.setDeveloperKey()`, then `app.key`, then `sdk.<serial>.key`, then")
    a("`sdk.key`. Bundling `sdk.key` is the simplest option and is what these projects")
    a("expect. It is a secret: the generated `.gitignore` files already exclude it.")
    a("")
    a("| Platform | Put `sdk.key` here | How it is loaded |")
    a("|---|---|---|")
    if android:
        a("| Android | `android/app/src/main/assets/sdk.key` | read from the APK assets at `Evs.init()` |")
    if ios:
        a(f"| iOS | `ios/{ctx.target_name}/sdk.key` | already wired as a bundle resource; read at `Evs.shared.doInit()` |")
    a("")
    a("Each of those directories has a `PUT-SDK-KEY-HERE.txt` reminder. Delete it once the")
    a("key is in place.")
    a("")
    a("If connecting ends in `AuthFailed` or an illegal-app error, the cause is almost")
    a("always one of: the file is missing, it is misnamed, it is in the wrong directory, or")
    a("it was issued for a different application namespace. So keep the ids below")
    a("unchanged, or ask for a key that matches your own.")
    a("")
    a("Both projects fail the **build** if no key is bundled, so this cannot be forgotten")
    a("silently. On Android `app.key` is accepted in place of `sdk.key`; the iOS project")
    a("wires the resource by filename, so rename an `app.key` to `sdk.key` or add it to the")
    a("target yourself.")
    a("")
    if android:
        a(f"- Android `applicationId`: `{ctx.package_name}`")
    if ios:
        a(f"- iOS `PRODUCT_BUNDLE_IDENTIFIER`: `{ctx.bundle_id}`")
    a("")

    if android:
        a("---")
        a("")
        a("## 2. Android")
        a("")
        a("### Authenticate to GitHub Packages (one time, per machine)")
        a("")
        a("The SDK is published to GitHub Packages, which requires a token **even though the")
        a("packages are public**. Create a classic personal access token with the")
        a("`read:packages` scope, then put it in `~/.gradle/gradle.properties`")
        a("(`%USERPROFILE%\\.gradle\\gradle.properties` on Windows) — your home directory, not")
        a("the project, so it never lands in git:")
        a("")
        a("```properties")
        a("gpr.user=<your-github-username>")
        a("gpr.key=<token-with-read:packages>")
        a("```")
        a("")
        a("Without it the build fails resolving")
        a("`com.everysight.mav2:maverick-ai-sdk-android` with a 401.")
        a("")
        a("### Build and run")
        a("")
        a("Open `android/` in Android Studio and press Run — it will provision the Gradle")
        a("wrapper on first open. From the CLI, generate the wrapper once, then build:")
        a("")
        a("```bash")
        a("cd android")
        a(f"gradle wrapper --gradle-version {GRADLE_VERSION}")
        a("./gradlew :app:installDebug")
        a("```")
        a("")
        a("Windows (PowerShell):")
        a("")
        a("```powershell")
        a("cd android")
        a(f"gradle wrapper --gradle-version {GRADLE_VERSION}")
        a(".\\gradlew.bat :app:installDebug")
        a("```")
        a("")
        a("Requires JDK 17. The debug build uses the `-debug` SDK artifact, which carries the")
        a("SDK's own debug UI and logging; release builds use the plain artifact.")
        a("")
        a("### Where things are")
        a("")
        a("```")
        a("android/")
        a("  settings.gradle.kts                      the GitHub Packages repository")
        a("  gradle.properties                        sdkVersion=" + ctx.sdk_version)
        a("  app/build.gradle.kts                     the two SDK dependencies")
        a("  app/src/main/AndroidManifest.xml         BLE permissions")
        a("  app/src/main/assets/                     <- sdk.key goes here")
        a(f"  app/src/main/java/{ctx.package_name.replace('.', '/')}/MainActivity.kt")
        a("```")
        a("")
        a("Change the SDK version with `-PsdkVersion=X.Y.Z`, the `MAV2_SDK_VERSION`")
        a("environment variable, or by editing `gradle.properties`.")
        a("")

    if ios:
        a("---")
        a("")
        a(f"## {'3' if android else '2'}. iOS")
        a("")
        a("No token needed — the Swift Package repository is public. Needs a Mac with Xcode; a")
        a("project generated on Windows can be copied to a Mac as is.")
        a("")
        a("### Build and run")
        a("")
        a(f"Open `ios/{ctx.target_name}.xcodeproj` and press Run. Xcode resolves the package")
        a("on first open. From the CLI:")
        a("")
        a("```bash")
        a("cd ios")
        a("xcodebuild \\")
        a(f"  -project {ctx.target_name}.xcodeproj \\")
        a(f"  -scheme {ctx.target_name} \\")
        a("  -configuration Debug \\")
        a('  -destination "generic/platform=iOS Simulator" \\')
        a("  build")
        a("```")
        a("")
        if not ctx.team:
            a("Signing is set to Automatic with no team. Pick your team in Xcode under")
            a("Signing & Capabilities before running on a device (the simulator builds")
            a("without one). Re-run the generator with `--team <TEAM_ID>` to bake it in.")
            a("")
        a("### How the package is pinned")
        a("")
        if ctx.spm_pin == "version":
            a(f"The project pins the Swift Package to **upToNextMinor from {ctx.sdk_version}**, so a")
            a("patch or minor SDK release is picked up but a major one is not. Change it in Xcode")
            a("under the package dependency, or regenerate with a different `--sdk-version`.")
        else:
            a("The project tracks the Swift Package's **`main` branch** rather than a version.")
            a("That picks up unreleased SDK changes, which also means an unrelated change can")
            a("break your build. Prefer a version pin unless you specifically need main:")
            a("regenerate with `--spm-pin version`.")
        a("")
        a("### Things that are easy to get wrong")
        a("")
        a("- **The `Copy Maverick AI Compose Resources` build phase must stay.** The SDK ships")
        a("  its images and fonts inside the framework bundle, where iOS will not look for")
        a("  them; the phase rsyncs them up into the `.app`. Delete it and SDK UI renders")
        a("  without its assets.")
        a("- **`EXCLUDED_ARCHS[sdk=iphonesimulator*] = x86_64` must stay.** The SDK's")
        a("  simulator slice is arm64-only, so an Intel slice fails to link.")
        a("- **The `NSBluetooth*` keys in `Info.plist` must stay.** iOS terminates the app on")
        a("  first BLE use without them. There is no runtime permission request to write on")
        a("  iOS: the system prompts on its own.")
        a("")
        a("### Where things are")
        a("")
        a("```")
        a("ios/")
        a(f"  {ctx.target_name}.xcodeproj/                pinned to SDK {ctx.sdk_version} (upToNextMinor)")
        a(f"  {ctx.target_name}/Info.plist               BLE usage strings")
        a(f"  {ctx.target_name}/ViewController.swift     the whole SDK flow + the HUD")
        a(f"  {ctx.target_name}/                         <- sdk.key goes here")
        a("```")
        a("")

    step = 4 if (android and ios) else 3
    a("---")
    a("")
    a(f"## {step}. Running it")
    a("")
    a("1. Put `sdk.key` in place (step 1) and launch the app.")
    a("2. **Init SDK** — nothing works before this.")
    a("3. **Configure glasses** — opens the SDK's own pairing screen. Grant Bluetooth when")
    a("   asked. This is also where you select the glasses simulator if you have one.")
    a("4. **Connect** — watch the status line. `ready` means connected *and* able to render.")
    a("5. The HUD appears automatically on `ready`; **Add/Remove HUD** toggles it.")
    a("")
    a("A HUD only appears while the glasses report `ready`. If the status stops at")
    a("`connecting`, the glasses are not paired, are out of range, or are connected to")
    a("another phone.")
    a("")
    a("---")
    a("")
    a(f"## {step + 1}. Where to go next")
    a("")
    a("| | |")
    a("|---|---|")
    a(f"| Full samples (sensors, AR, audio, OTA) | {SAMPLES_REPO_URL} |")
    a(f"| iOS Swift Package | https://github.com/{IOS_SPM_REPO} |")
    a(f"| Android Maven repository | https://github.com/{ANDROID_MAVEN_REPO} |")
    a("| Developer portal and API reference | https://everysight.github.io/maverick-ai-docs |")
    a("")
    a("The samples repository is the next stop after this project: `android-native` and")
    a("`ios-native` are the fuller versions of what you have here, and `kmp-compose-sample`")
    a("is the full Compose Multiplatform app.")
    a("")
    a("### The API surface you just used")
    a("")
    a("| Call | Does |")
    a("|---|---|")
    a("| `Evs.init(context)` / `Evs.shared.doInit()` | start the SDK; everything else needs it |")
    a("| `Evs.showAppUI(DefaultConfigure)` | the SDK's pairing UI |")
    a("| `Evs.showAppUI(DefaultAdjust)` | display-position adjustment UI |")
    a("| `Evs.glassesService.connect()` / `.disconnect()` | connection control |")
    a("| `Evs.glassesService.registerConnectionListener(l)` | connection state, incl. `onReady` |")
    a("| `Evs.screenService.addScreen(s)` / `.removeScreen(s)` | show/hide a HUD |")
    a("| `M2Screen` + `M2Text` / `M2RectFilled` / `M2RectOutline` | retained drawables |")
    a("")
    a("`M2Screen` is a retained scene, not an immediate-mode canvas: build the drawables")
    a("once in `onCreate()` and then mutate them. Do not rebuild the screen per frame.")
    a("")
    write(root / "README.md", "\n".join(L))


# ── main ─────────────────────────────────────────────────────────────────────

def main(argv: list[str]) -> int:
    p = argparse.ArgumentParser(
        prog="new_mav2_project.py",
        description="Generate a Maverick AI SDK hello-world project: Compose Multiplatform "
                    "(default) or native Android and/or iOS.",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="example:\n"
               "  ./new_mav2_project.py --name HelloMav2 --out ~/dev                    # KMP\n"
               "  ./new_mav2_project.py --name HelloMav2 --platform both --out ~/dev    # native\n",
    )
    p.add_argument("--name", default="HelloMav2",
                   help="project name, letters and digits (default: HelloMav2)")
    p.add_argument("--platform", choices=["kmp", "android", "ios", "both"], default="kmp",
                   help="kmp (default): one Compose Multiplatform project for Android + iOS. "
                        "android / ios / both: native projects")
    p.add_argument("--out", default=".",
                   help="parent directory for the generated project (default: .)")
    p.add_argument("--package", help="Android applicationId (default: com.example.<name>)")
    p.add_argument("--bundle-id", help="iOS bundle identifier (default: same as --package)")
    p.add_argument("--sdk-version",
                   help="SDK version to pin (default: look up the latest release)")
    p.add_argument("--team", default="",
                   help="Apple Developer team ID for iOS signing (default: unset)")
    p.add_argument("--spm-pin", choices=["branch", "version"], default="version",
                   help="how the iOS Swift Package is referenced. 'version' (default) pins "
                        "upToNextMinor from --sdk-version; 'branch' tracks main and picks up "
                        "SDK changes as they land")
    p.add_argument("--offline", action="store_true",
                   help="skip the version lookup; requires --sdk-version")
    p.add_argument("--force", action="store_true",
                   help="overwrite the target directory if it exists")
    ns = p.parse_args(argv)

    name = validate_name(ns.name)
    package = validate_package(ns.package or default_package(name), "--package")
    bundle_id = validate_package(ns.bundle_id or package, "--bundle-id")

    if ns.offline and not ns.sdk_version:
        raise Fail("--offline requires --sdk-version")
    if ns.sdk_version:
        version, version_source = ns.sdk_version, "pinned with --sdk-version"
    else:
        version, version_source = latest_sdk_version()

    platforms = {"android", "ios"} if ns.platform == "both" else {ns.platform}
    kmp = ns.platform == "kmp"

    root = Path(ns.out).expanduser().resolve() / name
    if root.exists():
        if not ns.force:
            raise Fail(f"{root} already exists (use --force to overwrite)")
        shutil.rmtree(root)

    ctx = Context(
        name=name,
        project_slug=slugify(name),
        app_label=name,
        package_name=package,
        bundle_id=bundle_id,
        target_name=name,
        log_tag=(name[:23] if len(name) > 23 else name),   # Android tag limit
        sdk_version=version,
        team=ns.team,
        spm_pin=ns.spm_pin,
    )

    if kmp:
        generate_kmp(root / "kmp", ctx)
        generate_readme_kmp(root, ctx, version_source)
    else:
        if "android" in platforms:
            generate_android(root / "android", ctx)
        if "ios" in platforms:
            generate_ios(root / "ios", ctx)
        generate_readme(root, ctx, platforms, version_source)

    rel = os.path.relpath(root, Path.cwd()) if str(root).startswith(str(Path.cwd())) else root
    print(f"Created {rel}")
    print(f"  SDK version : {version}  ({version_source})")
    print(f"  platforms   : {'kmp (Android + iOS)' if kmp else ', '.join(sorted(platforms))}")
    if kmp or "android" in platforms:
        print(f"  android id  : {package}")
    if kmp:
        print(f"  ios bundle  : {bundle_id}")
    if "ios" in platforms:
        print(f"  ios bundle  : {bundle_id}")
        pin = "branch main" if ns.spm_pin == "branch" else f"upToNextMinor from {version}"
        print(f"  ios package : {pin}")
    print()
    print("Next:")
    print(f"  1. read {Path(rel) / 'README.md'}")
    print("  2. put your sdk.key where the README says")
    if kmp or "android" in platforms:
        props = r"%USERPROFILE%\.gradle\gradle.properties" if os.name == "nt" else "~/.gradle/gradle.properties"
        print(f"  3. add gpr.user / gpr.key to {props} (GitHub Packages token)")
    if kmp:
        print(f"  4. in {Path(rel) / 'kmp'}: gradle wrapper --gradle-version {GRADLE_VERSION}")
        if os.name == "nt":
            print("     (iOS needs a Mac with Xcode - copy the project there to build it)")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main(sys.argv[1:]))
    except Fail as exc:
        print(exc, file=sys.stderr)
        raise SystemExit(1)
