package com.coeric.universalreader

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpubReaderScreen(
    uri: Uri,
    onBack: () -> Unit
) {

    val context =
        androidx.compose.ui.platform.LocalContext.current

    val scope =
        rememberCoroutineScope()

    var document by remember {
        mutableStateOf<EpubDocument?>(null)
    }

    var error by remember {
        mutableStateOf<String?>(null)
    }

    var loading by remember {
        mutableStateOf(true)
    }

    var settings by remember {
        mutableStateOf(
            ReaderSettingsRepository.get(
                context,
                uri.toString()
            )
        )
    }

    var showSearch by remember {
        mutableStateOf(false)
    }

    var showSettings by remember {
        mutableStateOf(false)
    }

    var showBookmarks by remember {
        mutableStateOf(false)
    }

    var showChapters by remember {
        mutableStateOf(false)
    }

    var showAddBookmark by remember {
        mutableStateOf(false)
    }

    var searchQuery by remember {
        mutableStateOf("")
    }

    var selectedChapter by remember {
        mutableStateOf(0)
    }

    val listState =
        rememberLazyListState()

    LaunchedEffect(uri) {

        loading = true
        error = null

        try {

            val loaded =
                withContext(
                    Dispatchers.IO
                ) {
                    EpubReader.open(
                        context,
                        uri
                    )
                }

            document = loaded

            val saved =
                ReadingPositionRepository.get(
                    context,
                    uri.toString()
                )

            if (
                saved != null &&
                loaded.chapters.isNotEmpty()
            ) {

                selectedChapter =
                    saved.chapterIndex
                        .coerceIn(
                            0,
                            loaded.chapters.lastIndex
                        )
            }

        } catch (
            exception: Exception
        ) {

            error =
                exception.message
                    ?: "Unable to open EPUB."
        }

        loading = false
    }

    LaunchedEffect(
        selectedChapter,
        document
    ) {

        if (
            document != null &&
            document!!.chapters.isNotEmpty()
        ) {

            listState.animateScrollToItem(
                selectedChapter
                    .coerceIn(
                        0,
                        document!!.chapters.lastIndex
                    )
            )
        }
    }

    LaunchedEffect(
        listState.firstVisibleItemIndex
    ) {

        val current =
            listState.firstVisibleItemIndex

        if (
            document != null &&
            current >= 0 &&
            current < document!!.chapters.size
        ) {

            selectedChapter = current

            ReadingPositionRepository.save(
                context = context,
                documentUri = uri.toString(),
                position =
                    ReadingPosition(
                        documentUri =
                            uri.toString(),
                        chapterIndex =
                            current,
                        scrollIndex =
                            current,
                        scrollOffset =
                            listState
                                .firstVisibleItemScrollOffset
                    )
            )
        }
    }

    if (loading) {

        Box(
            modifier =
                Modifier.fillMaxSize(),
            contentAlignment =
                Alignment.Center
        ) {

            CircularProgressIndicator()
        }

        return
    }

    if (error != null) {

        Box(
            modifier =
                Modifier.fillMaxSize(),
            contentAlignment =
                Alignment.Center
        ) {

            Column(
                horizontalAlignment =
                    Alignment.CenterHorizontally,
                verticalArrangement =
                    Arrangement.spacedBy(12.dp),
                modifier =
                    Modifier.padding(24.dp)
            ) {

                Text(
                    text = "Unable to open EPUB",
                    style =
                        MaterialTheme
                            .typography
                            .titleLarge
                )

                Text(
                    text = error!!
                )

                Button(
                    onClick = onBack
                ) {
                    Text("Back")
                }
            }
        }

        return
    }

    val currentDocument =
        document
            ?: return

    UniversalReaderTheme(
        readerTheme = settings.theme
    ) {

        Scaffold(

            topBar = {

                TopAppBar(

                    title = {
                        Text(
                            currentDocument.title
                        )
                    },

                    navigationIcon = {

                        IconButton(
                            onClick = onBack
                        ) {

                            Icon(
                                imageVector =
                                    Icons.Default.Close,
                                contentDescription =
                                    "Back"
                            )
                        }
                    },

                    actions = {

                        IconButton(
                            onClick = {
                                showChapters = true
                            }
                        ) {

                            Icon(
                                imageVector =
                                    Icons.Default.Menu,
                                contentDescription =
                                    "Chapters"
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
                                    "Search"
                            )
                        }

                        IconButton(
                            onClick = {
                                showAddBookmark = true
                            }
                        ) {

                            Icon(
                                imageVector =
                                    Icons.Default.BookmarkAdd,
                                contentDescription =
                                    "Add bookmark"
                            )
                        }

                        IconButton(
                            onClick = {
                                showBookmarks = true
                            }
                        ) {

                            Icon(
                                imageVector =
                                    Icons.Default.Bookmarks,
                                contentDescription =
                                    "Bookmarks"
                            )
                        }

                        IconButton(
                            onClick = {
                                showSettings = true
                            }
                        ) {

                            Icon(
                                imageVector =
                                    Icons.Default.Settings,
                                contentDescription =
                                    "Settings"
                            )
                        }
                    }
                )
            }
        ) { padding ->

            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
            ) {

                ReadingProgress(
                    currentItem =
                        selectedChapter,
                    totalItems =
                        currentDocument
                            .chapters
                            .size
                )

                LazyColumn(
                    state = listState,
                    modifier =
                        Modifier.fillMaxSize(),
                    contentPadding =
                        PaddingValues(
                            top = 8.dp,
                            bottom = 48.dp
                        )
                ) {

                    items(
                        items =
                            currentDocument.chapters,
                        key = { chapter ->
                            chapter.title +
                                "_" +
                                currentDocument
                                    .chapters
                                    .indexOf(
                                        chapter
                                    )
                        }
                    ) { chapter ->

                        Column(
                            modifier =
                                Modifier.fillMaxWidth()
                        ) {

                            if (
                                chapter.title
                                    .isNotBlank()
                            ) {

                                Text(
                                    text =
                                        chapter.title,
                                    style =
                                        MaterialTheme
                                            .typography
                                            .headlineSmall,
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(
                                                horizontal = 16.dp,
                                                vertical = 16.dp
                                            )
                                )
                            }

                            EpubChapterContent(
                                chapter =
                                    chapter,
                                document =
                                    currentDocument,
                                fontSize =
                                    settings.fontSize,
                                lineSpacing =
                                    settings.lineSpacing,
                                textAlignment =
                                    settings.textAlignment
                            )
                        }
                    }
                }
            }
        }
    }

    if (showSearch) {

        EpubSearchDialog(
            document =
                currentDocument,
            query =
                searchQuery,
            onQueryChanged = {
                searchQuery = it
            },
            onDismiss = {
                showSearch = false
            },
            onResultSelected = { chapterIndex ->

                showSearch = false

                selectedChapter =
                    chapterIndex

                scope.launch {

                    listState.animateScrollToItem(
                        chapterIndex
                    )
                }
            }
        )
    }

    if (showChapters) {

        EpubChapterDialog(
            document =
                currentDocument,
            currentChapter =
                selectedChapter,
            onDismiss = {
                showChapters = false
            },
            onChapterSelected = { index ->

                showChapters = false

                selectedChapter =
                    index

                scope.launch {

                    listState.animateScrollToItem(
                        index
                    )
                }
            }
        )
    }

    if (showBookmarks) {

        EpubBookmarkDialog(
            context = context,
            documentUri =
                uri.toString(),
            onDismiss = {
                showBookmarks = false
            },
            onBookmarkSelected = { index ->

                showBookmarks = false

                selectedChapter =
                    index

                scope.launch {

                    listState.animateScrollToItem(
                        index
                    )
                }
            }
        )
    }

    if (showSettings) {

        AlertDialog(

            onDismissRequest = {
                showSettings = false
            },

            title = {
                Text("Reader settings")
            },

            text = {

                ReaderSettingsPanel(
                    settings = settings,
                    onSettingsChanged = {

                        settings = it

                        ReaderSettingsRepository.save(
                            context,
                            uri.toString(),
                            it
                        )
                    }
                )
            },

            confirmButton = {

                TextButton(
                    onClick = {
                        showSettings = false
                    }
                ) {
                    Text("Done")
                }
            }
        )
    }

    if (showAddBookmark) {

        AddEpubBookmarkDialog(
            currentChapter =
                selectedChapter,
            document =
                currentDocument,
            onDismiss = {
                showAddBookmark = false
            },
            onSave = { title, note ->

                BookmarkRepository.add(
                    context,
                    Bookmark(
                        id =
                            UUID
                                .randomUUID()
                                .toString(),
                        documentUri =
                            uri.toString(),
                        chapterIndex =
                            selectedChapter,
                        title =
                            title,
                        note =
                            note
                    )
                )

                showAddBookmark = false
            }
        )
    }
}

@Composable
private fun EpubSearchDialog(
    document: EpubDocument,
    query: String,
    onQueryChanged:
        (String) -> Unit,
    onDismiss: () -> Unit,
    onResultSelected:
        (Int) -> Unit
) {

    val results =
        remember(
            document,
            query
        ) {

            DocumentSearch.search(
                document,
                query
            )
        }

    AlertDialog(

        onDismissRequest = onDismiss,

        title = {
            Text("Search book")
        },

        text = {

            Column(
                verticalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {

                OutlinedTextField(
                    value = query,
                    onValueChange =
                        onQueryChanged,
                    modifier =
                        Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = {
                        Text("Search")
                    }
                )

                LazyColumn(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                top = 8.dp
                            )
                ) {

                    items(
                        results
                    ) { result ->

                        Card(
                            onClick = {
                                onResultSelected(
                                    result.chapterIndex
                                )
                            },
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        vertical = 4.dp
                                    )
                        ) {

                            Column(
                                modifier =
                                    Modifier.padding(
                                        12.dp
                                    )
                            ) {

                                Text(
                                    text =
                                        result.chapterTitle,
                                    style =
                                        MaterialTheme
                                            .typography
                                            .titleMedium
                                )

                                Text(
                                    text =
                                        result.matchingText,
                                    style =
                                        MaterialTheme
                                            .typography
                                            .bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        },

        confirmButton = {

            TextButton(
                onClick = onDismiss
            ) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun EpubChapterDialog(
    document: EpubDocument,
    currentChapter: Int,
    onDismiss: () -> Unit,
    onChapterSelected:
        (Int) -> Unit
) {

    AlertDialog(

        onDismissRequest = onDismiss,

        title = {
            Text("Chapters")
        },

        text = {

            LazyColumn(
                modifier =
                    Modifier.fillMaxWidth()
            ) {

                items(
                    items =
                        document.chapters
                            .mapIndexed {
                                index,
                                chapter ->
                                index to chapter
                            }
                ) { item ->

                    val index =
                        item.first

                    val chapter =
                        item.second

                    TextButton(
                        onClick = {
                            onChapterSelected(
                                index
                            )
                        },
                        modifier =
                            Modifier.fillMaxWidth()
                    ) {

                        Text(
                            text =
                                if (
                                    index ==
                                    currentChapter
                                ) {
                                    "▶ ${chapter.title}"
                                } else {
                                    chapter.title
                                }
                        )
                    }
                }
            }
        },

        confirmButton = {

            TextButton(
                onClick = onDismiss
            ) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun EpubBookmarkDialog(
    context: android.content.Context,
    documentUri: String,
    onDismiss: () -> Unit,
    onBookmarkSelected:
        (Int) -> Unit
) {

    val bookmarks =
        remember(
            documentUri
        ) {

            BookmarkRepository
                .getForDocument(
                    context,
                    documentUri
                )
        }

    AlertDialog(

        onDismissRequest = onDismiss,

        title = {
            Text("Bookmarks")
        },

        text = {

            if (bookmarks.isEmpty()) {

                Text(
                    "No bookmarks yet."
                )

            } else {

                LazyColumn(
                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    items(
                        items = bookmarks,
                        key = {
                            it.id
                        }
                    ) { bookmark ->

                        TextButton(
                            onClick = {
                                onBookmarkSelected(
                                    bookmark.chapterIndex
                                )
                            },
                            modifier =
                                Modifier.fillMaxWidth()
                        ) {

                            Column(
                                modifier =
                                    Modifier.fillMaxWidth()
                            ) {

                                Text(
                                    bookmark.title
                                )

                                if (
                                    bookmark.note
                                        .isNotBlank()
                                ) {

                                    Text(
                                        bookmark.note,
                                        style =
                                            MaterialTheme
                                                .typography
                                                .bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },

        confirmButton = {

            TextButton(
                onClick = onDismiss
            ) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun AddEpubBookmarkDialog(
    currentChapter: Int,
    document: EpubDocument,
    onDismiss: () -> Unit,
    onSave:
        (String, String) -> Unit
) {

    var title by remember {

        mutableStateOf(
            document.chapters
                .getOrNull(currentChapter)
                ?.title
                ?.ifBlank {
                    "Bookmark"
                }
                ?: "Bookmark"
        )
    }

    var note by remember {
        mutableStateOf("")
    }

    AlertDialog(

        onDismissRequest = onDismiss,

        title = {
            Text("Add bookmark")
        },

        text = {

            Column(
                verticalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {

                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                    },
                    label = {
                        Text("Title")
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = {
                        note = it
                    },
                    label = {
                        Text("Note")
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                )
            }
        },

        dismissButton = {

            TextButton(
                onClick = onDismiss
            ) {
                Text("Cancel")
            }
        },

        confirmButton = {

            Button(
                onClick = {
                    onSave(
                        title.ifBlank {
                            "Bookmark"
                        },
                        note
                    )
                }
            ) {
                Text("Save")
            }
        }
    )
}