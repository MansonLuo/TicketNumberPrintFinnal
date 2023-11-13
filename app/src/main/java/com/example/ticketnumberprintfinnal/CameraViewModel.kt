package com.example.ticketnumberprintfinnal

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ticketnumberprintfinnal.api.MbrushRepository
import com.example.ticketnumberprintfinnal.extentions.ContextExts.Companion.deleteAllMbdFile
import com.example.ticketnumberprintfinnal.extentions.ContextExts.Companion.deleteTmpRgbFile
import com.example.ticketnumberprintfinnal.extentions.extractTicketNumber
import com.example.ticketnumberprintfinnal.extentions.saveGeneratedWhiteJpgTo
import com.example.ticketnumberprintfinnal.extentions.saveToFile
import com.example.ticketnumberprintfinnal.extentions.takePhotoAsync
import com.example.ticketnumberprintfinnal.extentions.transformAndSaveToTmpRgb
import com.example.ticketnumberprintfinnal.utils.CameraConfig
import com.example.ticketnumberprintfinnal.utils.CropRectUtils.getCropRect
import com.example.ticketnumberprintfinnal.utils.CropRectUtils.getCropRect90
import com.example.ticketnumberprintfinnal.utils.CropRectUtils.roundToAndroidRect
import com.example.ticketnumberprintfinnal.utils.ImageUtils
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


/*
 *
 *
 * @author: zhhli
 * @date: 22/7/29
 */
class CameraViewModel(
    config: CameraConfig,
    private val mbrushRepository: MbrushRepository
) : ViewModel() {

    val preview = config.options(Preview.Builder())
    val imageCapture: ImageCapture = config.options(ImageCapture.Builder())


    // We only need to analyze the part of the image that has text, so we set crop percentages
    // to avoid analyze the entire image from the live camera feed.
    // 裁剪区域 比例
    val cropTopLeftScale = mutableStateOf(Offset(x = 0.025f, y = 0.3f))
    val cropSizeScale = mutableStateOf(Size(width = 0.95f, height = 0.1f))
    fun expandCropBox() {
        cropSizeScale.value = cropSizeScale.value.copy(height = 0.3f)
    }
    fun shrinkCropBox() {
        cropSizeScale.value = cropSizeScale.value.copy(height = 0.1f)
    }


    private val textRecognizer: TextRecognizer = TextRecognition.getClient(
        ChineseTextRecognizerOptions.Builder().build()
    )

    var scanText = mutableStateOf("")

    //var bitmapR = mutableStateOf(Bitmap.createBitmap(10, 10, Bitmap.Config.RGB_565))
    var _bitmapR = mutableStateOf<Bitmap?>(null)
    val bitmapR
        get() = _bitmapR.value
    fun updateBitmapR(bitmap: Bitmap) {
        _bitmapR.value = bitmap
    }

    var _bitmapREnabled = mutableStateOf(false)
    val bitmapREnabled
        get() = _bitmapREnabled.value

    var enableTorch: MutableState<Boolean> = mutableStateOf(false)

    fun toggleTorch() {
        enableTorch.value = !enableTorch.value
    }


    // storage
    lateinit var rootImgPath: String
    lateinit var rootMbdPath: String
    lateinit var rootTmpPath: String
    lateinit var tmpRgbFilePath: String

    val _recognizedTicketNumbers = mutableStateListOf<String>()
    val recognizedTicketNumbers: String
        get() {
            return _recognizedTicketNumbers.joinToString(
                separator = ""
            ) {
                "$it\n"
            }
        }
    val _sendResultList = mutableStateListOf<String>()
    val sendResultList: String
        get() {
            return _sendResultList.joinToString(
                separator = ""
            ) {
                "$it\n"
            }
        }

    // Refactory Start
    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    suspend fun takePictureAsync(
        filenameFormat: String = "yyyy-MM-dd-HH-mm-ss-SSS",
        outputDirectory: File,
    ): Uri? {
        val imageProxy = imageCapture.takePhotoAsync()
        val photoFile = File(
            outputDirectory,
            SimpleDateFormat(filenameFormat, Locale.US).format(System.currentTimeMillis()) + ".jpg"
        )

        if (imageProxy.image == null) {
            imageProxy.close()

            return null
        }

        val bitmap = cropTextImage(imageProxy) ?: return null

        withContext(Dispatchers.IO) {
            async {
                bitmap.saveToFile(photoFile)
            }
        }

        imageProxy.close()

        return Uri.fromFile(photoFile)
    }

    suspend fun recognizeTicketNumberAsync(
        context: Context,
        imageUri: Uri
    ): List<String> = suspendCoroutine { continuation ->


        val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, imageUri)
        Log.e("Main", (bitmap == null).toString())
        val inputImageCrop = InputImage.fromBitmap(bitmap, 0)

        textRecognizer.process(inputImageCrop)
            .addOnSuccessListener { visionText ->
                val text = visionText.text

                for (block in visionText.textBlocks) {
                    for (line in block.lines) {
                        val res = line.text.extractTicketNumber()

                        if (res.isNotEmpty()) {
                            _recognizedTicketNumbers.add(res)
                        }
                    }
                }

                Log.d("zzz", "textRecognizer onSuccess")
                Log.d("zzzzzz OCR result", "ocr result: $text")
                _bitmapR.value = bitmap

                continuation.resume(_recognizedTicketNumbers.toList())
            }.addOnFailureListener { exception ->
                Log.d("zzz", "onFailure")
                _bitmapR.value = bitmap
                scanText.value = "onFailure"

                continuation.resumeWithException(exception)
            }
    }

    suspend fun uploadNumbers(
        context: Context,
        numbers: List<String>
    ) {
        coroutineScope {
            numbers.forEachIndexed { index, numberStr ->
                async {
                    transformTicketNumberStrToMbdFile(
                        numberStr,
                        context,
                        index.toString()
                    )
                }.await()
            }

            sendAllMbdFiles()
        }
    }

    private fun transformTicketNumberStrToMbdFile(
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
        }
    }

    private suspend fun sendAllMbdFiles() {
        _sendResultList.clear()

        withContext(Dispatchers.IO) {
            (0 until _recognizedTicketNumbers.size).forEach { index ->
                async {
                    val res = mbrushRepository.upload(
                        "$rootMbdPath/${index}.mbd",
                        index,
                    ).status

                    _sendResultList.add("发送状态: $res")
                }.await()
            }
        }

    }

    suspend fun removeUpload(context: Context) {
        viewModelScope.launch {
            _sendResultList.clear()
            _recognizedTicketNumbers.clear()

            val res = withContext(Dispatchers.IO) {
                async {
                    mbrushRepository.removeUpload().status
                }.await()
            }

            _sendResultList.add("清空状态: $res")
        }

        // reset
        context.deleteAllMbdFile(rootMbdPath)
        context.deleteTmpRgbFile(rootTmpPath)

        _recognizedTicketNumbers.clear()
    }


    fun updateRecognizedText(newText: String) {
        _recognizedTicketNumbers.clear()
        _recognizedTicketNumbers.add(newText)
    }

    fun updateSendResult(newText: String) {
        _sendResultList.clear()
        _sendResultList.add(newText)
    }

    // Refactory End

    fun getOutputDirectory(context: Context): File {


        val mediaDir = File(context.getExternalFilesDir(null), "image").apply {
            mkdir()
        }

        return mediaDir
    }


    @SuppressLint("UnsafeOptInUsageError")
    fun cropTextImage(imageProxy: ImageProxy): Bitmap? {
        val mediaImage = imageProxy.image ?: return null

        val rotationDegrees = imageProxy.imageInfo.rotationDegrees


        val imageHeight = mediaImage.height
        val imageWidth = mediaImage.width

        val cropRect = when (rotationDegrees) {
            // for ImageAnalyze, pass heigh, width
            //90, 270 -> getCropRect90(imageHeight.toFloat(), imageWidth.toFloat()).toAndroidRect()
            //else -> getCropRect(imageHeight.toFloat(), imageWidth.toFloat()).toAndroidRect()

            // for imageCapture.onImageCapture, pass width, height
            90, 270 -> getCropRect90(
                imageWidth.toFloat(),
                imageHeight.toFloat(),
                topLeftScale = Offset(
                    x = cropTopLeftScale.value.y, y = cropTopLeftScale.value.x
                ),
                sizeScale = Size(
                    width = cropSizeScale.value.height,
                    height = cropSizeScale.value.width
                ),
            ).roundToAndroidRect()

            else -> getCropRect(
                imageWidth.toFloat(),
                imageHeight.toFloat(),

                topLeftScale = cropTopLeftScale.value,
                sizeScale = cropSizeScale.value
            ).roundToAndroidRect()
        }


        //val convertImageToBitmap = ImageUtils.convertYuv420888ImageToBitmap(mediaImage)
        val convertImageToBitmap = ImageUtils.convertJpegImageToBitmap(mediaImage)

        val croppedBitmap =
            ImageUtils.rotateAndCrop(convertImageToBitmap, rotationDegrees, cropRect)

//        Log.d("===", "====================================")
//        Log.d("mediaImage", "$rotationDegrees width-height: $imageWidth * $imageHeight")
//        Log.d("cropRect", "$rotationDegrees width-height: ${cropRect.width()} * ${cropRect.height()}")
//        Log.d("cropRect", "$rotationDegrees ltrb: $cropRect")
//
//        Log.d("convertImageToBitmap", "width-height: ${convertImageToBitmap.width} * ${convertImageToBitmap.height}")
//        Log.d("croppedBitmap", "width-height: ${croppedBitmap.width} * ${croppedBitmap.height}")


        return croppedBitmap

    }


    fun loadRootPath(context: Context) {
        rootImgPath = context.getExternalFilesDir("genedImages")!!.absolutePath
        rootMbdPath = context.getExternalFilesDir("mbds")!!.absolutePath
        rootTmpPath = context.getExternalFilesDir("tmps")!!.absolutePath
        tmpRgbFilePath = "$rootTmpPath/tmp#.rgb"
    }

}


sealed class CameraUIAction {
    object OnCameraClick : CameraUIAction()
    object OnCancelCameraClick : CameraUIAction()
    object OnClearAllPrintsClick : CameraUIAction()
}
