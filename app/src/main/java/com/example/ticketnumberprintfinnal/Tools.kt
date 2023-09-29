package com.example.ticketnumberprintfinnal

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.util.DisplayMetrics
import android.util.Log
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException


class Tools {
    companion object {
        fun bitmapClip(mContext: Context, imgPath: String): Bitmap {
            var bitmap = BitmapFactory.decodeFile(imgPath)

            Log.d("Main", "width: ${bitmap.width}---heigh: ${bitmap.height}")

            val matrix = pictureDegree(imgPath)
            val bitmapRatio = bitmap.height * 1.0 / bitmap.width //基本上都是16/9

            val width: Int = getScreenwidth(mContext)
            val height: Int = getScreenHeight(mContext)
            val screenRatio = height * 1.0 / width //屏幕的宽高比

            bitmap = if (bitmapRatio > screenRatio) { //胖的手机
                Log.d("Main", "pang")
                val clipHeight = (bitmap.width * screenRatio).toInt()
                Bitmap.createBitmap(
                    bitmap,
                    0,
                    bitmap.height - clipHeight shr 1,
                    bitmap.width,
                    clipHeight,
                    matrix,
                    true
                )
            } else { //瘦长的手机
                Log.d("Main", "shou")
                //Bitmap.createBitmap(bitmap, 0, bitmap.width / 3, bitmap.width, bitmap.height / 3, matrix, true)
                Bitmap.createBitmap(bitmap, bitmap.width / 3, 0, bitmap.height, bitmap.height ,matrix, true)
            }
            return bitmap
        }

        private fun pictureDegree(imgPath: String): Matrix {
            val matrix = Matrix()
            var exif: ExifInterface? = null

            try {
                exif = ExifInterface(imgPath)
            } catch (e: IOException) {
                e.printStackTrace()
            }

            if (exif == null) return matrix
            var degree = 0
            val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1)

            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> degree = 90
                ExifInterface.ORIENTATION_ROTATE_180 -> degree = 180
                ExifInterface.ORIENTATION_ROTATE_270 -> degree = 270
                else -> {}
            }

            matrix.postRotate(degree.toFloat())

            return matrix
        }

        fun getScreenwidth(mContext: Context): Int {
            return getDisplayMetrics(mContext)!!.widthPixels
        }

        fun getScreenHeight(mContext: Context): Int {
            return getDisplayMetrics(mContext)!!.heightPixels
        }
        private fun getDisplayMetrics(mContext: Context): DisplayMetrics? {
            return mContext.resources.displayMetrics
        }

        fun saveBitmap(bitmap: Bitmap, savePath: String): Boolean {
            try {
                val file = File(savePath)
                val parent = file.parentFile
                if (!parent.exists()) {
                    parent.mkdirs()
                }
                val fos = FileOutputStream(file)
                val b = bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
                fos.flush()
                fos.close()
                return b
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return false
        }
    }
}