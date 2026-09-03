package com.coeric.universalreader

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun EpubReaderScreen(
    uri: Uri,
    onBack: () -> Unit = {}
) {

    val context =
        androidx.compose.ui.platform.LocalContext.current

    val scope =
        rememberCoroutineScope()

    var document by remember {
        mutableStateOf<EpubDocument?>(null)
    }

    var loading by remember {
        mutableStateOf(true)
    }

    var error by remember {
        mutableStateOf<String?>(null)
    }

    var settings by remember {
        mutableStateOf(
            ReaderSettingsRepository.get(
                context,
                uri.toString()
            )
        )
    }

    var selectedChapter by rememberSaveable {
        mutableStateOf(0)
    }

    var showChapters by remember {
        mutableStateOf(false)
    }

    var showSettings by remember {
        mutableStateOf(false)
    }

    var showBookmarks by remember {
        mutableStateOf(false)
    }

    var showSearch by remember {
        mutableStateOf(false)
    }

    var showAddBookmark by remember {
        mutableStateOf(false)
    }

    var bookmarkNote by remember {
        mutableStateOf("")
    }

    LaunchedEffect(uri) {

        loading = true
        error = null

        try {

            document =
                EpubReader.open(
                    context,
                    uri
                )

            val saved =
                ReadingPositionRepository.get(
                    context,
                    uri.toString()
                )

            if (
                saved != null &&
                document != null
            ) {

                selectedChapter =
                    saved.chapterIndex.coerceIn(
                        0,
                        document!!.chapters
                            .lastIndex
                            .coerceAtLeast(0)
                    )
            }

        } catch (exception: Exception) {

            document = null

            error =
                exception.message
                    ?: "Unable to open EPUB."

        } finally {
            loading = false
        }
    }

    when {

        loading -> {

            Box(
                modifier =
                    Modifier.fillMaxSize(),
                contentAlignment =
                    Alignment.Center
            ) {

                CircularProgressIndicator()
            }
        }

        error != null -> {

            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                horizontalAlignment =
                    Alignment.CenterHorizontally,
                verticalArrangement =
                    Arrangement.Center
            ) {

                Text(
                    text = "Unable to open EPUB.",
                    style =
                        MaterialTheme
                            .typography
                            .headlineSmall
                )

                Text(
                    text =
                        error
                            ?: "Unknown error.",
                    modifier =
                        Modifier.padding(
                            top = 12.dp
                        )
                )

                TextButton(
                    onClick = onBack
                ) {
                    Text("Back")
                }
            }
        }

        document != null -> {

            val epub =
                document!!

            ModalNavigationDrawer(
                drawerState =
                    androidx.compose.material3
                        .rememberDrawerState(
                            DrawerValue.Closed
                        ),
                drawerContent = {

                    ModalDrawerSheet {

                        Text(
                            text =
                                epub.title,
                            style =
                                MaterialTheme
                                    .typography
                                    .titleLarge,
                            modifier =
                                Modifier.padding(
                                    20.dp
                                )
                        )

                        Divider()

                        NavigationDrawerItem(
                            label = {
                                Text("Chapters")
                            },
                            selected =
                                showChapters,
                            onClick = {
                                showChapters = true
                                showSettings = false
                                showBookmarks = false
                            },
                            icon = {
                                Icon(
                                    imageVector =
                                        Icons.Default.Menu,
                                    contentDescription =
                                        "Chapters"
                                )
                            }
                        )

                        NavigationDrawerItem(
                            label = {
                                Text("Bookmarks")
                            },
                            selected =
                                showBookmarks,
                            onClick = {
                                showBookmarks = true
                                showChapters = false
                                showSettings = false
                            },
                            icon = {
                                Icon(
                                    imageVector =
                                        Icons.Default.Bookmarks,
                                    contentDescription =
                                        "Bookmarks"
                                )
                            }
                        )

                        NavigationDrawerItem(
                            label = {
                                Text("Settings")
                            },
                            selected =
                                showSettings,
                            onClick = {
                                showSettings = true
                                showChapters = false
                                showBookmarks = false
                            },
                            icon = {
                                Icon(
                                    imageVector =
                                        Icons.Default.Settings,
                                    contentDescription =
                                        "Settings"
                                )
                            }
                        )
                    }
                }
            ) {

                Scaffold(
                    topBar = {

                        TopAppBar(
                            title = {

                                Text(
                                    text =
                                        epub.chapters
                                            .getOrNull(
                                                selectedChapter
                                            )
                                            ?.title
                                            ?: epub.title
                                )
                            },

                            navigationIcon = {

                                IconButton(
                                    onClick = onBack
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
                                            Icons.Default.Bookmark,
                                        contentDescription =
                                            "Add bookmark"
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        showChapters = true
                                        showSettings = false
                                        showBookmarks = false
                                    }
                                ) {

                                    Icon(
                                        imageVector =
                                            Icons.Default.Menu,
                                        contentDescription =
                                            "Table of contents"
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        showSettings = true
                                        showChapters = false
                                        showBookmarks = false
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
                ) { paddingValues ->

                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(
                                    paddingValues
                                )
                    ) {

                        EpubChapterPager(
                            document = epub,
                            selectedChapter =
                                selectedChapter,
                            settings = settings,
                            uri = uri,
                            onChapterChanged = {
                                selectedChapter = it
                            }
                        )

                        if (showChapters) {

                            EpubChaptersPanel(
                                document = epub,
                                selectedChapter =
                                    selectedChapter,
                                onChapterSelected = {
                                    selectedChapter = it
                                    showChapters = false
                                },
                                onClose = {
                                    showChapters = false
                                }
                            )
                        }

                        if (showSettings) {

                            EpubSettingsPanel(
                                settings = settings,
                                onSettingsChanged = {
                                    settings = it

                                    ReaderSettingsRepository.save(
                                        context,
                                        uri.toString(),
                                        it
                                    )
                                },
                                onClose = {
                                    showSettings = false
                                }
                            )
                        }

                        if (showBookmarks) {

                            EpubBookmarksPanel(
                                uri = uri,
                                onChapterSelected = {
                                    selectedChapter = it
                                    showBookmarks = false
                                },
                                onClose = {
                                    showBookmarks = false
                                }
                            )
                        }
                    }
                }
            }

            if (showSearch) {

                EpubSearchDialog(
                    document = epub,
                    onResultSelected = {
                        selectedChapter = it
                        showSearch = false
                    },
                    onClose = {
                        showSearch = false
                    }
                )
            }

            if (showAddBookmark) {

                AlertDialog(
                    onDismissRequest = {
                        showAddBookmark = false
                    },

                    title = {
                        Text("Add bookmark")
                    },

                    text = {

                        androidx.compose.material3
                            .OutlinedTextField(
                                value = bookmarkNote,
                                onValueChange = {
                                    bookmarkNote = it
                                },
                                modifier =
                                    Modifier.fillMaxWidth(),
                                label = {
                                    Text(
                                        "Note (optional)"
                                    )
                                }
                            )
                    },

                    confirmButton = {

                        TextButton(
                            onClick = {

                                BookmarkRepository.add(
                                    context,
                                    Bookmark(
                                        id =
                                            System
                                                .currentTimeMillis()
                                                .toString(),
                                        documentUri =
                                            uri.toString(),
                                        chapterIndex =
                                            selectedChapter,
                                        title =
                                            epub.chapters
                                                .getOrNull(
                                                    selectedChapter
                                                )
                                                ?.title
                                                ?: "Chapter",
                                        note =
                                            bookmarkNote
                                    )
                                )

                                bookmarkNote = ""
                                showAddBookmark = false
                            }
                        ) {
                            Text("Save")
                        }
                    },

                    dismissButton = {

                        TextButton(
                            onClick = {
                                showAddBookmark = false
                            }
                        ) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun EpubChapterPager(
    document: EpubDocument,
    selectedChapter: Int,
    settings: ReaderSettings,
    uri: Uri,
    onChapterChanged: (Int) -> Unit
) {

    val safeChapter =
        selectedChapter.coerceIn(
            0,
            document.chapters.lastIndex
                .coerceAtLeast(0)
        )

    val listState =
        rememberLazyListState()

    LaunchedEffect(
        safeChapter,
        document.chapters.size
    ) {

        if (
            document.chapters.isNotEmpty()
        ) {

            listState.scrollToItem(
                safeChapter
            )
        }
    }

    LaunchedEffect(
        listState.firstVisibleItemIndex,
        listState.firstVisibleItemScrollOffset
    ) {

        if (
            document.chapters.isNotEmpty()
        ) {

            val current =
                listState
                    .firstVisibleItemIndex
                    .coerceIn(
                        0,
                        document.chapters.lastIndex
                    )

            onChapterChanged(current)

            ReadingPositionRepository.save(
                androidx.compose.ui.platform
                    .LocalContext.current,
                uri.toString(),
                ReadingPosition(
                    documentUri =
                        uri.toString(),
                    chapterIndex =
                        current,
                    scrollIndex =
                        listState
                            .firstVisibleItemIndex,
                    scrollOffset =
                        listState
                            .firstVisibleItemScrollOffset
                )
            )
        }
    }

    LazyColumn(
        state = listState,
        modifier =
            Modifier.fillMaxSize()
    ) {

        itemsIndexed(
            items = document.chapters,
            key = { index, chapter ->
                "${index}_${chapter.title}"
            }
        ) { _, chapter ->

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            vertical = 16.dp
                        )
            ) {

                Text(
                    text =
                        chapter.title,
                    style =
                        MaterialTheme
                            .typography
                            .headlineSmall,
                    modifier =
                        Modifier.padding(
                            horizontal = 16.dp,
                            vertical = 8.dp
                        )
                )

                EpubChapterContent(
                    chapter = chapter,
                    document = document,
                    fontSize =
                        settings.fontSize,
                    lineSpacing =
                        settings.lineSpacing
                )
            }
        }
    }
}

@Composable
private fun EpubChaptersPanel(
    document: EpubDocument,
    selectedChapter: Int,
    onChapterSelected: (Int) -> Unit,
    onClose: () -> Unit
) {

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp)
    ) {

        Row(
            modifier =
                Modifier.fillMaxWidth(),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Text(
                text = "Table of Contents",
                style =
                    MaterialTheme
                        .typography
                        .headlineSmall,
                modifier =
                    Modifier.weight(1f)
            )

            IconButton(
                onClick = onClose
            ) {

                Icon(
                    imageVector =
                        Icons.Default.Close,
                    contentDescription =
                        "Close"
                )
            }
        }

        Divider()

        LazyColumn {

            itemsIndexed(
                document.chapters
            ) { index, chapter ->

                NavigationDrawerItem(
                    label = {
                        Text(
                            chapter.title
                        )
                    },
                    selected =
                        index == selectedChapter,
                    onClick = {
                        onChapterSelected(
                            index
                        )
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun EpubSettingsPanel(
    settings: ReaderSettings,
    onSettingsChanged: (ReaderSettings) -> Unit,
    onClose: () -> Unit
) {

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp)
    ) {

        Row(
            modifier =
                Modifier.fillMaxWidth(),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Text(
                text = "Reader Settings",
                style =
                    MaterialTheme
                        .typography
                        .headlineSmall,
                modifier =
                    Modifier.weight(1f)
            )

            IconButton(
                onClick = onClose
            ) {

                Icon(
                    imageVector =
                        Icons.Default.Close,
                    contentDescription =
                        "Close"
                )
            }
        }

        Divider()

        ReaderSettingsPanel(
            settings = settings,
            onSettingsChanged =
                onSettingsChanged
        )
    }
}

@Composable
private fun EpubBookmarksPanel(
    uri: Uri,
    onChapterSelected: (Int) -> Unit,
    onClose: () -> Unit
) {

    val context =
        androidx.compose.ui.platform.LocalContext.current

    var bookmarks by remember {
        mutableStateOf(
            BookmarkRepository.getForDocument(
                context,
                uri.toString()
            )
        )
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp)
    ) {

        Row(
            modifier =
                Modifier.fillMaxWidth(),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Text(
                text = "Bookmarks",
                style =
                    MaterialTheme
                        .typography
                        .headlineSmall,
                modifier =
                    Modifier.weight(1f)
            )

            IconButton(
                onClick = onClose
            ) {

                Icon(
                    imageVector =
                        Icons.Default.Close,
                    contentDescription =
                        "Close"
                )
            }
        }

        Divider()

        if (bookmarks.isEmpty()) {

            Text(
                text = "No bookmarks yet.",
                modifier =
                    Modifier.padding(
                        top = 20.dp
                    )
            )

        } else {

            LazyColumn {

                items(
                    items = bookmarks,
                    key = {
                        it.id
                    }
                ) { bookmark ->

                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(
                                    vertical = 8.dp
                                ),
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        TextButton(
                            onClick = {
                                onChapterSelected(
                                    bookmark.chapterIndex
                                )
                            },
                            modifier =
                                Modifier.weight(1f)
                        ) {

                            Column {

                                Text(
                                    bookmark.title
                                )

                                if (
                                    bookmark.note
                                        .isNotBlank()
                                ) {

                                    Text(
                                        text =
                                            bookmark.note,
                                        style =
                                            MaterialTheme
                                                .typography
                                                .bodySmall
                                    )
                                }
                            }
                        }

                        TextButton(
                            onClick = {

                                BookmarkRepository.remove(
                                    context,
                                    bookmark.id
                                )

                                bookmarks =
                                    BookmarkRepository
                                        .getForDocument(
                                            context,
                                            uri.toString()
                                        )
                            }
                        ) {
                            Text("Delete")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EpubSearchDialog(
    document: EpubDocument,
    onResultSelected: (Int) -> Unit,
    onClose: () -> Unit
) {

    var query by remember {
        mutableStateOf("")
    }

    val results =
        remember(
            query,
            document
        ) {

            if (
                query.isBlank()
            ) {
                emptyList()
            } else {

                DocumentSearch.search(
                    document,
                    query
                )
            }
        }

    AlertDialog(
        onDismissRequest = onClose,

        title = {
            Text("Search book")
        },

        text = {

            Column {

                androidx.compose.material3
                    .OutlinedTextField(
                        value = query,
                        onValueChange = {
                            query = it
                        },
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
                                top = 12.dp
                            )
                ) {

                    items(
                        items = results
                    ) { result ->

                        TextButton(
                            onClick = {
                                onResultSelected(
                                    result.chapterIndex
                                )
                            }
                        ) {

                            Column(
                                modifier =
                                    Modifier.fillMaxWidth()
                            ) {

                                Text(
                                    text =
                                        result.chapterTitle
                                )

                                Text(
                                    text =
                                        result.matchingText,
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
        },

        confirmButton = {

            TextButton(
                onClick = onClose
            ) {
                Text("Close")
            }
        }
    )
}