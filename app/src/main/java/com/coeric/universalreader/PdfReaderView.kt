package com.coeric.universalreader

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class PdfPage(
    val number: Int,
    val bitmap: Bitmap
)

@Composable
fun PdfReaderView(
    context:Context,
    uri: Uri,
    modifier: Modifier = Modifier
) {

    val pages by produceState<List<PdfPage>>(
        initialValue = emptyList(),
        key1 = uri
    ) {

        value = withContext(Dispatchers.IO) {

            val result = mutableListOf<PdfPage>()

            var descriptor: ParcelFileDescriptor? = null
            var renderer: PdfRenderer? = null

            try {

                descriptor = context.contentResolver
                    .openFileDescriptor(uri, "r")

                if (descriptor != null) {

                    renderer = PdfRenderer(descriptor)

                    for (pageIndex in 0 until renderer.pageCount) {

                        val page = renderer.openPage(pageIndex)

                        val width = page.width * 2
                        val height = page.height * 2

                        val bitmap = Bitmap.createBitmap(
                            width,
                            height,
                            Bitmap.Config.ARGB_8888
                        )

                        bitmap.eraseColor(Color.WHITE)

                        page.render(
                            bitmap,
                            null,
                            null,
                            PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                        )

                        page.close()

                        result.add(
                            PdfPage(
                                number = pageIndex + 1,
                                bitmap = bitmap
                            )
                        )
                    }
                }

            } catch (_: Exception) {

            } finally {

                renderer?.close()
                descriptor?.close()
            }

            result
        }
    }

    if (pages.isEmpty()) {

        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Text("Loading PDF...")
        }

        return
    }

    var currentPage by remember {
        mutableIntStateOf(0)
    }

    val listState = rememberLazyListState()

    LaunchedEffect(listState.firstVisibleItemIndex) {
        currentPage = listState.firstVisibleItemIndex
    }

    Column(
        modifier = modifier
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            IconButton(
                enabled = currentPage > 0,
                onClick = {

                    val previousPage =
                        (currentPage - 1).coerceAtLeast(0)

                    currentPage = previousPage
                }
            ) {

                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Previous page"
                )
            }

            Text(
                text = "Page ${currentPage + 1} of ${pages.size}"
            )

            IconButton(
                enabled = currentPage < pages.lastIndex,
                onClick = {

                    val nextPage =
                        (currentPage + 1)
                            .coerceAtMost(pages.lastIndex)

                    currentPage = nextPage
                }
            ) {

                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "Next page"
                )
            }
        }

        LaunchedEffect(currentPage) {

            listState.animateScrollToItem(
                currentPage
            )
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth()
        ) {

            itemsIndexed(
                items = pages,
                key = { _, page ->
                    page.number
                }
            ) { _, page ->

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {

                    Image(
                        bitmap = page.bitmap.asImageBitmap(),
                        contentDescription =
                            "PDF page ${page.number}",
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "Page ${page.number}",
                        modifier = Modifier.padding(
                            horizontal = 16.dp,
                            vertical = 6.dp
                        )
                    )
                }
            }
        }
    }
}