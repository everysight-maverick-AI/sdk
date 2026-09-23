import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.application")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
    // Wires TensorFlowLiteC.xcframework (eye-tracker dependency) into iOS K/N
    // link + Xcode embed steps. Required only for iOS targets; no-op on Android.
    // Version is supplied centrally by settings.gradle.kts so a single
    // -PsdkVersion / MAV2_SDK_VERSION override re-points the SDK + plugin
    // together.
    id("com.everysight.mav2.sdk-ios")
}

val sdkVersion: String = providers.gradleProperty("sdkVersion")
    .orElse(providers.environmentVariable("MAV2_SDK_VERSION"))
    .orNull
    ?: error("sdkVersion is not set. Provide it via gradle.properties (sdkVersion=X.Y.Z), -PsdkVersion=X.Y.Z, or the MAV2_SDK_VERSION env var. The build_system auto-syncs gradle.properties on each release.")

mavericAiSdkIos {
    // Keep the helper-supplied TFLite version in lock-step with the SDK
    // version; the SDK and the iOS plugin are published together so they
    // always match.
    tfliteVersion.set(sdkVersion)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
    val iosTargets = listOf(
        iosArm64(),
        iosSimulatorArm64()
    )
    iosTargets.forEach { target ->
        target.binaries.framework {
            baseName = "ComposeApp"
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose:2.9.5")
            implementation("com.everysight.mav2:maverick-ai-sdk:$sdkVersion")
        }
        androidMain.dependencies {
            implementation(compose.preview)
            implementation("androidx.activity:activity-compose:1.10.1")
        }
    }
}

compose.resources {
    packageOfResClass = "com.everysight.samples.kmpcompose.generated.resources"
}

val resolvedDepsAssetsDir = layout.buildDirectory.dir("generated/resolved-deps/assets")

val generateResolvedDependencyVersions by tasks.registering {
    val outputFile = resolvedDepsAssetsDir.map { it.file("resolved-dependency-versions.properties") }
    outputs.file(outputFile)
    doLast {
        val targetFile = outputFile.get().asFile
        targetFile.parentFile.mkdirs()

        fun resolvedVersion(configurationName: String, group: String, module: String): String {
            val cfg = configurations.findByName(configurationName) ?: return "n/a (missing ${configurationName})"
            val component = cfg.incoming.resolutionResult.allComponents.firstOrNull { c ->
                val mv = c.moduleVersion ?: return@firstOrNull false
                mv.group == group && mv.name == module
            }
            return component?.moduleVersion?.version ?: "not-resolved"
        }

        val props = Properties().apply {
            setProperty("requested.sdkVersion", sdkVersion)
            setProperty(
                "resolved.com.everysight.mav2:maverick-ai-sdk",
                resolvedVersion("phoneDebugRuntimeClasspath", "com.everysight.mav2", "maverick-ai-sdk")
            )
            setProperty(
                "resolved.org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose",
                resolvedVersion("phoneDebugRuntimeClasspath", "org.jetbrains.androidx.lifecycle", "lifecycle-viewmodel-compose")
            )
            setProperty(
                "resolved.org.jetbrains.compose.runtime:runtime",
                resolvedVersion("phoneDebugRuntimeClasspath", "org.jetbrains.compose.runtime", "runtime")
            )
            setProperty(
                "resolved.org.jetbrains.compose.material3:material3",
                resolvedVersion("phoneDebugRuntimeClasspath", "org.jetbrains.compose.material3", "material3")
            )
            setProperty("plugin.com.everysight.mav2.sdk-ios", sdkVersion)
        }

        targetFile.outputStream().use { out ->
            props.store(out, "Generated from resolved phoneDebugRuntimeClasspath")
        }
    }
}

android {
    namespace = "com.everysight.samples.kmpcompose"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.everysight.samples.kmpcompose"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        manifestPlaceholders["sampleAppLabel"] = "MAV2 KMP Compose Sample"
    }

    flavorDimensions += "device"
    productFlavors {
        create("phone") {
            dimension = "device"
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    sourceSets["main"].assets.srcDir(resolvedDepsAssetsDir)
}

tasks.named("preBuild").configure {
    dependsOn(generateResolvedDependencyVersions)
}
