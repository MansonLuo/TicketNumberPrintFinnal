package com.example.ticketnumberprintfinnal

import android.Manifest
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.load.resource.drawable.DrawableResource
import com.example.ticketnumberprintfinnal.ui.theme.TicketNumberPrintFinnalTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions

class MainActivity : ComponentActivity() {
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
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun App() {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        var imgUri by remember {
            mutableStateOf<Uri?>(null)
        }

        CameraView(
            onImageCaptured = { uri, fromGallery ->
                Log.d("Main", "Image Uri Captured from Camera View: $uri")
                imgUri = uri
            },
            onError = { imageCaptureException ->
                Log.e("Main", "error")
            }
        )

        AndroidView(
            factory =  {
                val view = View(it).apply {
                    setBackgroundResource(R.drawable.background_drawable)
                }

                view
            },
            modifier = Modifier
                .width(350.dp)
                .height(100.dp)
                .align(Alignment.Center)
        )

        if (imgUri != null) {
            GlideImage(
                model = imgUri,
                contentDescription = "",
                modifier = Modifier.size(200.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AppPreview() {
    App()
}
