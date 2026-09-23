/*
 * Created by Everysight LTD.
 *
 * Android host for the Maverick AI KMP Compose sample. The host owns platform
 * SDK initialization, Android BLE permissions, and the SDK resource resolver,
 * while the shared Compose UI owns the sample flow.
 */

package com.everysight.samples.kmpcompose

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.everysight.mav2.sdk.Evs
import com.everysight.mav2.sdk.init
import com.everysight.mav2.sdk.services.IM2ResourcesResolver
import com.everysight.samples.kmpcompose.generated.resources.Res
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.ExperimentalResourceApi

/** Android entry point for the shared KMP Compose sample. */
class MainActivity : ComponentActivity() {
    private var pendingAfterPermission: (() -> Unit)? = null

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grantResults ->
        val granted = grantResults.values.all { it }
        val action = pendingAfterPermission
        pendingAfterPermission = null
        if (granted) {
            action?.invoke()
        } else {
            Toast.makeText(
                this,
                "Bluetooth is required to scan for glasses, and the camera for the Video sample.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Canonical KMP resolver: bridges the SDK's name-based lookup to Compose
     * Multiplatform's `Res.readBytes("files/$path")`. Required on KMP because
     * Compose-MP packages app files at `composeResources/<app-pkg>.generated.resources/files/...`,
     * which the SDK is a library and cannot construct on its own. See
     * `maverick-ai-docs/src/guides/resources.md`.
     */
    @OptIn(ExperimentalResourceApi::class)
    private fun installResourcesResolver() {
        Evs.resourcesService.setResourcesResolver(object : IM2ResourcesResolver {
            override fun loadResource(path: String): ByteArray? = try {
                runBlocking { Res.readBytes("files/$path") }
            } catch (_: Exception) {
                null
            }
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ask at startup, not only when a button first needs the hardware. Neither a denied
        // BLUETOOTH_SCAN nor a denied CAMERA raises anything - the scan comes back empty and the
        // video encoder produces no frames - so a user who never saw the prompt concludes the
        // glasses are broken. The per-action gates below stay, as the backstop for a permission
        // revoked while the app is running.
        ensurePermissionsThen(requiredBlePermissions() + Manifest.permission.CAMERA) { }
        val resolvedDependencies = readResolvedDependencyVersions(this)
        setContent {
            App(
                initSdk = {
                    Evs.init(applicationContext)
                    installResourcesResolver()
                },
                ensureBlePermissionsThen = { action ->
                    ensureBlePermissionsThen(action)
                },
                resolvedDependencies = resolvedDependencies
            )
        }
    }

    private fun ensureBlePermissionsThen(action: () -> Unit) =
        ensurePermissionsThen(requiredBlePermissions(), action)

    private fun ensurePermissionsThen(permissions: List<String>, action: () -> Unit) {
        val missing = permissions.filter { permission ->
            ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) {
            action()
            return
        }
        pendingAfterPermission = action
        permissionLauncher.launch(missing.toTypedArray())
    }

    private fun requiredBlePermissions(): List<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            listOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }
}
