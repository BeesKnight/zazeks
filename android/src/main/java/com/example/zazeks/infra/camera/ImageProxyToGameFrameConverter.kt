package com.example.zazeks.infra.camera

import android.graphics.*
import android.graphics.ImageFormat
import androidx.camera.core.ImageProxy
import com.example.zazeks.infra.ml.GameFrame
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.min

/**
 * Конвертирует ImageProxy -> JPEG (ByteArray) и УЧИТЫВАЕТ rotationDegrees
 * Возвращает корректные width/height уже после поворота.
 */
class ImageProxyToGameFrameConverter @Inject constructor() {

    fun convert(image: ImageProxy): GameFrame? {
        // На большинстве устройств формат YUV_420_888
        return if (image.format == ImageFormat.YUV_420_888) {
            fromYuv420Rotated(image)
        } else {
            fromRgbaPlaneRotated(image)
        }
    }

    // ---------- YUV путь ----------
    private fun fromYuv420Rotated(image: ImageProxy): GameFrame? {
        val nv21 = image.toNv21() ?: return null

        // 1) NV21 -> JPEG (в «сырой» ориентации)
        val yuv = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
        val tmp = ByteArrayOutputStream()
        yuv.compressToJpeg(Rect(0, 0, image.width, image.height), JPEG_QUALITY, tmp)
        val jpegBytes = tmp.toByteArray()

        // 2) JPEG -> Bitmap -> rotate(rotationDegrees)
        val bmp = BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size) ?: return null
        val deg = image.imageInfo.rotationDegrees
        val rotated = if (deg != 0) rotateBitmap(bmp, deg) else bmp

        // 3) назад в JPEG
        val out = ByteArrayOutputStream()
        rotated.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
        val finalJpeg = out.toByteArray()

        return GameFrame(
            finalJpeg,
            rotated.width,
            rotated.height,
            System.currentTimeMillis()
        )
    }

    // ---------- RGBA (fallback) ----------
    private fun fromRgbaPlaneRotated(image: ImageProxy): GameFrame? {
        val plane = image.planes.firstOrNull() ?: return null
        val buf = plane.buffer
        val rgba = ByteArray(buf.remaining()).also { buf.get(it) }

        val bmp = Bitmap.createBitmap(image.width, image.height, Bitmap.Config.ARGB_8888)
        bmp.copyPixelsFromBuffer(ByteBuffer.wrap(rgba))

        val deg = image.imageInfo.rotationDegrees
        val rotated = if (deg != 0) rotateBitmap(bmp, deg) else bmp

        val out = ByteArrayOutputStream()
        rotated.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
        return GameFrame(
            out.toByteArray(),
            rotated.width,
            rotated.height,
            System.currentTimeMillis()
        )
    }

    private fun rotateBitmap(src: Bitmap, degrees: Int): Bitmap {
        // Если нужна «зеркалка» для фронталки, раскомментируйте строку scaleX = -1f
        val m = Matrix().apply {
            // postScale(-1f, 1f, src.width / 2f, src.height / 2f)  // зеркалка по горизонтали (опционально)
            postRotate(degrees.toFloat())
        }
        return Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true)
    }

    companion object {
        private const val JPEG_QUALITY = 85
    }
}

/**
 * Надёжная конвертация YUV_420_888 -> NV21, с учётом rowStride/pixelStride
 */
private fun ImageProxy.toNv21(): ByteArray? {
    val yPlane = planes.getOrNull(0) ?: return null
    val uPlane = planes.getOrNull(1) ?: return null
    val vPlane = planes.getOrNull(2) ?: return null

    val ySize = yPlane.buffer.remaining()
    val uSize = uPlane.buffer.remaining()
    val vSize = vPlane.buffer.remaining()

    // Буфер NV21: Y + VU (interleaved)
    val out = ByteArray(width * height + 2 * ((width + 1) / 2) * ((height + 1) / 2))
    var outPos = 0

    // --- Копируем Y построчно (из-за rowStride) ---
    val yRowStride = yPlane.rowStride
    val yPixelStride = yPlane.pixelStride
    val yBuffer = yPlane.buffer
    yBuffer.rewind()

    if (yPixelStride == 1 && yRowStride == width) {
        yBuffer.get(out, 0, width * height)
        outPos += width * height
    } else {
        val y = ByteArray(ySize)
        yBuffer.get(y)
        var offset = 0
        for (row in 0 until height) {
            val length = min(width, y.size - offset)
            var col = 0
            var src = offset
            while (col < width && src < offset + length) {
                out[outPos++] = y[src]
                src += yPixelStride
                col++
            }
            offset += yRowStride
        }
    }

    // --- Копируем UV, формируя VU под NV21 ---
    val vRowStride = vPlane.rowStride
    val vPixelStride = vPlane.pixelStride
    val uRowStride = uPlane.rowStride
    val uPixelStride = uPlane.pixelStride

    val vBuf = ByteArray(vSize).also { vPlane.buffer.rewind(); vPlane.buffer.get(it) }
    val uBuf = ByteArray(uSize).also { uPlane.buffer.rewind(); uPlane.buffer.get(it) }

    val chromaHeight = height / 2
    val chromaWidth = width / 2

    var vOffset = 0
    var uOffset = 0
    for (row in 0 until chromaHeight) {
        var vSrc = vOffset
        var uSrc = uOffset
        for (col in 0 until chromaWidth) {
            // NV21: V затем U
            out[outPos++] = vBuf.getOrElse(vSrc) { 0 }
            out[outPos++] = uBuf.getOrElse(uSrc) { 0 }
            vSrc += vPixelStride
            uSrc += uPixelStride
        }
        vOffset += vRowStride
        uOffset += uRowStride
    }
    return out
}
