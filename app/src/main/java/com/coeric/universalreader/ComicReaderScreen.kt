package com.coeric.universalreader

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.max
import kotlin.math.min

@Composable
fun ComicReaderScreen(
    pages: List<ComicPage>
) {

    var currentPage by remember {
        mutableIntStateOf(0)
    }

    var zoom by remember {
        mutableFloatStateOf(1f)
    }

    val safePage =
        if (pages.isEmpty()) {
            0
        } else {
            currentPage.coerceIn(
                0,
                pages.lastIndex
            )
        }

    LaunchedEffect(
        pages.size
    ) {

        if (pages.isNotEmpty()) {

            currentPage =
                currentPage.coerceIn(
                    0,
                    pages.lastIndex
                )
        }
    }

    if (pages.isEmpty()) {

        Box(
            modifier =
                Modifier.fillMaxSize(),

            contentAlignment =
                Alignment.Center
        ) {

            Text(
                text =
                    "No comic pages found."
            )
        }

        return
    }

    val page =
        pages[safePage]

    val bitmap =
        remember(page.file.absolutePath) {

            BitmapFactory.decodeFile(
                page.file.absolutePath
            )
        }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    MaterialTheme
                        .colorScheme
                        .background
                )
    ) {

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 4.dp
                    ),

            verticalAlignment =
                Alignment.CenterVertically,

            horizontalArrangement =
                Arrangement.SpaceBetween
        ) {

            IconButton(
                onClick = {
                    if (safePage > 0) {
                        currentPage--
                        zoom = 1f
                    }
                }
            ) {

                Icon(
                    imageVector =
                        Icons.Default.KeyboardArrowLeft,

                    contentDescription =
                        "Previous page"
                )
            }

            Text(
                text =
                    "${safePage + 1} / ${pages.size}",

                style =
                    MaterialTheme
                        .typography
                        .titleMedium
            )

            IconButton(
                onClick = {
                    if (
                        safePage <
                        pages.lastIndex
                    ) {
                        currentPage++
                        zoom = 1f
                    }
                }
            ) {

                Icon(
                    imageVector =
                        Icons.Default.KeyboardArrowRight,

                    contentDescription =
                        "Next page"
                )
            }
        }

        LinearProgressIndicator(
            progress = {
                (
                    (safePage + 1).toFloat() /
                        pages.size.toFloat()
                ).coerceIn(
                    0f,
                    1f
                )
            },

            modifier =
                Modifier.fillMaxWidth()
        )

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 8.dp,
                        vertical = 4.dp
                    ),

            horizontalArrangement =
                Arrangement.Center,

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            IconButton(
                onClick = {
                    zoom =
                        max(
                            1f,
                            zoom - 0.25f
                        )
                }
            ) {

                Icon(
                    imageVector =
                        Icons.Default.ZoomOut,

                    contentDescription =
                        "Zoom out"
                )
            }

            Text(
                text =
                    "${(zoom * 100).toInt()}%"
            )

            IconButton(
                onClick = {
                    zoom =
                        min(
                            4f,
                            zoom + 0.25f
                        )
                }
            ) {

                Icon(
                    imageVector =
                        Icons.Default.ZoomIn,

                    contentDescription =
                        "Zoom in"
                )
            }
        }

        if (bitmap == null) {

            Box(
                modifier =
                    Modifier.fillMaxSize(),

                contentAlignment =
                    Alignment.Center
            ) {

                Text(
                    text =
                        "Unable to display this page."
                )
            }

        } else {

            ComicPageImage(
                bitmap =
                    bitmap,

                zoom =
                    zoom
            )
        }
    }
}

@Composable
private fun ComicPageImage(
    bitmap: android.graphics.Bitmap,
    zoom: Float
) {

    Box(
        modifier =
            Modifier.fillMaxSize()
    ) {

        LazyColumn(
            modifier =
                Modifier.fillMaxSize()
        ) {

            item {

                Image(
                    bitmap =
                        bitmap.asImageBitmap(),

                    contentDescription =
                        null,

                    contentScale =
                        ContentScale.FillWidth,

                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(
                                min = 200.dp
                            )
                            .scale(zoom)
                            .pointerInput(
                                zoom
                            ) {

                                detectTransformGestures {
                                    _, _, scale, _ ->

                                        // Gesture handling is
                                        // intentionally kept
                                        // passive here.
                                        // The zoom buttons
                                        // control the reader
                                        // zoom level.
                                }
                            }
                )
            }
        }
    }
}