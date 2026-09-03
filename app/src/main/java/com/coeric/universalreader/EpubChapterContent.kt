package com.coeric.universalreader

import android.text.Html
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun EpubChapterContent(
    chapter: EpubChapter,
    document: EpubDocument,
    fontSize: Float = 18f,
    lineSpacing: Float = 1.55f,
    textAlignment: ReaderTextAlignment =
        ReaderTextAlignment.LEFT
) {

    val blocks =
        remember(
            chapter.originalHtml
        ) {
            EpubContentParser.parse(
                chapter.originalHtml
            )
        }

    Column(
        modifier =
            Modifier.fillMaxWidth(),
        verticalArrangement =
            Arrangement.spacedBy(8.dp)
    ) {

        blocks.forEachIndexed { index, block ->

            when (block) {

                is EpubContentBlock.Text -> {

                    val text =
                        remember(
                            block.text
                        ) {

                            Html.fromHtml(
                                block.text,
                                Html.FROM_HTML_MODE_LEGACY
                            )
                                .toString()
                                .replace(
                                    Regex(
                                        "\\n{3,}"
                                    ),
                                    "\n\n"
                                )
                                .trim()
                        }

                    if (text.isNotBlank()) {

                        Text(
                            text = text,

                            style =
                                MaterialTheme
                                    .typography
                                    .bodyLarge
                                    .copy(
                                        fontSize =
                                            fontSize.sp,
                                        lineHeight =
                                            (
                                                fontSize *
                                                    lineSpacing
                                            ).sp
                                    ),

                            textAlign =
                                if (
                                    textAlignment ==
                                    ReaderTextAlignment.JUSTIFY
                                ) {
                                    TextAlign.Justify
                                } else {
                                    TextAlign.Start
                                },

                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        horizontal = 16.dp,
                                        vertical = 4.dp
                                    )
                        )
                    }
                }

                is EpubContentBlock.Image -> {

                    val image =
                        remember(
                            block.path,
                            document.images
                        ) {

                            EpubImageResolver
                                .findImage(
                                    document,
                                    block.path
                                )
                        }

                    val bitmap =
                        rememberEpubBitmap(
                            image
                        )

                    if (bitmap != null) {

                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        vertical = 8.dp
                                    ),
                            horizontalAlignment =
                                Alignment.CenterHorizontally
                        ) {

                            Image(
                                bitmap =
                                    bitmap
                                        .asImageBitmap(),

                                contentDescription =
                                    block.altText
                                        ?: "EPUB image",

                                contentScale =
                                    ContentScale.Fit,

                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .sizeIn(
                                            maxHeight =
                                                700.dp
                                        )
                                        .padding(
                                            horizontal = 8.dp
                                        )
                            )

                            if (
                                !block.altText
                                    .isNullOrBlank()
                            ) {

                                Text(
                                    text =
                                        block.altText!!,
                                    style =
                                        MaterialTheme
                                            .typography
                                            .bodySmall,
                                    modifier =
                                        Modifier.padding(
                                            start = 16.dp,
                                            top = 4.dp,
                                            end = 16.dp
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}