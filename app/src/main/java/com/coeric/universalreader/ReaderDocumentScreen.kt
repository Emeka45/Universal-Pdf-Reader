package com.coeric.universalreader

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderDocumentScreen(
    document: ReaderDocument,
    documentUri: String
) {
    val context =
        androidx.compose.ui.platform.LocalContext.current

    val backDispatcher =
        LocalOnBackPressedDispatcherOwner.current
            ?.onBackPressedDispatcher

    var showChapterList by remember {
        mutableStateOf(false)
    }

    var showSettings by remember {
        mutableStateOf(false)
    }

    var showBookmarks by remember {
        mutableStateOf(false)
    }

    var showHighlights by remember {
        mutableStateOf(false)
    }

    var showAddBookmark by remember {
        mutableStateOf(false)
    }

    var showSearch by remember {
        mutableStateOf(false)
    }

    var selectedChapter by remember {
        mutableStateOf(0)
    }

    var settings by remember {
        mutableStateOf(
            ReaderSettingsRepository.get(
                context
            )
        )
    }

    var bookmarks by remember(documentUri) {
        mutableStateOf(
            BookmarkRepository.getBookmarks(
                context,
                documentUri
            )
        )
    }

    var highlights by remember(documentUri) {
        mutableStateOf(
            HighlightRepository.getHighlights(
                context,
                documentUri
            )
        )
    }

    val savedPosition =
        remember(documentUri) {
            ReadingPositionRepository.get(
                context,
                documentUri
            )
        }

    LaunchedEffect(
        savedPosition
    ) {
        if (
            savedPosition != null
        ) {
            selectedChapter =
                savedPosition.chapterIndex
                    .coerceIn(
                        0,
                        (
                            document.chapters.size - 1
                        ).coerceAtLeast(0)
                    )
        }
    }

    BackHandler {

        when {

            showSearch -> {
                showSearch = false
            }

            showAddBookmark -> {
                showAddBookmark = false
            }

            showSettings -> {
                showSettings = false
            }

            showChapterList -> {
                showChapterList = false
            }

            showBookmarks -> {
                showBookmarks = false
            }

            showHighlights -> {
                showHighlights = false
            }

            else -> {
                backDispatcher
                    ?.onBackPressed()
            }
        }
    }

    if (
        showAddBookmark
    ) {
        AddBookmarkDialog(
            document =
                document,

            documentUri =
                documentUri,

            chapterIndex =
                selectedChapter,

            onDismiss = {
                showAddBookmark =
                    false
            },

            onAdded = {

                bookmarks =
                    BookmarkRepository
                        .getBookmarks(
                            context,
                            documentUri
                        )

                showAddBookmark =
                    false
            }
        )
    }

    Scaffold(

        topBar = {

            CenterAlignedTopAppBar(

                title = {

                    Text(
                        text =
                            document.title,

                        maxLines = 1
                    )
                },

                navigationIcon = {

                    IconButton(
                        onClick = {

                            when {

                                showSearch -> {
                                    showSearch = false
                                }

                                showAddBookmark -> {
                                    showAddBookmark = false
                                }

                                showSettings -> {
                                    showSettings = false
                                }

                                showChapterList -> {
                                    showChapterList = false
                                }

                                showBookmarks -> {
                                    showBookmarks = false
                                }

                                showHighlights -> {
                                    showHighlights = false
                                }

                                else -> {
                                    backDispatcher
                                        ?.onBackPressed()
                                }
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

                            showSearch =
                                !showSearch

                            showAddBookmark = false
                            showSettings = false
                            showChapterList = false
                            showBookmarks = false
                            showHighlights = false
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

                            showSearch = false
                            showSettings = false
                            showChapterList = false
                            showBookmarks = false
                            showHighlights = false
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

                            showBookmarks =
                                !showBookmarks

                            showSearch = false
                            showSettings = false
                            showChapterList = false
                            showHighlights = false
                        }
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.Star,

                            contentDescription =
                                "Bookmarks"
                        )
                    }

                    IconButton(
                        onClick = {

                            showHighlights =
                                !showHighlights

                            showSearch = false
                            showSettings = false
                            showChapterList = false
                            showBookmarks = false
                        }
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.Star,

                            contentDescription =
                                "Highlights"
                        )
                    }

                    IconButton(
                        onClick = {

                            showSettings =
                                !showSettings

                            showSearch = false
                            showChapterList = false
                            showBookmarks = false
                            showHighlights = false
                        }
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.Settings,

                            contentDescription =
                                "Reader settings"
                        )
                    }

                    IconButton(
                        onClick = {

                            showChapterList =
                                !showChapterList

                            showSearch = false
                            showSettings = false
                            showBookmarks = false
                            showHighlights = false
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

        when {

            showSearch -> {

                DocumentSearchPanel(
                    document =
                        document,

                    onChapterSelected = {
                        chapterIndex ->

                        selectedChapter =
                            chapterIndex

                        saveChapterPosition(
                            context =
                                context,

                            documentUri =
                                documentUri,

                            chapterIndex =
                                chapterIndex
                        )

                        showSearch =
                            false
                    },

                    modifier =
                        Modifier.padding(
                            paddingValues
                        )
                )
            }

            showSettings -> {

                ReaderSettingsPanel(
                    settings =
                        settings,

                    onSettingsChanged = {
                        newSettings ->

                        settings =
                            newSettings

                        ReaderSettingsRepository.save(
                            context,
                            newSettings
                        )
                    },

                    modifier =
                        Modifier.padding(
                            paddingValues
                        )
                )
            }

            showChapterList -> {

                ChapterList(
                    document =
                        document,

                    selectedChapter =
                        selectedChapter,

                    onChapterSelected = {
                        index ->

                        selectedChapter =
                            index

                        saveChapterPosition(
                            context =
                                context,

                            documentUri =
                                documentUri,

                            chapterIndex =
                                index
                        )

                        showChapterList =
                            false
                    },

                    modifier =
                        Modifier.padding(
                            paddingValues
                        )
                )
            }

            showBookmarks -> {

                BookmarkList(
                    bookmarks =
                        bookmarks,

                    onBookmarkSelected = {
                        bookmark ->

                        selectedChapter =
                            bookmark.chapterIndex

                        saveChapterPosition(
                            context =
                                context,

                            documentUri =
                                documentUri,

                            chapterIndex =
                                bookmark.chapterIndex
                        )

                        showBookmarks =
                            false
                    },

                    onDelete = {
                        bookmark ->

                        BookmarkRepository
                            .removeBookmark(
                                context,
                                bookmark.id
                            )

                        bookmarks =
                            BookmarkRepository
                                .getBookmarks(
                                    context,
                                    documentUri
                                )
                    },

                    modifier =
                        Modifier.padding(
                            paddingValues
                        )
                )
            }

            showHighlights -> {

                HighlightList(
                    highlights =
                        highlights,

                    onHighlightSelected = {
                        highlight ->

                        selectedChapter =
                            highlight.chapterIndex

                        saveChapterPosition(
                            context =
                                context,

                            documentUri =
                                documentUri,

                            chapterIndex =
                                highlight.chapterIndex
                        )

                        showHighlights =
                            false
                    },

                    onDelete = {
                        highlight ->

                        HighlightRepository
                            .removeHighlight(
                                context,
                                highlight.id
                            )

                        highlights =
                            HighlightRepository
                                .getHighlights(
                                    context,
                                    documentUri
                                )
                    },

                    modifier =
                        Modifier.padding(
                            paddingValues
                        )
                )
            }

            else -> {

                DocumentContent(
                    document =
                        document,

                    selectedChapter =
                        selectedChapter,

                    documentUri =
                        documentUri,

                    settings =
                        settings,

                    onBookmarkAdded = {

                        bookmarks =
                            BookmarkRepository
                                .getBookmarks(
                                    context,
                                    documentUri
                                )
                    },

                    onHighlightAdded = {

                        highlights =
                            HighlightRepository
                                .getHighlights(
                                    context,
                                    documentUri
                                )
                    },

                    modifier =
                        Modifier.padding(
                            paddingValues
                        )
                )
            }
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
            modifier.fillMaxSize()
    ) {

        item {

            Text(
                text =
                    "Chapters",

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
private fun BookmarkList(
    bookmarks: List<Bookmark>,
    onBookmarkSelected: (Bookmark) -> Unit,
    onDelete: (Bookmark) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier =
            modifier.fillMaxSize(),

        verticalArrangement =
            Arrangement.spacedBy(
                8.dp
            )
    ) {

        item {

            Text(
                text =
                    "Bookmarks",

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

        if (
            bookmarks.isEmpty()
        ) {

            item {

                Text(
                    text =
                        "No bookmarks yet.",

                    modifier =
                        Modifier.padding(
                            horizontal = 20.dp,
                            vertical = 12.dp
                        )
                )
            }
        }

        itemsIndexed(
            bookmarks
        ) { _, bookmark ->

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            onBookmarkSelected(
                                bookmark
                            )
                        }
                        .padding(
                            horizontal = 20.dp,
                            vertical = 8.dp
                        )
            ) {

                Text(
                    text =
                        bookmark.title,

                    style =
                        MaterialTheme
                            .typography
                            .titleMedium,

                    fontWeight =
                        FontWeight.SemiBold
                )

                Text(
                    text =
                        "Chapter ${bookmark.chapterIndex + 1}",

                    style =
                        MaterialTheme
                            .typography
                            .bodyMedium
                )

                OutlinedButton(
                    onClick = {
                        onDelete(
                            bookmark
                        )
                    },

                    modifier =
                        Modifier.padding(
                            top = 6.dp
                        )
                ) {

                    Text(
                        text =
                            "Delete"
                    )
                }
            }
        }
    }
}

@Composable
private fun HighlightList(
    highlights: List<Highlight>,
    onHighlightSelected: (Highlight) -> Unit,
    onDelete: (Highlight) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier =
            modifier.fillMaxSize(),

        verticalArrangement =
            Arrangement.spacedBy(
                8.dp
            )
    ) {

        item {

            Text(
                text =
                    "Highlights",

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

        if (
            highlights.isEmpty()
        ) {

            item {

                Text(
                    text =
                        "No highlights yet.",

                    modifier =
                        Modifier.padding(
                            horizontal = 20.dp,
                            vertical = 12.dp
                        )
                )
            }
        }

        itemsIndexed(
            highlights
        ) { _, highlight ->

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            onHighlightSelected(
                                highlight
                            )
                        }
                        .padding(
                            horizontal = 20.dp,
                            vertical = 8.dp
                        )
            ) {

                Text(
                    text =
                        "\"${highlight.selectedText}\"",

                    style =
                        MaterialTheme
                            .typography
                            .bodyLarge,

                    fontWeight =
                        FontWeight.Medium
                )

                Text(
                    text =
                        "Chapter ${highlight.chapterIndex + 1}",

                    style =
                        MaterialTheme
                            .typography
                            .bodyMedium,

                    modifier =
                        Modifier.padding(
                            top = 4.dp
                        )
                )

                if (
                    highlight.note.isNotBlank()
                ) {

                    Text(
                        text =
                            highlight.note,

                        style =
                            MaterialTheme
                                .typography
                                .bodyMedium,

                        modifier =
                            Modifier.padding(
                                top = 4.dp
                            )
                    )
                }

                OutlinedButton(
                    onClick = {
                        onDelete(
                            highlight
                        )
                    },

                    modifier =
                        Modifier.padding(
                            top = 6.dp
                        )
                ) {

                    Text(
                        text =
                            "Delete"
                    )
                }
            }
        }
    }
}

@Composable
private fun AddBookmarkDialog(
    document: ReaderDocument,
    documentUri: String,
    chapterIndex: Int,
    onDismiss: () -> Unit,
    onAdded: () -> Unit
) {
    val context =
        androidx.compose.ui.platform.LocalContext.current

    var title by remember {

        mutableStateOf(

            document.chapters
                .getOrNull(
                    chapterIndex
                )
                ?.title
                ?: "Bookmark"
        )
    }

    AlertDialog(

        onDismissRequest =
            onDismiss,

        title = {

            Text(
                text =
                    "Add Bookmark"
            )
        },

        text = {

            TextField(
                value =
                    title,

                onValueChange = {
                    title = it
                },

                label = {

                    Text(
                        text =
                            "Bookmark title"
                    )
                },

                modifier =
                    Modifier.fillMaxWidth()
            )
        },

        confirmButton = {

            Button(
                onClick = {

                    BookmarkRepository
                        .addBookmark(

                            context =
                                context,

                            documentUri =
                                documentUri,

                            chapterIndex =
                                chapterIndex,

                            title =
                                title.ifBlank {
                                    "Bookmark"
                                }
                        )

                    onAdded()
                }
            ) {

                Text(
                    text =
                        "Save"
                )
            }
        },

        dismissButton = {

            TextButton(
                onClick =
                    onDismiss
            ) {

                Text(
                    text =
                        "Cancel"
                )
            }
        }
    )
}

@Composable
private fun DocumentContent(
    document: ReaderDocument,
    selectedChapter: Int,
    documentUri: String,
    settings: ReaderSettings,
    onBookmarkAdded: () -> Unit,
    onHighlightAdded: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context =
        androidx.compose.ui.platform.LocalContext.current

    val listState =
        rememberLazyListState()

    val savedPosition =
        remember(documentUri) {

            ReadingPositionRepository.get(
                context,
                documentUri
            )
        }

    LaunchedEffect(
        savedPosition
    ) {

        if (
            savedPosition != null
        ) {

            listState.scrollToItem(

                savedPosition
                    .scrollIndex
                    .coerceAtLeast(
                        0
                    ),

                savedPosition
                    .scrollOffset
                    .coerceAtLeast(
                        0
                    )
            )
        }
    }

    LaunchedEffect(
        listState.firstVisibleItemIndex,
        listState.firstVisibleItemScrollOffset
    ) {

        ReadingPositionRepository.save(

            context,

            ReadingPosition(

                documentUri =
                    documentUri,

                chapterIndex =
                    selectedChapter,

                scrollIndex =
                    listState
                        .firstVisibleItemIndex,

                scrollOffset =
                    listState
                        .firstVisibleItemScrollOffset
            )
        )
    }

    val alignment =
        when (
            settings.textAlignment
        ) {

            ReaderTextAlignment.LEFT ->
                TextAlign.Start

            ReaderTextAlignment.JUSTIFY ->
                TextAlign.Justify
        }

    val textColor =
        when (
            settings.theme
        ) {

            ReaderTheme.LIGHT ->
                MaterialTheme
                    .colorScheme
                    .onBackground

            ReaderTheme.DARK ->
                MaterialTheme
                    .colorScheme
                    .onBackground

            ReaderTheme.SEPIA ->
                MaterialTheme
                    .colorScheme
                    .onBackground
        }

    val textStyle =
        TextStyle(

            fontSize =
                settings.fontSize.sp,

            lineHeight =
                (
                    settings.fontSize *
                        settings.lineSpacing
                    ).sp,

            textAlign =
                alignment,

            color =
                textColor
        )

    LazyColumn(

        state =
            listState,

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

                document.author?.let {
                    author ->

                    Text(
                        text =
                            author,

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
        ) { index, chapter ->

            Column(

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 22.dp
                        )
            ) {

                Row(

                    modifier =
                        Modifier.fillMaxWidth(),

                    horizontalArrangement =
                        Arrangement.SpaceBetween
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

                    IconButton(

                        onClick = {

                            BookmarkRepository
                                .addBookmark(

                                    context =
                                        context,

                                    documentUri =
                                        documentUri,

                                    chapterIndex =
                                        index,

                                    title =
                                        chapter.title
                                )

                            onBookmarkAdded()
                        }
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.Bookmark,

                            contentDescription =
                                "Bookmark chapter"
                        )
                    }
                }

                SelectableHighlightText(

                    text =
                        chapter.content,

                    documentUri =
                        documentUri,

                    chapterIndex =
                        index,

                    textStyle =
                        textStyle,

                    onHighlightAdded =
                        onHighlightAdded,

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

@Composable
private fun DocumentSearchPanel(
    document: ReaderDocument,
    onChapterSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
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

                document.chapters
                    .mapIndexedNotNull {
                        index,
                        chapter ->

                        val position =
                            chapter.content
                                .indexOf(
                                    query,
                                    ignoreCase = true
                                )

                        if (
                            position < 0
                        ) {
                            null
                        } else {

                            val start =
                                (
                                    position - 60
                                ).coerceAtLeast(0)

                            val end =
                                (
                                    position +
                                        query.length +
                                        100
                                ).coerceAtMost(
                                    chapter.content.length
                                )

                            SearchResult(
                                chapterIndex =
                                    index,

                                chapterTitle =
                                    chapter.title,

                                matchingText =
                                    chapter.content
                                        .substring(
                                            start,
                                            end
                                        )
                            )
                        }
                    }
            }
        }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(20.dp),

        verticalArrangement =
            Arrangement.spacedBy(
                12.dp
            )
    ) {

        TextField(

            value =
                query,

            onValueChange = {
                query = it
            },

            label = {
                Text(
                    text =
                        "Search this book"
                )
            },

            modifier =
                Modifier.fillMaxWidth()
        )

        LazyColumn(
            modifier =
                Modifier.fillMaxSize()
        ) {

            if (
                query.isNotBlank() &&
                results.isEmpty()
            ) {

                item {

                    Text(
                        text =
                            "No results found.",

                        modifier =
                            Modifier.padding(
                                vertical = 16.dp
                            )
                    )
                }
            }

            itemsIndexed(
                results
            ) { _, result ->

                Column(

                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable {

                                onChapterSelected(
                                    result.chapterIndex
                                )
                            }
                            .padding(
                                vertical = 12.dp
                            )
                ) {

                    Text(
                        text =
                            result.chapterTitle,

                        style =
                            MaterialTheme
                                .typography
                                .titleMedium,

                        fontWeight =
                            FontWeight.Bold
                    )

                    Text(
                        text =
                            result.matchingText,

                        style =
                            MaterialTheme
                                .typography
                                .bodyMedium,

                        modifier =
                            Modifier.padding(
                                top = 4.dp
                            )
                    )
                }
            }
        }
    }
}

private fun saveChapterPosition(
    context: android.content.Context,
    documentUri: String,
    chapterIndex: Int
) {
    ReadingPositionRepository.save(

        context,

        ReadingPosition(

            documentUri =
                documentUri,

            chapterIndex =
                chapterIndex,

            scrollIndex =
                chapterIndex + 1,

            scrollOffset =
                0
        )
    )
}