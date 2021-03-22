package com.darwin.viola.age

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log


/**
 * Utility class to manage helper functions
 *
 * @author Darwin Francis
 * @version 1.0
 * @since 11 Jul 2020
 */
internal class Util {
    companion object {
        var debug: Boolean = false
        fun printLog(message: String) {
            if (debug)
                Log.d("Viola-Age", message)
        }


        fun forceEvenBitmapSize(original: Bitmap): Bitmap? {
            var width = original.width
            var height = original.height
            if (width % 2 == 1) {
                width++
            }
            if (height % 2 == 1) {
                height++
            }
            var fixedBitmap = original
            if (width != original.width || height != original.height) {
                fixedBitmap = Bitmap.createScaledBitmap(original, width, height, false)
            }
            return fixedBitmap
        }

        fun forceConfig565(original: Bitmap): Bitmap {
            var convertedBitmap = original
            if (original.config != Bitmap.Config.RGB_565) {
                convertedBitmap =
                    Bitmap.createBitmap(original.width, original.height, Bitmap.Config.RGB_565)
                val canvas = Canvas(convertedBitmap)
                val paint = Paint()
                paint.color = Color.BLACK
                canvas.drawBitmap(original, 0.0f, 0.0f, paint)
            }
            return convertedBitmap
        }
    }
}