package com.example.zazeks.infra.camera

import android.content.Context
import android.view.Surface
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Binds CameraX use-cases to the lifecycle and exposes a handle for releasing resources.
 */
class CameraSessionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    suspend fun bind(
        previewView: PreviewView,
        lifecycleOwner: LifecycleOwner,
        analyzer: ImageAnalysis.Analyzer
    ): CameraSession {
        val cameraProvider = obtainCameraProvider()
        val preview = Preview.Builder().build().apply {
            setSurfaceProvider(previewView.surfaceProvider)
        }

        val analysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build().apply {
                targetRotation = previewView.display?.rotation ?: Surface.ROTATION_0
            }

        val executor = Executors.newSingleThreadExecutor()
        analysis.setAnalyzer(executor, analyzer)

        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(
            lifecycleOwner,
            CameraSelector.DEFAULT_FRONT_CAMERA,
            preview,
            analysis
        )

        return CameraSession(cameraProvider, listOf(preview, analysis), executor)
    }

    private suspend fun obtainCameraProvider(): ProcessCameraProvider {
        return suspendCancellableCoroutine { continuation ->
            val future = ProcessCameraProvider.getInstance(context)
            future.addListener(
                {
                    try {
                        continuation.resume(future.get())
                    } catch (exception: Exception) {
                        continuation.resumeWithException(exception)
                    }
                },
                ContextCompat.getMainExecutor(context)
            )
            continuation.invokeOnCancellation { future.cancel(true) }
        }
    }
}

class CameraSession internal constructor(
    private val cameraProvider: ProcessCameraProvider,
    private val useCases: List<UseCase>,
    private val executor: ExecutorService
) {
    fun close() {
        cameraProvider.unbind(*useCases.toTypedArray())
        executor.shutdown()
    }
}
