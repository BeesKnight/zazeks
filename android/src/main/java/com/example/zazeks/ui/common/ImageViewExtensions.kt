package com.example.zazeks.ui.common

import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.util.Base64
import android.widget.ImageView

fun ImageView.loadBase64Image(
    base64: String?,
    placeholder: Drawable? = null,
) {
    if (base64.isNullOrBlank()) {
        setImageDrawable(placeholder)
        return
    }
    val parts = base64.split(",", limit = 2)
    val encoded = if (parts.size == 2) parts[1] else base64
    val bytes = try {
        Base64.decode(encoded, Base64.DEFAULT)
    } catch (error: IllegalArgumentException) {
        setImageDrawable(placeholder)
        return
    }
    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    if (bitmap != null) {
        setImageBitmap(bitmap)
    } else {
        setImageDrawable(placeholder)
    }
}
