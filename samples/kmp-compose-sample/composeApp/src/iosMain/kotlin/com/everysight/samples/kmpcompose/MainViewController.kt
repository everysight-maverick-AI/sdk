/*
 * Created by Everysight LTD.
 *
 * iOS host bridge for the Maverick AI KMP Compose sample. Installs the
 * canonical KMP resource resolver that bridges the SDK to Compose
 * Multiplatform's `Res.readBytes("files/$path")`. See
 * maverick-ai-docs/src/guides/resources.md.
 */

package com.everysight.samples.kmpcompose

import androidx.compose.ui.window.ComposeUIViewController
import com.everysight.mav2.sdk.Evs
import com.everysight.mav2.sdk.init
import com.everysight.mav2.sdk.services.IM2ResourcesResolver
import com.everysight.samples.kmpcompose.generated.resources.Res
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.ExperimentalResourceApi
import platform.UIKit.UIColor
import platform.UIKit.UIViewController

/** Creates the UIKit view controller that hosts the shared Compose sample UI. */
@OptIn(ExperimentalResourceApi::class)
fun MainViewController(): UIViewController {
    fun initSdkWithResolver() {
        Evs.init()
        Evs.resourcesService.setResourcesResolver(object : IM2ResourcesResolver {
            override fun loadResource(path: String): ByteArray? = try {
                runBlocking { Res.readBytes("files/$path") }
            } catch (_: Exception) {
                null
            }
        })
    }

    return ComposeUIViewController {
        App(
            initSdk = { initSdkWithResolver() },
            ensureBlePermissionsThen = { action -> action() },
            resolvedDependencies = emptyList()
        )
    }.apply {
        view.backgroundColor = UIColor(
            red = 17.0 / 255.0,
            green = 23.0 / 255.0,
            blue = 35.0 / 255.0,
            alpha = 1.0
        )
    }
}
