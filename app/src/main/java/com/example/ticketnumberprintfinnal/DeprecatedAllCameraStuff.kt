package com.example.ticketnumberprintfinnal

import android.net.Uri
import android.util.Rational
import android.view.OrientationEventListener
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout.LayoutParams
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.ViewPort
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Cancel
import androidx.compose.material.icons.sharp.ClearAll
import androidx.compose.material.icons.sharp.Lens
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ticketnumberprintfinnal.api.MbrushRepository
import com.example.ticketnumberprintfinnal.api.RetrofitInstance
import com.example.ticketnumberprintfinnal.extentions.ContextExts.Companion.getCameraProvider
import com.example.ticketnumberprintfinnal.extentions.toPx
import kotlinx.coroutines.launch

@Composable
fun DeprecatedCameraView(onImageCaptured: (Uri) -> Unit, onError: (ImageCaptureException) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val mbrushRepository = remember {
        MbrushRepository(RetrofitInstance.mBrushService)
    }
    val viewmodel = remember {
        val vm = DeprecatedCameraViewModel(mbrushRepository)
        vm.deprecatedLoadRootPath(context = context)

        vm
    }


    var rotation = remember {
        Surface.ROTATION_0
    }
    val lensFacing by remember {
        mutableStateOf(CameraSelector.LENS_FACING_BACK)
    }

    val imageCapture: ImageCapture = remember {
        ImageCapture
            .Builder()
            .build()
    }

    val orientationEventListener = remember {
        object : OrientationEventListener(context) {
            override fun onOrientationChanged(orientation: Int) {
                //val rotation: Int = when (orientation) {
                rotation = when (orientation) {
                    in 45..134 -> Surface.ROTATION_270
                    in 135..224 -> Surface.ROTATION_180
                    in 225..314 -> Surface.ROTATION_90
                    else -> Surface.ROTATION_0
                }

                imageCapture.targetRotation = rotation
                //Log.d("Main", "ratation: $rotation")
            }
        }
    }

    orientationEventListener.enable()

    CameraPreviewView(
        imageCapture,
        rotation,
        viewmodel,
    ) { cameraUIAction ->
        /*
        val myBeforeOnImageCapture: (Uri) -> Unit = { uri ->
            scope.launch {
                viewmodel.recognizeTicketNumber(
                    context,
                    uri,
                    onSuccess = { visionText ->
                        // suppose only got one line text
                        // otherwise will throw exception
                        val allLines = mutableListOf<String>()

                        for (block in visionText.textBlocks) {
                            for (line in block.lines) {
                                allLines.add(line.text)
                            }
                        }


                        if (allLines.isEmpty()) {
                            viewmodel.resetState()

                            viewmodel.transformText(
                                "无法识别",
                                context
                            )
                            scope.launch {
                                viewmodel.send()
                            }

                            viewmodel.recognizedNumber.value = "无法识别"
                            viewmodel.sendResult.value = ""
                        } else {
                            allLines.subList(0, viewmodel.currentNumberOfTickets)
                                .forEach { recognizedTextLine ->
                                    val ticketNumber = recognizedTextLine.extractTicketNumber()
                                    viewmodel.resetState()

                                    viewmodel.transformText(
                                        ticketNumber,
                                        context
                                    )
                                    scope.launch {
                                        viewmodel.send()
                                    }

                                    viewmodel.recognizedNumber.value = ticketNumber
                                    viewmodel.sendResult.value = ""
                                }
                        }
                    },
                )
                onImageCaptured(uri)
            }
        }
         */

        when (cameraUIAction) {
            is DeprecatedCameraUIAction.OnCameraClick -> {
                /*
                scope.launch {
                    imageCapture.takePicture(
                        context,
                        lensFacing,
                        myBeforeOnImageCapture,
                        onError
                    )
                }
                */
                viewmodel.deprectaedTakePictureAndSendMbdFiles(
                    context,
                    imageCapture
                )
            }

            is DeprecatedCameraUIAction.OnCancelCameraClick -> {
                (context as MainActivity).finish()
            }

            is DeprecatedCameraUIAction.OnClearAllPrintsClick -> {
                scope.launch {
                    viewmodel.deprecatedRemoveUpload(context)
                }
            }
        }
    }
}

@Composable
private fun CameraPreviewView(
    imageCapture: ImageCapture,
    rotation: Int,
    viewModel: DeprecatedCameraViewModel,
    cameraUIAction: (DeprecatedCameraUIAction) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val cropBoxHeight = LocalConfiguration.current.screenHeightDp / 3

    val preview = remember {
        Preview
            .Builder()
            .build()
    }
    val cameraSelector = remember {
        CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()
    }

    val previewView = remember {
        val previewView = PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FIT_CENTER

            val layoutParams = LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setLayoutParams(layoutParams)
        }

        preview.setSurfaceProvider(previewView.surfaceProvider)

        previewView
    }

    val viewPort = remember {
        ViewPort.Builder(
            Rational(screenWidthDp.dp.toPx(context), cropBoxHeight.dp.toPx(context)),//viewModel.cropBoxHeight.toPx(context)),
            rotation
        ).build()
    }

    LaunchedEffect(CameraSelector.LENS_FACING_BACK) {
        val cameraProvider = context.getCameraProvider()
        cameraProvider.unbindAll()
        val useCaseGroup = UseCaseGroup.Builder()
            .addUseCase(preview)
            .addUseCase(imageCapture)
            .setViewPort(viewPort)
            .build()
        cameraProvider.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            useCaseGroup
        )
        cameraProvider.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            preview,
            imageCapture
        )
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        AndroidView({ previewView }, modifier = Modifier.fillMaxSize())

        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            if (viewModel.recognizedNumberList.isNotEmpty()) {
                Column {
                    viewModel.recognizedNumberList.forEach {
                        Text(
                            text = it,
                            color = Color.White
                        )
                    }
                }
            }

            if (viewModel.sendResultList.isNotEmpty()) {
                Column {
                    viewModel.sendResultList.forEach {
                        Text(text = it, color = Color.White)
                    }
                }
            }
        }

        AndroidView(
            factory = {
                val view = View(it).apply {
                    setBackgroundResource(R.drawable.background_drawable)
                }

                view
            },
            modifier = Modifier
                .width(screenWidthDp.dp)
                .height(cropBoxHeight.dp)
                .align(Alignment.Center)
        )

        Column(
            modifier = Modifier.align(Alignment.BottomCenter),
            verticalArrangement = Arrangement.Bottom
        ) {
            DeprecatedCameraControls(cameraUIAction)
        }
    }
}


@Composable
fun DeprecatedCameraControls(
    cameraUIAction: (DeprecatedCameraUIAction) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        DeprecatedCameraControl(
            Icons.Sharp.Cancel,
            R.string.icn_camera_view_cancel_camera_description,
            modifier = Modifier
                .size(64.dp)
                .padding(1.dp)
                .border(1.dp, Color.White, CircleShape),
            onClick = {
                cameraUIAction(DeprecatedCameraUIAction.OnCancelCameraClick)
            }
        )
        DeprecatedCameraControl(
            Icons.Sharp.Lens,
            R.string.icn_camera_view_camera_shutter_content_description,
            modifier = Modifier
                .size(64.dp)
                .padding(1.dp)
                .border(1.dp, Color.White, CircleShape),
            onClick = {
                cameraUIAction(DeprecatedCameraUIAction.OnCameraClick)
            }
        )
        DeprecatedCameraControl(
            Icons.Sharp.ClearAll,
            R.string.icn_clear_all_prints_description,
            modifier = Modifier
                .size(64.dp)
                .padding(1.dp)
                .border(1.dp, Color.White, CircleShape),
            onClick = {
                cameraUIAction(DeprecatedCameraUIAction.OnClearAllPrintsClick)
            }
        )
    }
}

@Composable
fun DeprecatedCameraControl(
    imageVector: ImageVector,
    contentDescId: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
    ) {
        Icon(
            imageVector,
            contentDescription = stringResource(id = contentDescId),
            modifier = modifier,
            tint = Color.White
        )
    }
}

sealed class DeprecatedCameraUIAction {
    object OnCameraClick : DeprecatedCameraUIAction()
    object OnCancelCameraClick : DeprecatedCameraUIAction()
    object OnClearAllPrintsClick : DeprecatedCameraUIAction()
}