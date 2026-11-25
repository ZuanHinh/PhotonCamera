package com.hinnka.mycamera.camera

import com.hinnka.mycamera.utils.PLog
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

object PreviewHeartbeat {
    private val results = AtomicInteger(0)
    private val surfaceFrames = AtomicInteger(0)
    private val lastActivity = AtomicLong(0L)
    @Volatile private var started = false

    private fun ensureStarted() {
        if (started) return
        synchronized(this) {
            if (started) return
            started = true
            Thread {
                while (true) {
                    try { Thread.sleep(1000) } catch (_: InterruptedException) { return@Thread }
                    val r = results.getAndSet(0)
                    val s = surfaceFrames.getAndSet(0)
                    if (System.currentTimeMillis() - lastActivity.get() < 15000) {
                        PLog.d("PreviewHeartbeat", "per second: hal_results=$r, surface_frames=$s")
                    }
                }
            }.apply { isDaemon = true; start() }
        }
    }

    fun tick() {
        ensureStarted()
        lastActivity.set(System.currentTimeMillis())
        results.incrementAndGet()
    }

    fun surfaceFrame() {
        ensureStarted()
        lastActivity.set(System.currentTimeMillis())
        surfaceFrames.incrementAndGet()
    }
}
