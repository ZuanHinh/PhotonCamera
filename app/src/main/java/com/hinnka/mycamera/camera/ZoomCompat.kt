package com.hinnka.mycamera.camera

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.os.Build
import android.util.Range

object ZoomCompat {
    private val ok = Build.VERSION.SDK_INT >= 30

    fun range(c: CameraCharacteristics): Range<Float>? =
        if (ok) c.get(CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE) else null

    fun requestZoom(b: CaptureRequest.Builder): Float? =
        if (ok) b.get(CaptureRequest.CONTROL_ZOOM_RATIO) else null

    fun setRequestZoom(b: CaptureRequest.Builder, v: Float) {
        if (ok) b.set(CaptureRequest.CONTROL_ZOOM_RATIO, v)
    }

    fun resultZoom(r: CaptureResult): Float? =
        if (ok) r.get(CaptureResult.CONTROL_ZOOM_RATIO) else null
}
