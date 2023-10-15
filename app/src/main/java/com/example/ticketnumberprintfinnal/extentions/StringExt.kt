package com.example.ticketnumberprintfinnal.extentions

import androidx.core.text.isDigitsOnly
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.cherryleafroad.kmagick.FilterType
import com.cherryleafroad.kmagick.MagickWand
import com.cherryleafroad.kmagick.PixelWand
import com.example.ticketnumberprintfinnal.extentions.ContextExts.Companion.getMagick
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date

fun String.selectNumberText(): String {
    return this.split(" ").map {
        it.trim()
    }.filter {
        it.isDigitsOnly()
    }.first()
}

fun String.saveJpgTo(rootPath: String): String {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.textSize = 80f
    paint.color = Color.BLACK
    paint.textAlign = Paint.Align.LEFT
    val basaLine = -paint.ascent()
    val width = (paint.measureText(this) + 0.5f).toInt()
    val height = (basaLine + paint.descent() + 0.5f).toInt()
    val image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(image)
    canvas.drawColor(Color.WHITE)
    canvas.drawText(this, 0f, basaLine, paint)

    val filePath = rootPath + File.separator + "${getRandomFileName()}.jpg"

    try {
        val quality = 100
        val fos = FileOutputStream(File(filePath))
        image.compress(Bitmap.CompressFormat.JPEG, quality, fos)
        fos.close()
    } catch (e: Exception) {
        e.printStackTrace()
    }

    return filePath
}

fun String.transformAndSaveToTmpRgb(context: Context, rootPath: String) {
    // generate tmp.rgb file
    context.getMagick().use {
        val wand = MagickWand()
        wand.readImage(this)

        var width = wand.getImageWidth()
        val height = wand.getImageHeight()

        val scaleHeight = 684f / height
        width = ((width / 5f) * scaleHeight).toLong()
        wand.resizeImage(width, 684, FilterType.BartlettFilter)

        val pixel = PixelWand()
        pixel.color = "transparent"
        wand.rotateImage(pixel, 90.00)

        wand.writeImage(rootPath + File.separator + "tmp.rgb")
    }

}

fun String.getRandomFileName(): String {
    val currentTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

    return currentTime.format(Date()).toString().replace(":", "")
}

fun String.extractTicketNumber(): String {
    val res = this.replace("[^0-9]".toRegex(), "")

    return if (res.isBlank()) "无法识别" else res
}