import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val sdkVersion: String = providers.gradleProperty("sdkVersion")
    .orElse(providers.environmentVariable("MAV2_SDK_VERSION"))
    .orNull
    ?: error("sdkVersion is not set. Provide it via gradle.properties (sdkVersion=X.Y.Z), -PsdkVersion=X.Y.Z, or the MAV2_SDK_VERSION env var. The build_system auto-syncs gradle.properties on each release.")

android {
    namespace = "com.everysight.samples.androidnative"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.everysight.samples.androidnative"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.05.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.10.1")

    // SDK via GitHub Packages repository:
    // https://maven.pkg.github.com/everysight-maverick-AI/mav-ai-android-maven
    debugImplementation("com.everysight.mav2:maverick-ai-sdk-android-debug:$sdkVersion")
    releaseImplementation("com.everysight.mav2:maverick-ai-sdk-android:$sdkVersion")
}
