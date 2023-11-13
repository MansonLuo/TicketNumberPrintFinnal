package com.example.ticketnumberprintfinnal

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.example.ticketnumberprintfinnal.api.MbrushRepository
import com.example.ticketnumberprintfinnal.api.RetrofitInstance
import com.example.ticketnumberprintfinnal.screens.camera.CameraScreen
import com.example.ticketnumberprintfinnal.ui.theme.TicketNumberPrintFinnalTheme
import com.example.ticketnumberprintfinnal.utils.AspectRatioCameraConfig
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    external fun generateMBDFile(rgbFilePath: String, mbdFilePath: String)

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        setContent {
            TicketNumberPrintFinnalTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val state = rememberMultiplePermissionsState(
                        permissions = listOf(
                            Manifest.permission.CAMERA,
                            //Manifest.permission.READ_EXTERNAL_STORAGE,
                            //Manifest.permission.WRITE_EXTERNAL_STORAGE
                        )
                    )
                    if (state.allPermissionsGranted) {
                        App()
                    } else {
                        RequiredPermission(state)
                    }
                }
            }
        }
    }

    companion object {
        init {
            System.loadLibrary("ticketnumberprintfinnal")
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun App() {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val viewModel = remember {
        val config = AspectRatioCameraConfig(context)
        val mbrushRepository = MbrushRepository(RetrofitInstance.mBrushService)
        val model = CameraViewModel(config, mbrushRepository)
        model.loadRootPath(context)
        model
    }

    val cameraUIAction: (CameraUIAction) -> Unit = { cameraUIAction ->
        when (cameraUIAction) {
            is CameraUIAction.OnCameraClick -> {

                scope.launch {
                    val imageUri = async {
                        viewModel.takePictureAsync(
                            outputDirectory = viewModel.getOutputDirectory(context)
                        )
                    }.await()

                    if (imageUri != null) {
                        val recognizedNumbers =
                            async { viewModel.recognizeTicketNumberAsync(context, imageUri) }.await()

                        if (recognizedNumbers.isEmpty())  {
                            viewModel.updateRecognizedText("无法识别")
                            viewModel.updateSendResult("未发送")

                            return@launch
                        }

                        viewModel.uploadNumbers(context, recognizedNumbers)
                    }
                }

            }

            is CameraUIAction.OnCancelCameraClick -> {
                (context as MainActivity).finish()
            }

            is CameraUIAction.OnClearAllPrintsClick -> {
                scope.launch {
                    viewModel.removeUpload(context)
                }
            }
        }
    }

    CameraScreen(
        preview = viewModel.preview,
        imageCapture = viewModel.imageCapture,
        enableTouchProvider = { viewModel.enableTorch.value },

        bitmapREnabledProvider = { viewModel.bitmapREnabled },
        bitmapRProvider = { viewModel.bitmapR },

        topLeftScaleProvider = { viewModel.cropTopLeftScale.value },
        sizeScaleProvider = { viewModel.cropSizeScale.value },
        onCropBoxRequestExpand = { onRequestExpand ->
            if (onRequestExpand) {
                viewModel.expandCropBox()
            } else {
                viewModel.shrinkCropBox()
            }
        },

        recognizedTicketNumbersProvider = { viewModel.recognizedTicketNumbers },
        sendResultListProvider = { viewModel.sendResultList },

        cameraUIAction = cameraUIAction
    )
}

@Preview(showBackground = true)
@Composable
fun AppPreview() {
    App()
}
