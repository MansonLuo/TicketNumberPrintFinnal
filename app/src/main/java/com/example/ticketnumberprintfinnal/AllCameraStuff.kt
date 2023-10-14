package com.example.ticketnumberprintfinnal

import android.net.Uri
import android.util.Log
import android.util.Rational
import android.view.OrientationEventListener
import android.view.Surface
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ticketnumberprintfinnal.extentions.getCameraProvider
import com.example.ticketnumberprintfinnal.extentions.takePicture
import com.example.ticketnumberprintfinnal.extentions.toPx
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun CameraView(onImageCaptured: (Uri, Boolean) -> Unit, onError: (ImageCaptureException) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val viewmodel = viewModel<CameraViewModel>()


    var rotation = remember {
        Surface.ROTATION_0
    }
    val lensFacing by remember {
        mutableStateOf(CameraSelector.LENS_FACING_BACK)
    }

    val imageCapture: ImageCapture = remember {
        ImageCapture.Builder().build()
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
                Log.d("Main", "ratation: $rotation")
            }
        }
    }

    orientationEventListener.enable()

    CameraPreviewView(
        imageCapture,
        rotation,
        viewmodel,
    ) { cameraUIAction ->

        val myBeforeOnImageCapture: (Uri, Boolean) -> Unit = { uri, res ->
            scope.launch {
                viewmodel.recognizeTicketNumber(context, uri)
                onImageCaptured(uri, res)
            }
        }

        when (cameraUIAction) {
            is CameraUIAction.OnCameraClick -> {
                scope.launch {
                    imageCapture.takePicture(
                        context,
                        lensFacing,
                        myBeforeOnImageCapture,
                        onError)
                }
            }
        }
    }
}

@Composable
private fun CameraPreviewView(
    imageCapture: ImageCapture,
    rotation: Int,
    viewModel: CameraViewModel,
    cameraUIAction: (CameraUIAction) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val preview = Preview.Builder().build()
    val cameraSelector = remember {
        CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()
    }

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FIT_CENTER

            val layoutParams = LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setLayoutParams(layoutParams)
        }
    }
    preview.setSurfaceProvider(previewView.surfaceProvider)

    val viewPort = ViewPort.Builder(Rational(350.dp.toPx(context), 100.dp.toPx(context)), rotation).build()

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

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView({ previewView }, modifier = Modifier.fillMaxSize())

        viewModel.recognizedNumber.value?.let {  number ->
            Text(
                text = number,
                modifier = Modifier.align(Alignment.CenterStart)
            )
        }

        Column(
            modifier = Modifier.align(Alignment.BottomCenter),
            verticalArrangement = Arrangement.Bottom
        ) {
            CameraControls(cameraUIAction)
        }
    }
}

@Composable
fun CameraControls(
    cameraUIAction: (CameraUIAction) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black)
            .padding(16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CameraControl(
            Icons.Sharp.Lens,
            R.string.icn_camera_view_switch_camera_content_descriptioin,
            modifier = Modifier
                .size(64.dp)
                .padding(1.dp)
                .border(1.dp, Color.White, CircleShape),
            onClick = {
                cameraUIAction(CameraUIAction.OnCameraClick)
            }
        )
    }
}

@Composable
fun CameraControl(
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

sealed class CameraUIAction {
    object OnCameraClick: CameraUIAction()
}