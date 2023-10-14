package com.example.ticketnumberprintfinnal.extentions

import android.content.Context
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

suspend fun Context.getCameraProvider(): ProcessCameraProvider = suspendCoroutine {  continuation ->
    ProcessCameraProvider.getInstance(this).also {  cameraProvider ->
        cameraProvider.addListener({
            continuation.resume(cameraProvider.get())
        }, ContextCompat.getMainExecutor(this))
    }
}

fun Context.getImageOutputRootDirectory(): String {
    var rootImageDirectory = ""

    try {
         rootImageDirectory = this.getExternalFilesDir("images")?.absolutePath!!
    } catch (e: Exception) {
        e.printStackTrace()
    }

    return rootImageDirectory
}