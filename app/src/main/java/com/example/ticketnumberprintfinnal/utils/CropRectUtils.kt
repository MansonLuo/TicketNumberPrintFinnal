package com.example.ticketnumberprintfinnal.utils

import android.util.Log
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import kotlin.math.roundToInt

object CropRectUtils {
    fun getCropRect(
        surfaceHeight: Float,
        surfaceWidth: Float,
        topLeftScale: Offset,
        sizeScale: Size
    ): Rect {

        Log.e("Main", "in crop")
        val height = surfaceHeight * sizeScale.height
        val with = surfaceWidth * sizeScale.width
        val topLeft = Offset(x = surfaceWidth * topLeftScale.x, y = surfaceHeight * topLeftScale.y)

        return Rect(offset = topLeft, size = Size(with, height))

    }

    fun getCropRect90(
        surfaceHeight: Float,
        surfaceWidth: Float,
        topLeftScale: Offset,
        sizeScale: Size
    ): Rect {

        Log.e("Main", "in Crop 90")
        val height = surfaceHeight * sizeScale.height
        val with = surfaceWidth * sizeScale.width
        val topLeft = Offset(x = surfaceWidth * topLeftScale.x, y = surfaceHeight * topLeftScale.y)

        return Rect(offset = topLeft, size = Size(with, height))

    }

    fun Rect.roundToAndroidRect(): android.graphics.Rect {
        return android.graphics.Rect(
            left.roundToInt(),
            top.roundToInt(),
            right.roundToInt(),
            bottom.roundToInt()
        )
    }
}