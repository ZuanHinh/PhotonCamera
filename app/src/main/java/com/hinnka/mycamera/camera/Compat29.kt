package com.hinnka.mycamera.camera

import android.graphics.Bitmap
import android.graphics.Color
import android.hardware.camera2.CaptureResult
import android.os.Build

object Compat29 {
    fun activePhysicalId(result: CaptureResult?): String? {
        if (result == null) return null
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        return result.get(CaptureResult.LOGICAL_MULTI_CAMERA_ACTIVE_PHYSICAL_ID)
    }

    fun pixelColor(bitmap: Bitmap, x: Int, y: Int): Color =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            bitmap.getColor(x, y)
        } else {
            Color.valueOf(bitmap.getPixel(x, y))
        }
}
