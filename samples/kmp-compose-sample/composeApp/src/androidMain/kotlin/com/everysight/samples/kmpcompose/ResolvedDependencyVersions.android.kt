package com.everysight.samples.kmpcompose

import android.content.Context
import java.util.Properties

internal fun readResolvedDependencyVersions(context: Context): List<Pair<String, String>> {
    val props = Properties()
    return runCatching {
        context.assets.open("resolved-dependency-versions.properties").use { input ->
            props.load(input)
        }
        listOf(
            "requested sdkVersion" to (props.getProperty("requested.sdkVersion") ?: "missing"),
            "resolved maverick-ai-sdk" to (props.getProperty("resolved.com.everysight.mav2:maverick-ai-sdk") ?: "missing"),
            "resolved lifecycle-viewmodel-compose" to (props.getProperty("resolved.org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose") ?: "missing"),
            "resolved compose-runtime" to (props.getProperty("resolved.org.jetbrains.compose.runtime:runtime") ?: "missing"),
            "resolved compose-material3" to (props.getProperty("resolved.org.jetbrains.compose.material3:material3") ?: "missing"),
            "plugin com.everysight.mav2.sdk-ios" to (props.getProperty("plugin.com.everysight.mav2.sdk-ios") ?: "missing")
        )
    }.getOrElse {
        listOf("resolved dependencies" to "unavailable (${it.message ?: "asset read failed"})")
    }
}
