package com.example.zazeks.infra.camera

import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.zazeks.infra.ml.GameFrame
import javax.inject.Inject

/**
 * Factory that builds [ImageAnalysis.Analyzer] instances responsible for converting
 * CameraX frames into [GameFrame] DTOs.
 */
class CameraFrameAnalyzerFactory @Inject constructor(
    private val converter: ImageProxyToGameFrameConverter
) {

    fun create(onFrame: (GameFrame) -> Unit): ImageAnalysis.Analyzer {
        return ImageAnalysis.Analyzer { image: ImageProxy ->
            try {
                val frame = converter.convert(image)
                if (frame != null) {
                    onFrame(frame)
                }
            } catch (throwable: Throwable) {
                Log.e(TAG, "Failed to process camera frame", throwable)
            } finally {
                image.close()
            }
        }
    }

    companion object {
        private const val TAG = "CameraFrameAnalyzer"
    }
}
