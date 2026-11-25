package com.hinnka.mycamera.camera

import android.util.Size
import com.hinnka.mycamera.utils.PLog

object CapCompat {
    private const val PATH = "/sdcard/Android/data/com.hinnka.mycamera.debug/files/cap.txt"

    private fun capPixels(): Long? = try {
        val f = java.io.File(PATH)
        if (f.exists()) f.readText().trim().toFloatOrNull()?.let { (it * 1_000_000f).toLong() } else null
    } catch (_: Throwable) { null }

    fun active(): Boolean = capPixels() != null

    fun limit(sizes: Array<Size>): Array<Size> {
        val cap = capPixels() ?: return sizes
        val filtered = sizes.filter { it.width.toLong() * it.height <= cap }
        if (filtered.isEmpty()) return sizes
        PLog.d("CapCompat", "cap=$cap px -> ${filtered.size}/${sizes.size} sizes")
        return filtered.toTypedArray()
    }
}
