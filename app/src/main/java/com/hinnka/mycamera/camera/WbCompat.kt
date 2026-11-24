package com.hinnka.mycamera.camera

import android.hardware.camera2.CameraMetadata

object WbCompat {
    fun presetMode(kelvin: Float, available: IntArray): Int {
        val presets = listOf(
            2700f to CameraMetadata.CONTROL_AWB_MODE_INCANDESCENT,
            3800f to CameraMetadata.CONTROL_AWB_MODE_WARM_FLUORESCENT,
            4500f to CameraMetadata.CONTROL_AWB_MODE_FLUORESCENT,
            5500f to CameraMetadata.CONTROL_AWB_MODE_DAYLIGHT,
            6500f to CameraMetadata.CONTROL_AWB_MODE_CLOUDY_DAYLIGHT,
            7500f to CameraMetadata.CONTROL_AWB_MODE_TWILIGHT,
            8000f to CameraMetadata.CONTROL_AWB_MODE_SHADE
        ).filter { available.contains(it.second) }
        return presets.minByOrNull { kotlin.math.abs(it.first - kelvin) }?.second
            ?: CameraMetadata.CONTROL_AWB_MODE_AUTO
    }
}
