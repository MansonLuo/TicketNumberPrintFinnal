package com.example.ticketnumberprintfinnal.screens.camera.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun CropBoxResizer(
    modifier: Modifier = Modifier,
    onRequestResize: (Boolean) -> Unit
) {
    val expanded = remember {
        mutableStateOf(false)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            modifier = modifier,
            onClick = {
                expanded.value = !expanded.value
                onRequestResize(expanded.value)
            }
        ) {
            Icon(
                modifier = modifier,
                imageVector = if (expanded.value) {
                    Icons.Filled.UnfoldLess
                } else {
                    Icons.Filled.UnfoldMore
                },
                contentDescription = "",
                tint = Color.White
            )
        }
    }
}

@Preview
@Composable
fun Preview_CropBoxResizer() {
    CropBoxResizer(onRequestResize = {})
}