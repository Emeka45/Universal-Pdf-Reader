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
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun PdfReaderView(
    uri: Uri
) {
    val context = LocalContext.current

    var pageCount by remember {
        mutableIntStateOf(0)
    }

    var currentPage by remember {
        mutableIntStateOf(0)
    }

    var bitmap by remember {
        mutableStateOf<Bitmap?>(null)
    }

    var loading by remember {
        mutableStateOf(true)
    }

    var error by remember {
        mutableStateOf<String?>(null)
    }

    var scale by remember {
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

    fun resetZoom() {
        scale = 1f
        offsetX = 0f
        offsetY = 0f
    }

    BackHandler {
        // Let the Activity handle back navigation.
    }

    LaunchedEffect(uri) {

        var tempFile: File? = null
        var descriptor: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null

        try {

            tempFile =
                withContext(Dispatchers.IO) {

                    val file =
                        File(
                            context.cacheDir,
                            "pdf_${System.currentTimeMillis()}.pdf"
                        )

                    context.contentResolver
                        .openInputStream(uri)
                        ?.use { input ->

                            file.outputStream()
                                .use { output ->

                                    input.copyTo(
                                        output
                                    )
                                }
                        }
                        ?: throw IllegalStateException(
                            "Unable to open PDF file."
                        )

                    file
                }

            descriptor =
                withContext(Dispatchers.IO) {

                    ParcelFileDescriptor.open(
                        tempFile,
                        ParcelFileDescriptor.MODE_READ_ONLY
                    )
                }

            renderer =
                withContext(Dispatchers.IO) {

                    PdfRenderer(
                        descriptor!!
                    )
                }

            pageCount =
                renderer!!.pageCount

            if (pageCount <= 0) {
                throw IllegalStateException(
                    "The PDF contains no pages."
                )
            }

            currentPage =
                currentPage.coerceIn(
                    0,
                    pageCount - 1
                )

            var renderedPage = -1

            while (true) {

                val requestedPage =
                    currentPage.coerceIn(
                        0,
                        pageCount - 1
                    )

                if (
                    requestedPage !=
                    renderedPage
                ) {

                    loading = true

                    try {

                        val renderedBitmap =
                            withContext(
                                Dispatchers.IO
                            ) {

                                val pdfRenderer =
                                    renderer
                                        ?: throw IllegalStateException(
                                            "PDF renderer is closed."
                                        )

                                val page =
                                    pdfRenderer.openPage(
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

                        ensureActive()

                        if (
                            currentPage ==
                            requestedPage
                        ) {

                            bitmap =
                                renderedBitmap

                            renderedPage =
                                requestedPage

                            resetZoom()

                        } else {

                            renderedBitmap.recycle()
                        }

                    } catch (
                        exception: Exception
                    ) {

                        if (
                            exception is
                            kotlinx.coroutines.CancellationException
                        ) {
                            throw exception
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

                delay(40)
            }

        } catch (
            exception: Exception
        ) {

            if (
                exception is
                kotlinx.coroutines.CancellationException
            ) {
                throw exception
            }

            error =
                exception.message
                    ?: "Unable to open PDF."

            loading = false

        } finally {

            bitmap = null

            withContext(
                Dispatchers.IO
            ) {

                try {
                    renderer?.close()
                } catch (_: Exception) {
                }

                try {
                    descriptor?.close()
                } catch (_: Exception) {
                }

                try {
                    tempFile?.delete()
                } catch (_: Exception) {
                }
            }
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

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                horizontalArrangement =
                    Arrangement.SpaceBetween,
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Text(
                    text =
                        if (pageCount > 0)
                            "Page ${currentPage + 1} / $pageCount"
                        else
                            "PDF",
                    style =
                        MaterialTheme.typography.titleMedium
                )

                Row {

                    IconButton(
                        onClick = {
                            if (
                                currentPage > 0
                            ) {
                                currentPage--
                            }
                        },
                        enabled =
                            currentPage > 0
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.ArrowBack,
                            contentDescription =
                                "Previous page"
                        )
                    }

                    IconButton(
                        onClick = {
                            if (
                                currentPage <
                                pageCount - 1
                            ) {
                                currentPage++
                            }
                        },
                        enabled =
                            currentPage <
                            pageCount - 1
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.ArrowBack,
                            contentDescription =
                                "Next page",
                            modifier =
                                Modifier.graphicsLayer {
                                    rotationY = 180f
                                }
                        )
                    }

                    IconButton(
                        onClick = {
                            scale =
                                (scale + 0.25f)
                                    .coerceAtMost(4f)
                        }
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.ZoomIn,
                            contentDescription =
                                "Zoom in"
                        )
                    }

                    IconButton(
                        onClick = {
                            scale =
                                (scale - 0.25f)
                                    .coerceAtLeast(1f)

                            if (scale == 1f) {
                                offsetX = 0f
                                offsetY = 0f
                            }
                        }
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.ZoomOut,
                            contentDescription =
                                "Zoom out"
                        )
                    }

                    IconButton(
                        onClick = {
                            showSearch = true
                        }
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.Search,
                            contentDescription =
                                "Search PDF"
                        )
                    }
                }
            }

            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(
                            MaterialTheme.colorScheme.background
                        )
                        .pointerInput(scale) {

                            detectTransformGestures {
                                _,
                                pan,
                                zoom,
                                _ ->

                                scale =
                                    (
                                        scale * zoom
                                    ).coerceIn(
                                        1f,
                                        4f
                                    )

                                if (scale > 1f) {

                                    offsetX +=
                                        pan.x

                                    offsetY +=
                                        pan.y

                                } else {

                                    offsetX = 0f
                                    offsetY = 0f
                                }
                            }
                        },
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

                    bitmap != null -> {

                        Image(
                            bitmap =
                                bitmap!!.asImageBitmap(),
                            contentDescription =
                                "PDF page ${currentPage + 1}",
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {

                                        scaleX =
                                            scale

                                        scaleY =
                                            scale

                                        translationX =
                                            offsetX

                                        translationY =
                                            offsetY
                                    },
                            contentScale =
                                ContentScale.Fit
                        )
                    }

                    loading -> {

                        CircularProgressIndicator()
                    }
                }
            }
        }
    }

    if (showSearch) {

        Dialog(
            onDismissRequest = {
                showSearch = false
            }
        ) {

            Surface {

                Text(
                    text =
                        "PDF search will be connected here.",
                    modifier =
                        Modifier.padding(
                            24.dp
                        )
                )
            }
        }
    }
}