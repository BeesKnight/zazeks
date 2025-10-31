package com.example.zazeks.infra.camera

import android.graphics.Bitmap
import android.graphics.Bitmap.Config
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import androidx.camera.core.ImageProxy
import com.example.zazeks.infra.ml.GameFrame
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import javax.inject.Inject

/**
 * Converts [ImageProxy] frames produced by CameraX into the [GameFrame] DTO that is
 * expected by the recognition bridge.
 */
class ImageProxyToGameFrameConverter @Inject constructor() {

    fun convert(image: ImageProxy): GameFrame? {
        return when (image.format) {
            ImageFormat.YUV_420_888 -> fromYuv420(image)
            ImageFormat.UNKNOWN -> fromRgbaPlane(image)
            else -> fromRgbaPlane(image)
        }
    }

    private fun fromYuv420(image: ImageProxy): GameFrame? {
        val nv21 = image.toNv21() ?: return null
        val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
        val outputStream = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, image.width, image.height), JPEG_QUALITY, outputStream)
        return GameFrame(outputStream.toByteArray(), image.width, image.height, System.currentTimeMillis())
    }

    private fun fromRgbaPlane(image: ImageProxy): GameFrame? {
        val plane = image.planes.firstOrNull() ?: return null
        plane.buffer.rewind()
        val buffer = ByteArray(plane.buffer.remaining())
        plane.buffer.get(buffer)
        val bitmap = Bitmap.createBitmap(image.width, image.height, Config.ARGB_8888)
        bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(buffer))
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream)
        bitmap.recycle()
        return GameFrame(outputStream.toByteArray(), image.width, image.height, System.currentTimeMillis())
    }

    private fun ImageProxy.toNv21(): ByteArray? {
        if (format != ImageFormat.YUV_420_888) return null
        val yPlane = planes[0]
        val uPlane = planes[1]
        val vPlane = planes[2]

        val ySize = yPlane.buffer.remaining()
        val uSize = uPlane.buffer.remaining()
        val vSize = vPlane.buffer.remaining()
        val nv21 = ByteArray(ySize + uSize + vSize)

        yPlane.buffer.get(nv21, 0, ySize)

        val chromaHeight = height / 2
        val chromaWidth = width / 2
        var outputOffset = ySize
        val uBuffer = uPlane.buffer
        val vBuffer = vPlane.buffer
        val uRowStride = uPlane.rowStride
        val vRowStride = vPlane.rowStride
        val uPixelStride = uPlane.pixelStride
        val vPixelStride = vPlane.pixelStride

        val uData = ByteArray(uBuffer.remaining()).also { buffer ->
            uBuffer.get(buffer)
            uBuffer.rewind()
        }
        val vData = ByteArray(vBuffer.remaining()).also { buffer ->
            vBuffer.get(buffer)
            vBuffer.rewind()
        }

        for (row in 0 until chromaHeight) {
            for (col in 0 until chromaWidth) {
                val uIndex = row * uRowStride + col * uPixelStride
                val vIndex = row * vRowStride + col * vPixelStride
                nv21[outputOffset++] = vData.getOrElse(vIndex) { 0 }
                nv21[outputOffset++] = uData.getOrElse(uIndex) { 0 }
            }
        }
        return nv21
    }

    companion object {
        private const val JPEG_QUALITY = 85
    }
}
