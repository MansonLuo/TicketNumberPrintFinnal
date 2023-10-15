package com.example.ticketnumberprintfinnal

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.ticketnumberprintfinnal.api.MbrushRepository
import com.example.ticketnumberprintfinnal.extentions.ContextExts.Companion.deleteAllMbdFile
import com.example.ticketnumberprintfinnal.extentions.ContextExts.Companion.deleteTmpRgbFile
import com.example.ticketnumberprintfinnal.extentions.extractTicketNumber
import com.example.ticketnumberprintfinnal.extentions.saveJpgTo
import com.example.ticketnumberprintfinnal.extentions.transformAndSaveToTmpRgb
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import java.io.File
import java.io.IOException

class CameraViewModel(
    private val mbrushRepository: MbrushRepository
): ViewModel() {
    val recognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
    val recognizedNumber = mutableStateOf<String?>(null)

    val sendResult = mutableStateOf<String?>(null)
    var imageUri = mutableStateOf<Uri?>(null)
    var pos = mutableStateOf(0)
    lateinit var rootImgPath: String
    lateinit var rootMbdPath: String
    lateinit var rootTmpPath: String
    lateinit var tmpRgbFilePath: String

    suspend fun recognizeTicketNumber(
        context: Context,
        uri: Uri,
        onRecognizeNumber: (String) -> Unit
    ){
        lateinit var image: InputImage

        try {
            image = InputImage.fromFilePath(context, uri)
        } catch (e: IOException) {
            e.printStackTrace()
        }

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                // suppose only got one line text
                // otherwise will throw exception
                var extractedNumber = visionText.text

                extractedNumber = extractedNumber.extractTicketNumber()

                recognizedNumber.value = extractedNumber
                sendResult.value = ""
                onRecognizeNumber(extractedNumber)
            }
            .addOnFailureListener {  e ->
                e.printStackTrace()
            }
    }

    suspend fun send() {
        val res = mbrushRepository.upload(
            "$rootMbdPath/${pos.value}.mbd",
            pos.value,
        ).status

        sendResult.value = "发送状态: $res"
        pos.value += 1
    }

    suspend fun removeUpload(context: Context) {
        val res = mbrushRepository.removeUpload().status

        // reset
        context.deleteAllMbdFile(rootMbdPath)
        pos.value = 0
        sendResult.value = "清空状态: $res"
        recognizedNumber.value = ""
    }

    fun transformText(
        text: String,
        context: Context,
    ) {
        text.saveJpgTo(rootImgPath).let { imgFilePath ->
            imgFilePath.transformAndSaveToTmpRgb(context, rootTmpPath)

            (context as MainActivity).generateMBDFile(
                tmpRgbFilePath,
                "$rootMbdPath/${pos.value}.mbd"
            )

            context.deleteTmpRgbFile(tmpRgbFilePath)
            imageUri.value = Uri.fromFile(File(imgFilePath))
        }

    }

    fun resetState() {
        sendResult.value = null
    }

    fun loadRootPath(context: Context) {
        rootImgPath = context.getExternalFilesDir("genedImages")!!.absolutePath
        rootMbdPath = context.getExternalFilesDir("mbds")!!.absolutePath
        rootTmpPath = context.getExternalFilesDir("tmps")!!.absolutePath
        tmpRgbFilePath = "$rootTmpPath/tmp.rgb"
    }
}