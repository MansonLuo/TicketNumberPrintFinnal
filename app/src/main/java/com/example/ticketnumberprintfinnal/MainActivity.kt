package com.example.ticketnumberprintfinnal

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.example.ticketnumberprintfinnal.api.MbrushRepository
import com.example.ticketnumberprintfinnal.api.RetrofitInstance
import com.example.ticketnumberprintfinnal.components.CameraControls
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

                        viewModel.updateRecognizedText(
                            recognizedNumbers.joinToString {
                            "$it/n"
                            }
                        )

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

    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = Modifier
            .fillMaxSize()
    ) {
        CameraView(
            preview = viewModel.preview,
            imageCapture = viewModel.imageCapture,
            enableTorchProvider = { viewModel.enableTorch.value },
            modifier = Modifier
                .fillMaxSize()
        )

        // 裁剪区域
        DrawCropScan(
            topLeftScaleProvider = { viewModel.cropTopLeftScale.value },
            sizeScaleProvider = { viewModel.cropSizeScale.value }
        )

        // show cropped bitmap provided by taking picture
        if (viewModel.bitmapREnabled.value) {
            if (viewModel.bitmapR.value != null) {
                ShowAfterCropImageToAnalysis(bitmapProvider = { viewModel.bitmapR.value!! })
            }
        }

        // show send results
        Text(
            text = viewModel.sendResultList.joinToString { "$it\n" },
            modifier = Modifier
                .align(alignment = Alignment.TopEnd)
                .padding(horizontal = 10.dp, vertical = 10.dp)
                .heightIn(max = 150.dp)
                .widthIn(min = 100.dp)
                .background(Color.Transparent.copy(alpha = 0.6f)),
            color = Color.Red,
            textAlign = TextAlign.Right
        )

        // show recognized text
        Text(
            text = viewModel.scanText.value,
            modifier = Modifier
                .align(alignment = Alignment.BottomStart)
                .padding(horizontal = 10.dp, vertical = 150.dp)
                .heightIn(max = 150.dp)
                .widthIn(min = 100.dp)
                .background(Color.Transparent.copy(alpha = 0.6f)),
            color = Color.Red,
            textAlign = TextAlign.Left
        )

        // Bottom controls
        Column(
            modifier = Modifier.align(Alignment.BottomCenter),
            verticalArrangement = Arrangement.Bottom
        ) {
            CameraControls(cameraUIAction)
        }

    }
}

@Preview(showBackground = true)
@Composable
fun AppPreview() {
    App()
}
