package com.example.ticketnumberprintfinnal.extentions

import android.content.Context
import androidx.compose.ui.unit.Dp

fun Dp.toPx(context: Context): Int {
    val density = context.resources.displayMetrics.density

    return Math.round(this.value * density)
}