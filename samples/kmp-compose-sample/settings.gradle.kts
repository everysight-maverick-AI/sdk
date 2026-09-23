pluginManagement {
    // pluginManagement.plugins{} is parsed in a separate early-evaluation
    // phase, so the version expression must be self-contained here (top-level
    // settings vals are not in scope yet).
    val sdkVersion: String = providers.gradleProperty("sdkVersion")
        .orElse(providers.environmentVariable("MAV2_SDK_VERSION"))
        .orNull
        ?: error("sdkVersion is not set. Provide it via gradle.properties (sdkVersion=X.Y.Z), -PsdkVersion=X.Y.Z, or the MAV2_SDK_VERSION env var. The build_system auto-syncs gradle.properties on each release.")

    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        // The com.everysight.mav2.sdk-ios plugin is published to GitHub Packages, next to the SDK.
        maven {
            url = uri("https://maven.pkg.github.com/everysight-maverick-AI/mav-ai-android-maven")
            // gpr.user / gpr.key from ~/.gradle/gradle.properties, or GITHUB_TOKEN / GH_TOKEN
            // (with GITHUB_ACTOR / GITHUB_USERNAME) from the environment - the same pair CI uses.
            val githubPackagesToken = providers.gradleProperty("gpr.key")
                .orElse(providers.environmentVariable("GITHUB_TOKEN"))
                .orElse(providers.environmentVariable("GH_TOKEN"))
            val githubPackagesUser = providers.gradleProperty("gpr.user")
                .orElse(providers.environmentVariable("GITHUB_ACTOR"))
                .orElse(providers.environmentVariable("GITHUB_USERNAME"))
                .orElse(githubPackagesToken.map { "x-access-token" })
            if (githubPackagesToken.isPresent) {
                credentials {
                    username = githubPackagesUser.get()
                    password = githubPackagesToken.get()
                }
            }
        }
    }
    plugins {
        id("com.everysight.mav2.sdk-ios") version sdkVersion
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven {
            url = uri("https://maven.pkg.github.com/everysight-maverick-AI/mav-ai-android-maven")
            // gpr.user / gpr.key from ~/.gradle/gradle.properties, or GITHUB_TOKEN / GH_TOKEN
            // (with GITHUB_ACTOR / GITHUB_USERNAME) from the environment - the same pair CI uses.
            val githubPackagesToken = providers.gradleProperty("gpr.key")
                .orElse(providers.environmentVariable("GITHUB_TOKEN"))
                .orElse(providers.environmentVariable("GH_TOKEN"))
            val githubPackagesUser = providers.gradleProperty("gpr.user")
                .orElse(providers.environmentVariable("GITHUB_ACTOR"))
                .orElse(providers.environmentVariable("GITHUB_USERNAME"))
                .orElse(githubPackagesToken.map { "x-access-token" })
            if (githubPackagesToken.isPresent) {
                credentials {
                    username = githubPackagesUser.get()
                    password = githubPackagesToken.get()
                }
            }
        }
    }
}

rootProject.name = "kmp-compose-sample"
include(":composeApp")
