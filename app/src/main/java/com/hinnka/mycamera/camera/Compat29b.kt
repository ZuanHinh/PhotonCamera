package com.hinnka.mycamera.camera

import android.media.MediaMetadataRetriever

object Compat29b {
    inline fun <T> useRetriever(
        retriever: MediaMetadataRetriever,
        block: (MediaMetadataRetriever) -> T
    ): T {
        try {
            return block(retriever)
        } finally {
            retriever.release()
        }
    }
}
