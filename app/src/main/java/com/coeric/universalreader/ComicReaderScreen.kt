package com.coeric.universalreader

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max
import kotlin.math.min

@Composable
fun ComicReaderScreen(
    pages: List<ComicPage>,
    documentUri: String
) {

    val context = LocalContext.current

    var currentPage by remember {
        mutableIntStateOf(
            ComicReadingPositionRepository
                .get(
                    context,
                    documentUri
                )
                ?.pageIndex
                ?.coerceIn(
                    0,
                    max(0, pages.lastIndex)
                )
                ?: 0
        )
    }

    var zoom by remember {
        mutableFloatStateOf(1f)
    }

    var offsetX by remember {
        mutableFloatStateOf(0f)
    }

    var offsetY by remember {
        mutableFloatStateOf(0f)
    }

    LaunchedEffect(
        currentPage,
        documentUri
    ) {

        ComicReadingPositionRepository.save(
            context,
            ComicReadingPosition(
                documentUri = documentUri,
                pageIndex = currentPage
            )
        )
    }

    if (pages.isEmpty()) {

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {

            Text(
                text = "This comic contains no readable pages."
            )
        }

        return
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    MaterialTheme.colorScheme.background
                )
    ) {

        ComicTopBar(
            currentPage = currentPage,
            totalPages = pages.size,
            zoom = zoom,

            onPrevious = {

                if (currentPage > 0) {

                    currentPage--

                    zoom = 1f
                    offsetX = 0f
                    offsetY = 0f
                }
            },

            onNext = {

                if (currentPage < pages.lastIndex) {

                    currentPage++

                    zoom = 1f
                    offsetX = 0f
                    offsetY = 0f
                }
            },

            onZoomOut = {

                zoom =
                    (zoom - 0.25f)
                        .coerceAtLeast(1f)

                if (zoom == 1f) {
                    offsetX = 0f
                    offsetY = 0f
                }
            },

            onZoomIn = {

                zoom =
                    (zoom + 0.25f)
                        .coerceAtMost(4f)
            }
        )

        LinearProgressIndicator(
            progress = {
                if (pages.size <= 1) {
                    1f
                } else {
                    currentPage.toFloat() /
                        pages.lastIndex.toFloat()
                }
            },

            modifier =
                Modifier
                    .fillMaxWidth()
        )

        ComicPageView(
            page = pages[currentPage],

            zoom = zoom,

            offsetX = offsetX,
            offsetY = offsetY,

            onTransform = { gestureZoom, gestureX, gestureY ->

                val newZoom =
                    (zoom * gestureZoom)
                        .coerceIn(1f, 4f)

                zoom = newZoom

                if (newZoom <= 1f) {

                    offsetX = 0f
                    offsetY = 0f

                } else {

                    offsetX += gestureX
                    offsetY += gestureY
                }
            },

            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
        )

        Text(
            text =
                pages[currentPage].name,

            style =
                MaterialTheme.typography.labelMedium,

            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 12.dp,
                        vertical = 6.dp
                    )
        )
    }
}

@Composable
private fun ComicTopBar(
    currentPage: Int,
    totalPages: Int,
    zoom: Float,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onZoomOut: () -> Unit,
    onZoomIn: () -> Unit
) {

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 8.dp,
                    vertical = 4.dp
                ),

        verticalAlignment =
            Alignment.CenterVertically,

        horizontalArrangement =
            Arrangement.SpaceBetween
    ) {

        Row(
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            IconButton(
                onClick = onPrevious,
                enabled = currentPage > 0
            ) {

                Icon(
                    imageVector =
                        Icons.Default.ChevronLeft,

                    contentDescription =
                        "Previous page"
                )
            }

            Text(
                text =
                    "${currentPage + 1} / $totalPages",

                style =
                    MaterialTheme.typography.titleMedium
            )

            IconButton(
                onClick = onNext,
                enabled =
                    currentPage < totalPages - 1
            ) {

                Icon(
                    imageVector =
                        Icons.Default.ChevronRight,

                    contentDescription =
                        "Next page"
                )
            }
        }

        Row(
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            IconButton(
                onClick = onZoomOut,
                enabled = zoom > 1f
            ) {

                Icon(
                    imageVector =
                        Icons.Default.Remove,

                    contentDescription =
                        "Zoom out"
                )
            }

            Text(
                text =
                    "${(zoom * 100).toInt()}%",

                style =
                    MaterialTheme.typography.labelLarge
            )

            IconButton(
                onClick = onZoomIn,
                enabled = zoom < 4f
            ) {

                Icon(
                    imageVector =
                        Icons.Default.Add,

                    contentDescription =
                        "Zoom in"
                )
            }
        }
    }
}

@Composable
private fun ComicPageView(
    page: ComicPage,
    zoom: Float,
    offsetX: Float,
    offsetY: Float,
    onTransform: (
        Float,
        Float,
        Float
    ) -> Unit,
    modifier: Modifier = Modifier
) {

    var bitmap by remember(page.file) {
        androidx.compose.runtime.mutableStateOf<ImageBitmap?>(null)
    }

    LaunchedEffect(page.file) {

        bitmap =
            withContext(Dispatchers.IO) {

                BitmapFactory
                    .decodeFile(
                        page.file.absolutePath
                    )
                    ?.asImageBitmap()
            }
    }

    Box(
        modifier =
            modifier
                .clipToBounds()
                .pointerInput(page.file) {

                    detectTransformGestures {
                        _,
                        pan,
                        gestureZoom,
                        _ ->

                        onTransform(
                            gestureZoom,
                            pan.x,
                            pan.y
                        )
                    }
                },

        contentAlignment =
            Alignment.Center
    ) {

        val image =
            bitmap

        if (image == null) {

            Text(
                text = "Loading page..."
            )

        } else {

            Image(
                bitmap = image,

                contentDescription =
                    page.name,

                contentScale =
                    ContentScale.Fit,

                modifier =
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {

                            scaleX = zoom
                            scaleY = zoom

                            translationX =
                                offsetX

                            translationY =
                                offsetY
                        }
            )
        }
    }
}