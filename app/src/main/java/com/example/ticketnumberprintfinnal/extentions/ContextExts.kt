package com.example.ticketnumberprintfinnal.extentions

import android.content.Context
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.cherryleafroad.kmagick.Magick
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import java.io.File

class ContextExts {
    companion object {

        var magick: Magick? = null

        fun Context.getMagick(): Magick {
            if (magick == null) {
                magick = Magick.initialize()
            }

            return magick!!
        }

        fun Context.deleteTmpRgbFile(path: String) {
            val rootPath = File(path)

            try {
                rootPath.listFiles()?.forEach {
                    it.delete()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun Context.deleteAllMbdFile(rootPath: String) {
            val rootDir = File(rootPath)

            try {
                rootDir.listFiles()?.forEach {
                    it.delete()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        suspend fun Context.getCameraProvider(): ProcessCameraProvider =
            suspendCoroutine { continuation ->
                ProcessCameraProvider.getInstance(this).also { cameraProvider ->
                    cameraProvider.addListener({
                        continuation.resume(cameraProvider.get())
                    }, ContextCompat.getMainExecutor(this))
                }
            }

        fun Context.getImageOutputRootDirectory(): String {
            var rootImageDirectory = ""

            try {
                rootImageDirectory = this.getExternalFilesDir("croppedImages")?.absolutePath!!
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return rootImageDirectory
        }
    }
}