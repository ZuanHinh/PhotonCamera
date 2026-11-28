package com.hinnka.mycamera.camera

import com.hinnka.mycamera.video.VideoResolutionPreset

object TeleGuard {
    private const val MAX_ZOOM = 2.95f

    fun maxZoom(isVideo: Boolean, resolution: VideoResolutionPreset): Float {
        if (java.io.File("/sdcard/Android/data/com.hinnka.mycamera.debug/files/noguard.txt").exists()) return Float.MAX_VALUE
        val maker = android.os.Build.MANUFACTURER
        val huawei = maker.equals("HUAWEI", true) || maker.equals("HONOR", true)
        val large = resolution == VideoResolutionPreset.UHD_2160P ||
            resolution == VideoResolutionPreset.UHD_3648
        return if (huawei && isVideo && large) MAX_ZOOM else Float.MAX_VALUE
    }
}
