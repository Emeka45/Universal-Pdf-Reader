package com.coeric.universalreader

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun PdfReaderView(
    uri: Uri
) {

    val context =
        LocalContext.current

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

    fun resetZoom() {

        zoom = 1f
        offsetX = 0f
        offsetY = 0f
    }

    BackHandler {

        if (showSearch) {

            showSearch = false

        } else {

            (context as? android.app.Activity)
                ?.finish()
        }
    }

    /*
     * One LaunchedEffect owns the renderer.
     *
     * This is important because PdfRenderer must not be closed
     * while another coroutine is using it.
     */
    LaunchedEffect(uri) {

        var descriptor: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        var temporaryFile: File? = null
        var renderJob: Job? = null

        try {

            loading = true
            error = null

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

                    try {

                        context
                            .contentResolver
                            .openInputStream(uri)
                            ?.use { input ->

                                target
                                    .outputStream()
                                    .use { output ->

                                        input.copyTo(
                                            output,
                                            bufferSize = 64 * 1024
                                        )
                                    }
                            }
                            ?: throw IllegalArgumentException(
                                "Unable to open PDF."
                            )

                        target

                    } catch (
                        exception: Exception
                    ) {

                        target.delete()

                        throw exception
                    }
                }

            temporaryFile = file

            descriptor =
                ParcelFileDescriptor.open(
                    file,
                    ParcelFileDescriptor.MODE_READ_ONLY
                )

            renderer =
                PdfRenderer(
                    descriptor!!
                )

            val pdf =
                renderer!!

            pageCount =
                pdf.pageCount

            if (pageCount <= 0) {

                throw IllegalArgumentException(
                    "The PDF contains no readable pages."
                )
            }

            currentPage = 0
            resetZoom()
            loading = false

            /*
             * Watch currentPage using a simple polling loop.
             *
             * This avoids snapshotFlow entirely and gives us
             * explicit control over cancellation.
             */
            while (
                currentCoroutineContext()
                    .isActive
            ) {

                val requestedPage =
                    currentPage

                renderJob?.cancel()

                renderJob =
                    launch {

                        loading = true

                        try {

                            val renderedBitmap =
                                withContext(
                                    Dispatchers.IO
                                ) {

                                    val page =
                                        pdf.openPage(
                                            requestedPage
                                        )

                                    try {

                                        val width =
                                            (
                                                page.width * 2f
                                            ).toInt()

                                        val height =
                                            (
                                                page.height * 2f
                                            ).toInt()

                                        val result =
                                            Bitmap.createBitmap(
                                                width,
                                                height,
                                                Bitmap.Config.ARGB_8888
                                            )

                                        page.render(
                                            result,
                                            null,
                                            null,
                                            PdfRenderer
                                                .Page
                                                .RENDER_MODE_FOR_DISPLAY
                                        )

                                        result

                                    } finally {

                                        page.close()
                                    }
                                }

                            ensureActive()

                            /*
                             * Make sure the page hasn't changed
                             * while rendering was taking place.
                             */
                            if (
                                currentPage ==
                                requestedPage
                            ) {

                                bitmap?.recycle()

                                bitmap =
                                    renderedBitmap

                                resetZoom()
                            } else {

                                renderedBitmap.recycle()
                            }

                        } catch (
                            exception: Exception
                        ) {

                            if (
                                exception is
                                    kotlinx.coroutines
                                        .CancellationException
                            ) {
                                return@launch
                            }

                            error =
                                exception.message
                                    ?: "Unable to render PDF page."

                        } finally {

                            if (
                                currentPage ==
                                requestedPage
                            ) {
                                loading = false
                            }
                        }
                    }

                /*
                 * Wait until currentPage changes.
                 *
                 * This is deliberately simple and reliable.
                 */
                var lastPage =
                    requestedPage

                while (
                    currentPage ==
                    lastPage
                ) {

                    kotlinx.coroutines
                        .delay(40)
                }
            }

        } catch (
            exception: Exception
        ) {

            if (
                exception is
                    kotlinx.coroutines
                        .CancellationException
            ) {
                throw exception
            }

            bitmap?.recycle()
            bitmap = null

            error =
                exception.message
                    ?: "Unable to open PDF."

            loading = false

        } finally {

            renderJob?.cancel()

            /*
             * Wait for the render job to stop before closing
             * the renderer.
             */
            try {
                renderJob?.join()
            } catch (_: Exception) {
            }

            bitmap?.recycle()
            bitmap = null

            try {
                renderer?.close()
            } catch (_: Exception) {
            }

            renderer = null

            try {
                descriptor?.close()
            } catch (_: Exception) {
            }

            descriptor = null

            temporaryFile?.delete()
            temporaryFile = null
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

                onBack = {

                    (context as? android.app.Activity)
                        ?.finish()
                },

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
                        zoom <= 1f
                    ) {

                        resetZoom()
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
                                        currentPage
                                    ) {

                                        detectTransformGestures {
                                                _,
                                                pan,
                                                scale,
                                                _ ->

                                            val newZoom =
                                                (
                                                    zoom *
                                                        scale
                                                ).coerceIn(
                                                    1f,
                                                    4f
                                                )

                                            if (
                                                newZoom <= 1f
                                            ) {

                                                zoom = 1f

                                                offsetX = 0f
                                                offsetY = 0f

                                            } else {

                                                zoom =
                                                    newZoom

                                                offsetX +=
                                                    pan.x

                                                offsetY +=
                                                    pan.y
                                            }
                                        }
                                    },

                            contentAlignment =
                                Alignment.Center
                        ) {

                            Image(
                                bitmap =
                                    bitmap!!
                                        .asImageBitmap(),

                                contentDescription =
                                    "PDF page ${
                                        currentPage + 1
                                    }",

                                contentScale =
                                    ContentScale.Fit,

                                modifier =
                                    Modifier
                                        .fillMaxSize()
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

                        resetZoom()
                    }
                },

                onNext = {

                    if (
                        currentPage <
                        pageCount - 1
                    ) {

                        currentPage++

                        resetZoom()
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

                        if (
                            page in
                            0 until pageCount
                        ) {

                            currentPage =
                                page

                            resetZoom()
                        }

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
    onBack: () -> Unit,
    onSearch: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit
) {

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 4.dp,
                    vertical = 4.dp
                ),

        verticalAlignment =
            Alignment.CenterVertically
    ) {

        IconButton(
            onClick =
                onBack
        ) {

            Icon(
                imageVector =
                    Icons.Default.ArrowBack,

                contentDescription =
                    "Close PDF"
            )
        }

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
                    .titleMedium,

            modifier =
                Modifier.weight(1f)
        )

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
                    horizontal = 2.dp
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
                text =
                    "Previous"
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
                text =
                    "Next"
            )
        }
    }
}