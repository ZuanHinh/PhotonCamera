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
cat > ~/wb.sed << 'EOF'
/val resolvedAnchor = anchor ?: return applyAutoWhiteBalanceSettings(/i\
        if (android.os.Build.MANUFACTURER.equals("HUAWEI", true) || android.os.Build.MANUFACTURER.equals("HONOR", true)) {\
            builder.set(CaptureRequest.CONTROL_AWB_MODE, WbCompat.presetMode(state.awbTemperature.toFloat(), availableAwbModes))\
            builder.set(CaptureRequest.CONTROL_AWB_LOCK, false)\
            builder.set(CaptureRequest.COLOR_CORRECTION_MODE, CameraMetadata.COLOR_CORRECTION_MODE_FAST)\
            return\
        }
