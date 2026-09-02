package com.coeric.universalreader

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun PdfReaderView(
    uri: Uri
) {

    val context =
        androidx.compose.ui.platform.LocalContext.current

    val scope =
        rememberCoroutineScope()

    var renderer by remember {
        mutableStateOf<PdfRenderer?>(null)
    }

    var descriptor by remember {
        mutableStateOf<ParcelFileDescriptor?>(null)
    }

    var temporaryFile by remember {
        mutableStateOf<File?>(null)
    }

    var pageCount by remember {
        mutableIntStateOf(0)
    }

    var currentPage by remember {
        mutableIntStateOf(0)
    }

    var bitmap by remember {
        mutableStateOf<Bitmap?>(null)
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

    var showSearch by remember {
        mutableStateOf(false)
    }

    var loading by remember {
        mutableStateOf(true)
    }

    var error by remember {
        mutableStateOf<String?>(null)
    }

    LaunchedEffect(uri) {

        loading =
            true

        error =
            null

        try {

            val file =
                withContext(
                    Dispatchers.IO
                ) {

                    val target =
                        File.createTempFile(
                            "universal_reader_pdf_",
                            ".pdf",
                            context.cacheDir
                        )

                    context
                        .contentResolver
                        .openInputStream(uri)
                        ?.use { input ->

                            target
                                .outputStream()
                                .use { output ->

                                    input.copyTo(
                                        output,
                                        64 * 1024
                                    )
                                }
                        }
                        ?: throw IllegalArgumentException(
                            "Unable to open PDF."
                        )

                    target
                }

            temporaryFile =
                file

            val fd =
                ParcelFileDescriptor
                    .open(
                        file,
                        ParcelFileDescriptor.MODE_READ_ONLY
                    )

            descriptor =
                fd

            val pdf =
                PdfRenderer(fd)

            renderer =
                pdf

            pageCount =
                pdf.pageCount

        } catch (
            exception: Exception
        ) {

            error =
                exception.message
                    ?: "Unable to open PDF."

        } finally {

            loading =
                false
        }
    }

    LaunchedEffect(
        renderer,
        currentPage
    ) {

        val pdf =
            renderer
                ?: return@LaunchedEffect

        if (
            currentPage !in
            0 until pdf.pageCount
        ) {
            return@LaunchedEffect
        }

        loading =
            true

        bitmap =
            withContext(
                Dispatchers.IO
            ) {

                val page =
                    pdf.openPage(
                        currentPage
                    )

                try {

                    val width =
                        (
                            page.width *
                                2f
                        ).toInt()

                    val height =
                        (
                            page.height *
                                2f
                        ).toInt()

                    Bitmap.createBitmap(
                        width,
                        height,
                        Bitmap.Config.ARGB_8888
                    ).also { result ->

                        page.render(
                            result,
                            null,
                            null,
                            PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                        )
                    }

                } finally {

                    page.close()
                }
            }

        zoom =
            1f

        offsetX =
            0f

        offsetY =
            0f

        loading =
            false
    }

    androidx.compose.runtime.DisposableEffect(Unit) {

        onDispose {

            renderer?.close()
            descriptor?.close()
            temporaryFile?.delete()
        }
    }

    Surface(
        modifier =
            Modifier.fillMaxSize()
    ) {

        Column(
            modifier =
                Modifier.fillMaxSize()
        ) {

            PdfToolbar(
                currentPage =
                    currentPage,

                pageCount =
                    pageCount,

                zoom =
                    zoom,

                onSearch = {
                    showSearch = true
                },

                onZoomIn = {

                    zoom =
                        (
                            zoom + 0.25f
                        ).coerceAtMost(
                            4f
                        )
                },

                onZoomOut = {

                    zoom =
                        (
                            zoom - 0.25f
                        ).coerceAtLeast(
                            1f
                        )

                    if (
                        zoom == 1f
                    ) {

                        offsetX =
                            0f

                        offsetY =
                            0f
                    }
                }
            )

            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(
                            MaterialTheme
                                .colorScheme
                                .surfaceVariant
                        ),

                contentAlignment =
                    Alignment.Center
            ) {

                when {

                    error != null -> {

                        Text(
                            text =
                                error
                                    ?: "Unable to open PDF.",

                            modifier =
                                Modifier.padding(
                                    24.dp
                                )
                        )
                    }

                    loading -> {

                        CircularProgressIndicator()
                    }

                    bitmap != null -> {

                        Box(
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .pointerInput(
                                        zoom
                                    ) {

                                        detectTransformGestures {
                                                _,
                                                pan,
                                                scale,
                                                _ ->

                                            zoom =
                                                (
                                                    zoom *
                                                        scale
                                                ).coerceIn(
                                                    1f,
                                                    4f
                                                )

                                            if (
                                                zoom > 1f
                                            ) {

                                                offsetX +=
                                                    pan.x

                                                offsetY +=
                                                    pan.y

                                            } else {

                                                offsetX =
                                                    0f

                                                offsetY =
                                                    0f
                                            }
                                        }
                                    }
                                    .verticalScroll(
                                        rememberScrollState()
                                    )
                        ) {

                            Image(
                                bitmap =
                                    bitmap!!
                                        .asImageBitmap(),

                                contentDescription =
                                    "PDF page ${currentPage + 1}",

                                contentScale =
                                    ContentScale.Fit,

                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .wrapContentHeight()
                                        .graphicsLayer {

                                            scaleX =
                                                zoom

                                            scaleY =
                                                zoom

                                            translationX =
                                                offsetX

                                            translationY =
                                                offsetY
                                        }
                            )
                        }
                    }
                }
            }

            PdfNavigationBar(
                currentPage =
                    currentPage,

                pageCount =
                    pageCount,

                onPrevious = {

                    if (
                        currentPage > 0
                    ) {

                        currentPage--
                    }
                },

                onNext = {

                    if (
                        currentPage <
                        pageCount - 1
                    ) {

                        currentPage++
                    }
                }
            )
        }
    }

    if (
        showSearch
    ) {

        Dialog(
            onDismissRequest = {
                showSearch = false
            }
        ) {

            Surface(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),

                shape =
                    MaterialTheme
                        .shapes
                        .large
            ) {

                PdfSearchPanel(
                    uri = uri,

                    onClose = {
                        showSearch = false
                    },

                    onResultSelected = {
                        page ->

                        currentPage =
                            page

                        showSearch =
                            false
                    }
                )
            }
        }
    }
}

@Composable
private fun PdfToolbar(
    currentPage: Int,
    pageCount: Int,
    zoom: Float,
    onSearch: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit
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

        Text(
            text =
                if (pageCount > 0) {
                    "${currentPage + 1} / $pageCount"
                } else {
                    "PDF"
                },

            style =
                MaterialTheme
                    .typography
                    .titleMedium
        )

        Row {

            IconButton(
                onClick =
                    onSearch
            ) {

                Icon(
                    imageVector =
                        Icons.Default.Search,

                    contentDescription =
                        "Search PDF"
                )
            }

            IconButton(
                onClick =
                    onZoomOut
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
                    "${(zoom * 100).toInt()}%",

                modifier =
                    Modifier.padding(
                        horizontal = 4.dp
                    )
            )

            IconButton(
                onClick =
                    onZoomIn
            ) {

                Icon(
                    imageVector =
                        Icons.Default.ZoomIn,

                    contentDescription =
                        "Zoom in"
                )
            }
        }
    }
}

@Composable
private fun PdfNavigationBar(
    currentPage: Int,
    pageCount: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(8.dp),

        horizontalArrangement =
            Arrangement.SpaceBetween,

        verticalAlignment =
            Alignment.CenterVertically
    ) {

        androidx.compose.material3.Button(
            onClick =
                onPrevious,

            enabled =
                currentPage > 0
        ) {

            Text(
                text = "Previous"
            )
        }

        Text(
            text =
                if (pageCount > 0) {
                    "Page ${currentPage + 1}"
                } else {
                    ""
                }
        )

        androidx.compose.material3.Button(
            onClick =
                onNext,

            enabled =
                currentPage <
                    pageCount - 1
        ) {

            Text(
                text = "Next"
            )
        }
    }
}