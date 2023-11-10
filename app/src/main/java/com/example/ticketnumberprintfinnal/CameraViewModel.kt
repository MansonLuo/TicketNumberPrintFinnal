package com.example.ticketnumberprintfinnal

import android.content.Context
import android.net.Uri
import androidx.camera.core.ImageCapture
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ticketnumberprintfinnal.api.MbrushRepository
import com.example.ticketnumberprintfinnal.extentions.ContextExts.Companion.deleteAllMbdFile
import com.example.ticketnumberprintfinnal.extentions.ContextExts.Companion.deleteTmpRgbFile
import com.example.ticketnumberprintfinnal.extentions.extractTicketNumber
import com.example.ticketnumberprintfinnal.extentions.saveGeneratedWhiteJpgTo
import com.example.ticketnumberprintfinnal.extentions.takePicture
import com.example.ticketnumberprintfinnal.extentions.transformAndSaveToTmpRgb
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

class CameraViewModel(
    private val mbrushRepository: MbrushRepository
) : ViewModel() {
    val recognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
    val recognizedNumberList = mutableStateListOf<String>()

    val sendResultList = mutableStateListOf<String>()

    var imageUri = mutableStateOf<Uri?>(null)
    var pos = mutableStateOf(0)

    lateinit var rootImgPath: String
    lateinit var rootMbdPath: String
    lateinit var rootTmpPath: String
    lateinit var tmpRgbFilePath: String

    fun recognizeTicketNumber(
        context: Context,
        uri: Uri,
        onSuccess: () -> Unit,
    ) {
        lateinit var image: InputImage

        try {
            image = InputImage.fromFilePath(context, uri)
        } catch (e: IOException) {
            e.printStackTrace()
        }

        recognizer.process(image)
            .addOnSuccessListener { visionText ->

                for (block in visionText.textBlocks) {
                    for (line in block.lines) {
                        val res = line.text.extractTicketNumber()

                        if (res.isNotEmpty()) {
                            recognizedNumberList.add(res)
                        }
                    }
                }

                onSuccess()
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
            }
    }

    suspend fun send() {
        /*
        val res = mbrushRepository.upload(
            "$rootMbdPath/${pos.value}.mbd",
            pos.value,
        ).status

         */
        delay(500)

        sendResultList.apply {
            clear()
            add("发送状态: ok")
        }
        pos.value += 1
    }

    suspend fun removeUpload(context: Context) {
        val res = "ok"//mbrushRepository.removeUpload().status

        // reset
        context.deleteAllMbdFile(rootMbdPath)
        pos.value = 0
        sendResultList.apply {
            clear()
            add("清空状态: $res")
        }

        recognizedNumberList.clear()
    }

    fun transformTicketNumberStrToMbdFile(
        ticketNumber: String,
        context: Context,
        name: String
    ) {
        ticketNumber.saveGeneratedWhiteJpgTo(rootImgPath, name).let { generatedImageFilePath ->
            generatedImageFilePath.transformAndSaveToTmpRgb(context, rootTmpPath, name)

            (context as MainActivity).generateMBDFile(
                tmpRgbFilePath.replace("#", name),
                "$rootMbdPath/${name}.mbd"
            )

            imageUri.value = Uri.fromFile(File(generatedImageFilePath))
        }

    }

    fun loadRootPath(context: Context) {
        rootImgPath = context.getExternalFilesDir("genedImages")!!.absolutePath
        rootMbdPath = context.getExternalFilesDir("mbds")!!.absolutePath
        rootTmpPath = context.getExternalFilesDir("tmps")!!.absolutePath
        tmpRgbFilePath = "$rootTmpPath/tmp#.rgb"
    }

    fun takePictureAndSendMbdFiles(
        context: Context,
        imageCapture: ImageCapture
    ) {
        // take picture
        imageCapture.takePicture(
            context,
            onError = { e -> e.printStackTrace() },
        ) { imageFieUri ->
            // recognize ticket numbers
            recognizeTicketNumber(
                context,
                imageFieUri
            ) {
                runBlocking {
                    recognizedNumberList.forEachIndexed { index, recognizedNumberStr ->
                        // transform numbers to mbd files
                        viewModelScope.launch {
                            transformTicketNumberStrToMbdFile(
                                recognizedNumberStr,
                                context,
                                index.toString()
                            )
                        }

                    }
                }




                // send mbd files
                //runBlocking { send() }
            }
        }
    }
}