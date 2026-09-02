package com.coeric.universalreader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderDocumentScreen(
    document: ReaderDocument
) {

    var showChapterList by remember {
        mutableStateOf(false)
    }

    Scaffold(
        topBar = {

            CenterAlignedTopAppBar(

                title = {
                    Text(
                        text = document.title,
                        maxLines = 1
                    )
                },

                navigationIcon = {

                    IconButton(
                        onClick = {}
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.ArrowBack,

                            contentDescription =
                                "Back"
                        )
                    }
                },

                actions = {

                    IconButton(
                        onClick = {
                            showChapterList =
                                !showChapterList
                        }
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.Menu,

                            contentDescription =
                                "Chapters"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->

        if (showChapterList) {

            ChapterList(
                document = document,
                modifier =
                    Modifier.padding(
                        paddingValues
                    )
            )

        } else {

            DocumentContent(
                document = document,
                modifier =
                    Modifier.padding(
                        paddingValues
                    )
            )
        }
    }
}

@Composable
private fun ChapterList(
    document: ReaderDocument,
    modifier: Modifier = Modifier
) {

    LazyColumn(
        modifier =
            modifier.fillMaxSize(),

        verticalArrangement =
            Arrangement.spacedBy(
                4.dp
            )
    ) {

        item {

            Text(
                text = "Chapters",

                style =
                    MaterialTheme
                        .typography
                        .headlineSmall,

                fontWeight =
                    FontWeight.Bold,

                modifier =
                    Modifier.padding(
                        horizontal = 20.dp,
                        vertical = 16.dp
                    )
            )
        }

        items(
            document.chapters
        ) { chapter ->

            Text(
                text = chapter.title,

                style =
                    MaterialTheme
                        .typography
                        .bodyLarge,

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 20.dp,
                            vertical = 14.dp
                        )
            )
        }
    }
}

@Composable
private fun DocumentContent(
    document: ReaderDocument,
    modifier: Modifier = Modifier
) {

    LazyColumn(
        modifier =
            modifier.fillMaxSize(),

        verticalArrangement =
            Arrangement.spacedBy(
                18.dp
            )
    ) {

        item {

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 22.dp,
                            vertical = 24.dp
                        )
            ) {

                Text(
                    text = document.title,

                    style =
                        MaterialTheme
                            .typography
                            .headlineMedium,

                    fontWeight =
                        FontWeight.Bold
                )

                document.author?.let { author ->

                    Text(
                        text = author,

                        style =
                            MaterialTheme
                                .typography
                                .bodyLarge,

                        modifier =
                            Modifier.padding(
                                top = 8.dp
                            )
                    )
                }
            }
        }

        items(
            document.chapters
        ) { chapter ->

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 22.dp
                        )
            ) {

                Text(
                    text = chapter.title,

                    style =
                        MaterialTheme
                            .typography
                            .titleLarge,

                    fontWeight =
                        FontWeight.SemiBold
                )

                Text(
                    text = chapter.content,

                    style =
                        MaterialTheme
                            .typography
                            .bodyLarge,

                    modifier =
                        Modifier.padding(
                            top = 10.dp,
                           bottom = 10.dp
                        )
                )
            }
        }
    }
}