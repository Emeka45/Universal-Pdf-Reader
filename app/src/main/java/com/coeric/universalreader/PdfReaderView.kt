package com.coeric.universalreader

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

@Composable
fun PdfReaderView(
    uri: Uri
) {

    val context =
        LocalContext.current

    var currentPage by remember {
        mutableIntStateOf(0)
    }

    var zoom by remember {
        mutableFloatStateOf(1f)
    }

    var pageCount by remember {
        mutableIntStateOf(0)
    }

    var error by remember {
        mutableStateOf<String?>(null)
    }

    var positionLoaded by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(uri) {

        try {

            val savedPosition =
                ReadingPositionRepository.get(
                    context,
                    uri.toString()
                )

            pageCount =
                withContext(
                    Dispatchers.IO
                ) {

                    val descriptor =
                        context
                            .contentResolver
                            .openFileDescriptor(
                                uri,
                                "r"
                            )
                            ?: throw IllegalArgumentException(
                                "Unable to open PDF."
                            )

                    descriptor.use {

                        PdfRenderer(it).use { renderer ->

                            renderer.pageCount
                        }
                    }
                }

            if (
                pageCount > 0 &&
                savedPosition != null
            ) {

                currentPage =
                    savedPosition.chapterIndex
                        .coerceIn(
                            0,
                            pageCount - 1
                        )
            }

            positionLoaded = true

        } catch (
            exception: Exception
        ) {

            error =
                exception.message
                    ?: "Unable to open PDF."
        }
    }

    val bitmapState =
        produceState<Bitmap?>(
            initialValue = null,
            key1 = uri,
            key2 = currentPage,
            key3 = zoom
        ) {

            if (
                pageCount <= 0 ||
                !positionLoaded
            ) {
                value = null
                return@produceState
            }

            value =
                try {

                    withContext(
                        Dispatchers.IO
                    ) {

                        val descriptor =
                            context
                                .contentResolver
                                .openFileDescriptor(
                                    uri,
                                    "r"
                                )
                                ?: return@withContext null

                        descriptor.use {

                            PdfRenderer(
                                it
                            ).use { renderer ->

                                if (
                                    currentPage !in
                                    0 until renderer.pageCount
                                ) {
                                    return@use null
                                }

                                renderer
                                    .openPage(
                                        currentPage
                                    )
                                    .use { page ->

                                        val width =
                                            (
                                                page.width *
                                                    zoom
                                            )
                                                .toInt()
                                                .coerceAtLeast(
                                                    1
                                                )

                                        val height =
                                            (
                                                page.height *
                                                    zoom
                                            )
                                                .toInt()
                                                .coerceAtLeast(
                                                    1
                                                )

                                        val bitmap =
                                            Bitmap.createBitmap(
                                                width,
                                                height,
                                                Bitmap.Config.ARGB_8888
                                            )

                                        page.render(
                                            bitmap,
                                            null,
                                            null,
                                            PdfRenderer
                                                .Page
                                                .RENDER_MODE_FOR_DISPLAY
                                        )

                                        bitmap
                                    }
                            }
                        }
                    }

                } catch (
                    exception: Exception
                ) {

                    error =
                        exception.message
                            ?: "Unable to render PDF page."

                    null
                }
        }

    LaunchedEffect(
        currentPage,
        pageCount,
        positionLoaded
    ) {

        if (
            positionLoaded &&
            pageCount > 0
        ) {

            ReadingPositionRepository.save(

                context,

                ReadingPosition(
                    documentUri =
                        uri.toString(),

                    chapterIndex =
                        currentPage,

                    scrollIndex =
                        0,

                    scrollOffset =
                        0
                )
            )
        }
    }

    when {

        error != null -> {

            PdfErrorScreen(
                message =
                    error
                        ?: "Unable to open PDF."
            )
        }

        pageCount == 0 ||
        !positionLoaded -> {

            Box(
                modifier =
                    Modifier.fillMaxSize(),

                contentAlignment =
                    Alignment.Center
            ) {

                CircularProgressIndicator()
            }
        }

        else -> {

            val safePage =
                currentPage.coerceIn(
                    0,
                    pageCount - 1
                )

            val bitmap =
                bitmapState.value

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
                        Modifier.fillMaxWidth(),

                    verticalAlignment =
                        Alignment.CenterVertically,

                    horizontalArrangement =
                        Arrangement.SpaceBetween
                ) {

                    IconButton(
                        onClick = {

                            if (
                                safePage > 0
                            ) {

                                currentPage--

                                zoom = 1f
                            }
                        }
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default
                                    .KeyboardArrowLeft,

                            contentDescription =
                                "Previous page"
                        )
                    }

                    Text(
                        text =
                            "${safePage + 1} / $pageCount",

                        style =
                            MaterialTheme
                                .typography
                                .titleMedium
                    )

                    IconButton(
                        onClick = {

                            if (
                                safePage <
                                pageCount - 1
                            ) {

                                currentPage++

                                zoom = 1f
                            }
                        }
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default
                                    .KeyboardArrowRight,

                            contentDescription =
                                "Next page"
                        )
                    }
                }

                LinearProgressIndicator(
                    progress = {

                        (
                            (safePage + 1).toFloat() /
                                pageCount.toFloat()
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
                        Modifier.fillMaxWidth(),

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

                if (
                    bitmap == null
                ) {

                    Box(
                        modifier =
                            Modifier.fillMaxSize(),

                        contentAlignment =
                            Alignment.Center
                    ) {

                        CircularProgressIndicator()
                    }

                } else {

                    LazyColumn(
                        modifier =
                            Modifier.fillMaxSize()
                    ) {

                        item {

                            Image(
                                bitmap =
                                    bitmap.asImageBitmap(),

                                contentDescription =
                                    "PDF page ${safePage + 1}",

                                contentScale =
                                    ContentScale.FillWidth,

                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .scale(1f)
                                        .pointerInput(
                                            zoom
                                        ) {

                                            detectTransformGestures {
                                                _, _, _, _ ->
                                            }
                                        }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PdfErrorScreen(
    message: String
) {

    Box(
        modifier =
            Modifier.fillMaxSize(),

        contentAlignment =
            Alignment.Center
    ) {

        Text(
            text =
                message
        )
    }
}