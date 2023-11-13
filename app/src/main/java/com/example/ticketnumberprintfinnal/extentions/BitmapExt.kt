package com.example.ticketnumberprintfinnal.extentions

import android.graphics.Bitmap
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

fun Bitmap.saveToFile(file: File) {
    var fos: FileOutputStream? = null

    try {
        fos = FileOutputStream(file)
        this.compress(Bitmap.CompressFormat.JPEG, 100, fos);
        fos.flush();
    } catch (e: IOException) {
        e.printStackTrace();
    } finally {
        try {
            if (fos != null) {
                fos.close();
            }
        } catch (e: IOException) {
            e.printStackTrace();
        }
    }
}