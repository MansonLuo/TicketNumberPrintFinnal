package com.example.ticketnumberprintfinnal.utils

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