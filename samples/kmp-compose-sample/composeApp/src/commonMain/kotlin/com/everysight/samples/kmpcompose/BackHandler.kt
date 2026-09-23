/*
 * Created by Everysight LTD.
 *
 * A full-screen overlay is not a navigation destination: it is a boolean in composition. Android
 * does not know that, so the system back gesture went to the activity and closed the app instead
 * of the page. Anything that covers the sample declares itself here while it is up.
 */

package com.everysight.samples.kmpcompose

import androidx.compose.runtime.Composable

/**
 * Intercepts the platform back gesture while [enabled].
 *
 * Android routes the predictive-back gesture and the back button here. iOS has no equivalent
 * system gesture for a modally presented view, so the actual is a no-op and the screen's own
 * Close button remains the way out.
 */
@Composable
expect fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit)
