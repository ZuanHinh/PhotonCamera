package com.hinnka.mycamera.camera

import com.hinnka.mycamera.utils.PLog

object PreviewHeartbeat {
    private var last = 0L
    private var n = 0

    @Synchronized
    fun tick() {
        n++
        val now = System.currentTimeMillis()
        if (last == 0L) last = now
        if (now - last >= 1000) {
            PLog.d("PreviewHeartbeat", "completed preview frames in last second: $n")
            n = 0
            last = now
        }
    }
}
