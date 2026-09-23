/*
 * Created by Everysight LTD.
 */

package com.everysight.samples.kmpcompose

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap

/** Android bitmaps are pooled by the SDK, so the preview needs its own pixels. */
actual fun ownedCopyOf(image: ImageBitmap): ImageBitmap {
    val source = image.asAndroidBitmap()
    return source.copy(source.config ?: Bitmap.Config.ARGB_8888, false).asImageBitmap()
}
