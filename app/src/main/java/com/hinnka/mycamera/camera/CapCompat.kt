package com.hinnka.mycamera.camera

import android.content.Context
import android.util.Size
import com.hinnka.mycamera.MyCameraApplication
import com.hinnka.mycamera.utils.PLog

object CapCompat {
    private const val PREFS = "photon_compat"
    private const val KEY_MP = "photo_cap_mp"

    fun getMp(context: Context): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_MP, 0)

    fun setMp(context: Context, mp: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt(KEY_MP, mp).apply()
    }

    private fun capPixels(): Long? = try {
        val mp = getMp(MyCameraApplication.instance)
        if (mp > 0) mp * 1_000_000L else null
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
