package com.example.util

import android.content.Context
import android.content.res.Resources
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import android.view.WindowMetrics

object DeviceDisplayUtils {

    data class DisplaySize(val width: Int, val height: Int)

    fun getRealDisplaySize(context: Context): DisplaySize {
        return try {
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
            if (windowManager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val metrics: WindowMetrics = windowManager.currentWindowMetrics
                val bounds = metrics.bounds
                DisplaySize(bounds.width(), bounds.height())
            } else {
                val dm = context.resources.displayMetrics
                DisplaySize(dm.widthPixels, dm.heightPixels)
            }
        } catch (e: Exception) {
            DisplaySize(1080, 2400)
        }
    }

    /**
     * Maps an (X, Y) coordinate from the AI reference resolution (e.g. 1080x2400)
     * to the actual physical screen resolution of the device where the app is installed.
     */
    fun mapCoordinatesToDevice(
        aiX: Int,
        aiY: Int,
        referenceWidth: Int = 1080,
        referenceHeight: Int = 2400,
        deviceSize: DisplaySize
    ): Pair<Float, Float> {
        val targetX = (aiX.toFloat() / referenceWidth.toFloat()) * deviceSize.width
        val targetY = (aiY.toFloat() / referenceHeight.toFloat()) * deviceSize.height
        return targetX to targetY
    }
}
