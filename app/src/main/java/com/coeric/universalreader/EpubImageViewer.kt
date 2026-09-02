package com.coeric.universalreader

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

@Composable
fun EpubImageGallery(
    images: List<EpubImage>,
    modifier: Modifier = Modifier
) {

    if (
        images.isEmpty()
    ) {
        return
    }

    LazyColumn(
        modifier =
            modifier
                .fillMaxWidth(),

        verticalArrangement =
            Arrangement.spacedBy(
                16.dp
            )
    ) {

        items(
            items = images,
            key = {
                it.path
            }
        ) { image ->

            EpubImageItem(
                image = image
            )
        }
    }
}

@Composable
private fun EpubImageItem(
    image: EpubImage
) {

    val bitmap =
        remember(image.path) {

            BitmapFactory
                .decodeByteArray(
                    image.data,
                    0,
                    image.data.size
                )
        }

    if (
        bitmap == null
    ) {

        return
    }

    Column(
        modifier =
            Modifier.fillMaxWidth(),

        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        Image(
            bitmap =
                bitmap.asImageBitmap(),

            contentDescription =
                image.path
                    .substringAfterLast('/'),

            contentScale =
                ContentScale.Fit,

            modifier =
                Modifier
                    .fillMaxWidth()
                    .sizeIn(
                        maxHeight = 500.dp
                    )
        )

        Text(
            text =
                image.path
                    .substringAfterLast('/'),

            style =
                MaterialTheme
                    .typography
                    .labelSmall,

            modifier =
                Modifier.padding(
                    top = 4.dp
                )
        )
    }
}