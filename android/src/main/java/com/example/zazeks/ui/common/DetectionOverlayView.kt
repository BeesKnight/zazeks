package com.example.zazeks.ui.common

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.example.zazeks.R
import com.example.zazeks.ui.offline.BoundingBoxUiModel
import java.util.Locale
import kotlin.math.max

/**
 * Lightweight overlay that renders bounding boxes produced by the gesture detector.
 */
class DetectionOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val strokeWidth = resources.displayMetrics.density * 3f
    private val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = this@DetectionOverlayView.strokeWidth
        color = ContextCompat.getColor(context, R.color.color_state_success)
    }
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#B3000000")
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = resources.displayMetrics.scaledDensity * 16f
    }

    private var detection: BoundingBoxUiModel? = null

    fun setDetection(box: BoundingBoxUiModel?) {
        detection = box?.clamp()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val detection = detection ?: return
        if (!detection.isValid()) return

        val left = detection.left * width
        val top = detection.top * height
        val right = detection.right * width
        val bottom = detection.bottom * height
        if (right <= left || bottom <= top) {
            return
        }

        val rect = RectF(left, top, right, bottom)
        canvas.drawRect(rect, boxPaint)

        val labelText = buildLabel(detection)
        if (labelText.isNotBlank()) {
            val textWidth = textPaint.measureText(labelText)
            val textHeight = textPaint.fontMetrics.let { it.bottom - it.top }
            val padding = resources.displayMetrics.density * 6f
            val backgroundRect = RectF(
                rect.left,
                max(0f, rect.top - textHeight - padding * 2),
                rect.left + textWidth + padding * 2,
                rect.top
            )
            canvas.drawRoundRect(backgroundRect, padding, padding, backgroundPaint)
            canvas.drawText(
                labelText,
                backgroundRect.left + padding,
                backgroundRect.bottom - padding - textPaint.fontMetrics.bottom,
                textPaint
            )
        }
    }

    private fun buildLabel(box: BoundingBoxUiModel): String {
        val builder = StringBuilder()
        val label = box.label
        if (!label.isNullOrBlank()) {
            builder.append(label.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() })
        }
        val confidence = box.confidence
        if (confidence != null && confidence > 0.0) {
            if (builder.isNotEmpty()) {
                builder.append(' ')
            }
            builder.append(String.format(Locale.getDefault(), "%.0f%%", confidence * 100))
        }
        return builder.toString()
    }
}
