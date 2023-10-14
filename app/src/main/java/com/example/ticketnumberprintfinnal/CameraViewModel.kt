package com.example.ticketnumberprintfinnal

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.ticketnumberprintfinnal.extentions.selectNumberText
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import java.io.IOException

class CameraViewModel: ViewModel() {
    val recognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
    val recognizedNumber = mutableStateOf<String?>(null)


    suspend fun recognizeTicketNumber(context: Context, uri: Uri) {
        lateinit var image: InputImage

        try {
            image = InputImage.fromFilePath(context, uri)
        } catch (e: IOException) {
            e.printStackTrace()
        }

        val result = recognizer.process(image)
            .addOnSuccessListener { visionText ->
                recognizedNumber.value = visionText.text
            }
            .addOnFailureListener {  e ->
                e.printStackTrace()
            }
    }
}