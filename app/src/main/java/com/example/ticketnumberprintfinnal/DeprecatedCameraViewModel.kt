package com.example.ticketnumberprintfinnal

import android.content.Context
import android.net.Uri
import androidx.camera.core.ImageCapture
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
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
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

class DeprecatedCameraViewModel(
    private val mbrushRepository: MbrushRepository
) : ViewModel() {
    val recognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
    val recognizedNumberList = mutableStateListOf<String>()

    val sendResultList = mutableStateListOf<String>()

    var imageUri = mutableStateOf<Uri?>(null)

    lateinit var rootImgPath: String
    lateinit var rootMbdPath: String
    lateinit var rootTmpPath: String
    lateinit var tmpRgbFilePath: String

    fun deprecatedRecognizeTicketNumber(
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

    suspend fun deprecatedSend() {
        sendResultList.clear()

        withContext(Dispatchers.IO) {
            (0 until recognizedNumberList.size).forEach { index ->
                async {
                    val res = mbrushRepository.upload(
                        "$rootMbdPath/${index}.mbd",
                        index,
                    ).status

                    sendResultList.add("发送状态: $res")
                }.await()
            }
        }

    }

    suspend fun deprecatedRemoveUpload(context: Context) {
        viewModelScope.launch {
            sendResultList.clear()

            val res = withContext(Dispatchers.IO) {
                async {
                    mbrushRepository.removeUpload().status
                }.await()
            }

            sendResultList.add("清空状态: $res")
        }

        // reset
        context.deleteAllMbdFile(rootMbdPath)
        context.deleteTmpRgbFile(rootTmpPath)
        recognizedNumberList.clear()
    }

    fun deprecatedTransformTicketNumberStrToMbdFile(
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

    fun deprecatedLoadRootPath(context: Context) {
        rootImgPath = context.getExternalFilesDir("genedImages")!!.absolutePath
        rootMbdPath = context.getExternalFilesDir("mbds")!!.absolutePath
        rootTmpPath = context.getExternalFilesDir("tmps")!!.absolutePath
        tmpRgbFilePath = "$rootTmpPath/tmp#.rgb"
    }

    fun deprectaedTakePictureAndSendMbdFiles(
        context: Context,
        imageCapture: ImageCapture
    ) {
        // take picture
        imageCapture.takePicture(
            context,
            onError = { e -> e.printStackTrace() },
        ) { imageFieUri ->
            // recognize ticket numbers
            deprecatedRecognizeTicketNumber(
                context,
                imageFieUri
            ) {

                viewModelScope.launch {
                    recognizedNumberList.forEachIndexed { index, recognizedNumberStr ->
                        async {
                            deprecatedTransformTicketNumberStrToMbdFile(
                                recognizedNumberStr,
                                context,
                                index.toString()
                            )
                        }.await()
                    }

                    deprecatedSend()
                }
            }
        }
    }
}