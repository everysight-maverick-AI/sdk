/*
 * Created by Everysight LTD.
 */

package com.everysight.samples.kmpcompose

import androidx.compose.ui.graphics.ImageBitmap

/** On iOS the SDK hands out a fresh bitmap per frame, so the frame itself can be kept. */
actual fun ownedCopyOf(image: ImageBitmap): ImageBitmap = image
