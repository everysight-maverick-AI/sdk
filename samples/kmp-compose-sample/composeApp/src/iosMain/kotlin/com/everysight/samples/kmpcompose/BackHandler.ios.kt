/*
 * Created by Everysight LTD.
 */

package com.everysight.samples.kmpcompose

import androidx.compose.runtime.Composable

/** iOS has no system back gesture to intercept here; the Close button is the way out. */
@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
}
