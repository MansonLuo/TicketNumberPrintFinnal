package com.example.ticketnumberprintfinnal

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import com.example.ticketnumberprintfinnal.api.MbrushRepository
import com.example.ticketnumberprintfinnal.extentions.ContextExts.Companion.deleteAllMbdFile
import com.example.ticketnumberprintfinnal.extentions.ContextExts.Companion.deleteTmpRgbFile
import com.example.ticketnumberprintfinnal.extentions.extractTicketNumber
import com.example.ticketnumberprintfinnal.extentions.saveJpgTo
import com.example.ticketnumberprintfinnal.extentions.takePicture
import com.example.ticketnumberprintfinnal.extentions.transformAndSaveToTmpRgb
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import java.io.File
import java.io.IOException

class CameraViewModel(
    private val mbrushRepository: MbrushRepository
): ViewModel() {
    val recognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
    val recognizedNumber = mutableStateOf<String?>(null)
    val recognizedNumberList = mutableStateListOf<String>()
    var currentNumberOfTickets = mutableStateOf<Int>(1)

    val sendResult = mutableStateOf<String?>(null)
    var imageUri = mutableStateOf<Uri?>(null)
    var pos = mutableStateOf(0)
    lateinit var rootImgPath: String
    lateinit var rootMbdPath: String
    lateinit var rootTmpPath: String
    lateinit var tmpRgbFilePath: String

    var expanded by mutableStateOf<Boolean>(false)
        private set
    var cropBoxHeight by mutableStateOf<Dp>(60.dp)
    fun recognizeTicketNumber(
        context: Context,
        uri: Uri,
        onSuccess: () -> Unit,
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
                val allLines = mutableListOf<String>()
                val wantedLines = mutableListOf<String>()

                for (block in visionText.textBlocks) {
                    for (line in block.lines) {
                        //allLines.add(line.text)
                        val res = line.text.extractTicketNumber()

                        recognizedNumberList.add(res)
                    }
                }
                recognizedNumberList.add(visionText.text)

                /*
                recognizedNumberList.clear()

                if (allLines.isEmpty()) {
                    recognizedNumberList.add("无法识别")
                } else {
                    if (allLines.size < currentNumberOfTickets.value) {
                        wantedLines.apply {
                            addAll(allLines)
                        }
                    } else {
                        wantedLines.apply {
                            addAll(
                                allLines.subList(0, currentNumberOfTickets.value)
                            )
                        }
                    }
                    wantedLines.map {  recognizedTextLine ->
                        recognizedNumberList.add(
                            recognizedTextLine.extractTicketNumber()
                        )
                    }
                }
                 */
                onSuccess()
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

    fun expandDropMenu() {
        expanded = true
    }

    fun dismissDropMenu() {
        expanded = false
    }

    fun changeCropBoxHeight(number: Int) {
        currentNumberOfTickets.value = number
        cropBoxHeight = (number * 60).dp
    }

    fun takePictureAndSendMbdFiles(
        context: Context,
        imageCapture: ImageCapture
    ) {
        // take picture
        imageCapture.takePicture(
            context,
            onError = { e -> e.printStackTrace()},
        ) { imageFieUri ->
            // recognize ticket numbers
            recognizeTicketNumber(
                context,
                imageFieUri
            ) {
            }

            // transform numbers to mbd files

            // send mbd files
        }
    }
}