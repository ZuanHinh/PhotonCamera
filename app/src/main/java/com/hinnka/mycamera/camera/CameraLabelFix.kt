package com.hinnka.mycamera.camera

object CameraLabelFix {
    private const val ULTRAWIDE_MM = 14f

    fun adjust(computed: Float, isBack: Boolean): Float {
        val maker = android.os.Build.MANUFACTURER
        val huawei = maker.equals("HUAWEI", true) || maker.equals("HONOR", true)
        return if (huawei && isBack && computed in 8f..13f) ULTRAWIDE_MM else computed
    }
}
