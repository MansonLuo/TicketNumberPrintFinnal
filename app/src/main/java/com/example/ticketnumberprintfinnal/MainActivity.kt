package com.example.ticketnumberprintfinnal

import android.Manifest
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.example.ticketnumberprintfinnal.ui.theme.TicketNumberPrintFinnalTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

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
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        CameraView(
            onImageCaptured = { uri, fromGallery ->
                Log.d("Main", "Image Uri Captured from Camera View: $uri")
            },
            onError = { imageCaptureException ->
                Log.e("Main", "error")
            }
        )


    }
}

@Preview(showBackground = true)
@Composable
fun AppPreview() {
    App()
}
