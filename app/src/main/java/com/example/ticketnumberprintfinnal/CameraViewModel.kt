package com.example.ticketnumberprintfinnal

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.media.MediaActionSound
import android.net.Uri
import android.os.Build
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
import com.cherryleafroad.kmagick.Magick
import com.example.ticketnumberprintfinnal.api.MbrushRepository
import com.example.ticketnumberprintfinnal.api.WorkingStatu
import com.example.ticketnumberprintfinnal.api.models.Status
import com.example.ticketnumberprintfinnal.extentions.ContextExts.Companion.deleteAllMbdFile
import com.example.ticketnumberprintfinnal.extentions.ContextExts.Companion.deleteTmpRgbFile
import com.example.ticketnumberprintfinnal.extentions.extractTicketNumber
import com.example.ticketnumberprintfinnal.extentions.getRandomFileName
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
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
    config: CameraConfig, private val mbrushRepository: MbrushRepository
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
        return coroutineScope {
            async {
                val imageProxy = imageCapture.takePhotoAsync()
                val photoFile = File(
                    outputDirectory, SimpleDateFormat(
                        filenameFormat, Locale.US
                    ).format(System.currentTimeMillis()) + ".jpg"
                )

                if (imageProxy.image == null) {
                    imageProxy.close()

                    return@async null
                }


                withContext(Dispatchers.IO) {
                    val bitmap = cropTextImage(imageProxy) ?: return@withContext
                    bitmap.saveToFile(photoFile)
                    bitmap.recycle()
                }

                imageProxy.close()

                Uri.fromFile(photoFile)
            }.await()
        }
    }

    suspend fun recognizeTicketNumberAsync(
        context: Context, imageUri: Uri
    ): List<String> = suspendCoroutine { continuation ->


        val bitmap = if (Build.VERSION.SDK_INT < 28) {
            MediaStore.Images.Media.getBitmap(
                context.contentResolver, imageUri
            )
        } else {
            val source = ImageDecoder.createSource(context.contentResolver, imageUri)
            ImageDecoder.decodeBitmap(source)
        }
        Log.e("Main", (bitmap == null).toString())
        val inputImageCrop = InputImage.fromBitmap(bitmap, 0)

        textRecognizer.process(inputImageCrop).addOnSuccessListener { visionText ->
            val text = visionText.text

            for (block in visionText.textBlocks) {
                for (line in block.lines) {
                    val res = line.text.extractTicketNumber()

                    if (res.isNotBlank()) {
                        _recognizedTicketNumbers.add(res)
                    }
                }
            }

            Log.d("zzz", "textRecognizer onSuccess")
            Log.d("zzzzzz OCR result", "ocr result: $text")

            bitmap.recycle()

            continuation.resume(_recognizedTicketNumbers.toList())
        }.addOnFailureListener { exception ->
            Log.d("zzz", "onFailure")


            bitmap.recycle()
            continuation.resumeWithException(exception)
        }
    }

    suspend fun uploadNumbers(
        context: Context,
        numbers: List<String>,
    ) {
        _sendResultList.clear()
        val len = numbers.size

        coroutineScope {
            withContext(Dispatchers.IO) {
                // generate images
                val deffered = numbers.mapIndexed() { index, ticketNumber ->
                    async {
                        ticketNumber.saveGeneratedWhiteJpgTo(rootImgPath, index.toString())
                    }
                }
                val generatedImagePaths = deffered.map {
                    it.await()
                }

                // save tmp.rgb files
                // each coroutine will save one tmp.rgb file.
                Magick.initialize()
                generatedImagePaths.mapIndexed { index, path ->
                    launch {
                        path.transformAndSaveToTmpRgb(context, rootTmpPath, index.toString())
                    }
                }.forEach { it.join() }
                Magick.terminate()

                // save to mbd.file
                (0 until len).mapIndexed { index, s ->
                    launch {
                        withContext(Dispatchers.Default) {
                            (context as MainActivity).generateMBDFile(
                                tmpRgbFilePath.replace("#", index.toString()),
                                "$rootMbdPath/${index}.mbd"
                            )
                        }
                    }
                }.forEach { it.join() }

                // send mbd files
                (0 until len).map { index ->
                    launch {
                        withContext(Dispatchers.Default) {
                            mbrushRepository.upload(
                                mbdFilePath = "$rootMbdPath/$index.mbd",
                                index
                            )

                            coroutineContext[Job]?.invokeOnCompletion {
                                if (0 == index) {
                                    MediaActionSound().play(MediaActionSound.STOP_VIDEO_RECORDING)
                                }
                                _sendResultList.add("发送结果: OK")
                            }
                        }
                    }
                }.forEach { it.join() }
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

    suspend fun simulateShortPress(): Status {
        return coroutineScope {
            async {
                withContext(Dispatchers.IO) {
                    val status = mbrushRepository.simulateShortPress()

                    status
                }
            }.await()
        }
    }

    suspend fun getPrinterStatu(): WorkingStatu {
        return coroutineScope {
            async {
                withContext(Dispatchers.IO) {
                    mbrushRepository.getPrinterStatu()
                }
            }.await()
        }
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
                    width = cropSizeScale.value.height, height = cropSizeScale.value.width
                ),
            ).roundToAndroidRect()

            else -> getCropRect(
                imageWidth.toFloat(), imageHeight.toFloat(),

                topLeftScale = cropTopLeftScale.value, sizeScale = cropSizeScale.value
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
