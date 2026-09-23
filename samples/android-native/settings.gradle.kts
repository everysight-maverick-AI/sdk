pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
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

rootProject.name = "android-native"
include(":app")
