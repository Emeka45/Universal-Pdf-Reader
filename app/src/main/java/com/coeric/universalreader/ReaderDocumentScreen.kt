package com.coeric.universalreader

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.runtime.LaunchedEffect
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

    val backDispatcher =
        LocalOnBackPressedDispatcherOwner.current
            ?.onBackPressedDispatcher

    var showChapterList by remember {
        mutableStateOf(false)
    }

    var selectedChapter by remember {
        mutableStateOf(0)
    }

    BackHandler {

        if (showChapterList) {

            showChapterList = false

        } else {

            backDispatcher?.onBackPressed()
        }
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
                        onClick = {

                            if (showChapterList) {

                                showChapterList = false

                            } else {

                                backDispatcher
                                    ?.onBackPressed()
                            }
                        }
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

                selectedChapter =
                    selectedChapter,

                onChapterSelected = { index ->

                    selectedChapter =
                        index

                    showChapterList =
                        false
                },

                modifier =
                    Modifier.padding(
                        paddingValues
                    )
            )

        } else {

            DocumentContent(
                document = document,

                selectedChapter =
                    selectedChapter,

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
    selectedChapter: Int,
    onChapterSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {

    LazyColumn(
        modifier =
            modifier.fillMaxSize(),

        verticalArrangement =
            Arrangement.spacedBy(
                2.dp
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

        itemsIndexed(
            document.chapters
        ) { index, chapter ->

            Text(
                text =
                    "${index + 1}. ${chapter.title}",

                style =
                    MaterialTheme
                        .typography
                        .bodyLarge,

                fontWeight =
                    if (
                        index ==
                        selectedChapter
                    ) {
                        FontWeight.Bold
                    } else {
                        FontWeight.Normal
                    },

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable {

                            onChapterSelected(
                                index
                            )
                        }
                        .padding(
                            horizontal = 20.dp,
                            vertical = 16.dp
                        )
            )
        }
    }
}

@Composable
private fun DocumentContent(
    document: ReaderDocument,
    selectedChapter: Int,
    modifier: Modifier = Modifier
) {

    val listState =
        rememberLazyListState()

    LaunchedEffect(
        selectedChapter
    ) {

        if (
            document.chapters.isNotEmpty()
        ) {

            listState.animateScrollToItem(
                selectedChapter + 1
            )
        }
    }

    LazyColumn(
        state = listState,

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
                    text =
                        document.title,

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

        itemsIndexed(
            document.chapters
        ) { _, chapter ->

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 22.dp
                        )
            ) {

                Text(
                    text =
                        chapter.title,

                    style =
                        MaterialTheme
                            .typography
                            .titleLarge,

                    fontWeight =
                        FontWeight.SemiBold
                )

                Text(
                    text =
                        chapter.content,

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