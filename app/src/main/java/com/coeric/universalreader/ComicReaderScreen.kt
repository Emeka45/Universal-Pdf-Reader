package com.coeric.universalreader

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp

@Composable
fun ComicReaderScreen(
    pages: List<ComicPage>
) {

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

    val decodedPages =
        remember(pages) {

            pages.mapNotNull { page ->

                val bitmap =
                    BitmapFactory.decodeByteArray(
                        page.data,
                        0,
                        page.data.size
                    )

                bitmap?.let {
                    page to it.asImageBitmap()
                }
            }
        }

    if (decodedPages.isEmpty()) {

        Box(
            modifier =
                Modifier.fillMaxSize(),
            contentAlignment =
                Alignment.Center
        ) {

            Text(
                text =
                    "Unable to decode the comic pages."
            )
        }

        return
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

        Text(
            text =
                "Page 1 / ${decodedPages.size}",

            style =
                MaterialTheme
                    .typography
                    .titleMedium,

            modifier =
                Modifier.padding(
                    16.dp
                )
        )

        LazyColumn(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f),

            horizontalAlignment =
                Alignment.CenterHorizontally,

            verticalArrangement =
                Arrangement.Top
        ) {

            itemsIndexed(
                decodedPages
            ) { _, item ->

                Image(
                    bitmap = item.second,
                    contentDescription =
                        item.first.name,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = 4.dp,
                                vertical = 2.dp
                            )
                )
            }
        }

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        8.dp
                    ),

            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Text(
                text =
                    "${decodedPages.size} pages",

                style =
                    MaterialTheme
                        .typography
                        .bodyMedium
            )

            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            top = 4.dp
                        )
            ) {

                Icon(
                    imageVector =
                        Icons.Default.ArrowBack,

                    contentDescription =
                        "Previous page",

                    modifier =
                        Modifier
                            .align(
                                Alignment.CenterStart
                            )
                            .size(1.dp)
                )

                IconButton(
                    onClick = {},
                    modifier =
                        Modifier
                            .align(
                                Alignment.Center
                            )
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.ArrowForward,

                        contentDescription =
                            "Next page"
                    )
                }
            }
        }
    }
}