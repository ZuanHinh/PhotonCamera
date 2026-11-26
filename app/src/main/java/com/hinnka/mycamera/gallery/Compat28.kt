package com.hinnka.mycamera.gallery

import android.os.Environment
import java.io.File

object Compat28 {
    fun legacyDataPath(relativePath: String?, displayName: String): String {
        val rel = (relativePath ?: "DCIM/PhotonCamera").trim('/')
        val dir = File(Environment.getExternalStorageDirectory(), rel)
        dir.mkdirs()
        return File(dir, displayName).absolutePath
    }
}
