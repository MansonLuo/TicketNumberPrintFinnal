package com.example.ticketnumberprintfinnal.screens.camera

import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ticketnumberprintfinnal.CameraUIAction
import com.example.ticketnumberprintfinnal.CameraView
import com.example.ticketnumberprintfinnal.DrawCropScan
import com.example.ticketnumberprintfinnal.screens.camera.components.CameraControls
import com.example.ticketnumberprintfinnal.screens.camera.components.CropBoxResizer

@Composable
fun CameraScreen(
    preview: Preview,
    imageCapture: ImageCapture,

    topLeftScaleProvider: () -> Offset,
    sizeScaleProvider: () -> Size,
    onCropBoxRequestExpand: (Boolean) -> Unit,

    recognizedTicketNumbersProvider: () -> String,
    sendResultListProvider: () -> String,

    cameraUIAction: (CameraUIAction) -> Unit
) {
    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = Modifier
            .fillMaxSize()
    ) {
        CameraView(
            preview = preview,
            imageCapture = imageCapture,
            modifier = Modifier
                .fillMaxSize()
        )

        // 裁剪区域
        DrawCropScan(
            topLeftScaleProvider = topLeftScaleProvider,
            sizeScaleProvider = sizeScaleProvider
        )

        // show recognized text
        Text(
            text = recognizedTicketNumbersProvider(),
            modifier = Modifier
                .align(alignment = Alignment.TopStart)
                .padding(horizontal = 10.dp, vertical = 10.dp)
                .heightIn(max = 300.dp)
                .widthIn(min = 100.dp)
                .background(Color.Transparent.copy(alpha = 0.6f)),
            color = Color.Red,
            textAlign = TextAlign.Left
        )

        // show send results
        Text(
            text = sendResultListProvider(),
            modifier = Modifier
                .align(alignment = Alignment.TopEnd)
                .padding(horizontal = 10.dp, vertical = 10.dp)
                .heightIn(max = 300.dp)
                .widthIn(min = 100.dp)
                .background(Color.Transparent.copy(alpha = 0.6f)),
            color = Color.Red,
            textAlign = TextAlign.Right
        )


        // Bottom controls
        Column(
            modifier = Modifier.align(Alignment.BottomCenter),
            verticalArrangement = Arrangement.Bottom
        ) {
            CropBoxResizer(
                modifier = Modifier
                    .size(64.dp)
                    .padding(1.dp)
                    .border(1.dp, Color.White, CircleShape),
                onRequestResize = onCropBoxRequestExpand
            )
            CameraControls(cameraUIAction)
        }

    }
}