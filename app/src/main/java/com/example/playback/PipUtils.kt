package com.example.playback

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.Build
import android.util.Rational

object PipUtils {
    private const val MIN_ASPECT_RATIO = 0.43f
    private const val MAX_ASPECT_RATIO = 2.38f

    fun isPipSupported(context: Context): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
    }

    /**
     * Android crashes enterPictureInPictureMode / setPictureInPictureParams if aspect ratio
     * falls outside [1:2.39 .. 2.39:1]. Clamp video dimensions safely.
     */
    fun safeAspectRatio(width: Int, height: Int): Rational {
        if (width <= 0 || height <= 0) return Rational(16, 9)
        val raw = width.toFloat() / height.toFloat()
        val clamped = raw.coerceIn(MIN_ASPECT_RATIO, MAX_ASPECT_RATIO)
        val scaledWidth = (clamped * 1000f).toInt().coerceAtLeast(430)
        return Rational(scaledWidth, 1000)
    }

    tailrec fun Context.findActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}
