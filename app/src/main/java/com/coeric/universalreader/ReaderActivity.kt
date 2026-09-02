@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.coeric.universalreader

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.TextIncrease
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ReaderActivity : ComponentActivity() {

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )

        val documentUri =
            intent.getStringExtra(
                "document_uri"
            )

        setContent {

            MaterialTheme {

                if (documentUri != null) {

                    ReaderScreen(
                        uri = Uri.parse(documentUri),
                        onBack = {
                            finish()
                        }
                    )

                } else {

                    Scaffold(

                        topBar = {

                            TopAppBar(

                                title = {
                                    Text(
                                        "Universal Reader"
                                    )
                                },

                                navigationIcon = {

                                    IconButton(
                                        onClick = {
                                            finish()
                                        }
                                    ) {

                                        Icon(
                                            imageVector =
                                                Icons.Default.ArrowBack,
                                            contentDescription =
                                                "Back"
                                        )
                                    }
                                }
                            )
                        }

                    ) { paddingValues ->

                        Text(
                            text =
                                "No document selected.",
                            modifier =
                                Modifier
                                    .padding(
                                        paddingValues
                                    )
                                    .padding(
                                        20.dp
                                    )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReaderScreen(
    uri: Uri,
    onBack: () -> Unit
) {

    val context =
        LocalContext.current

    val extension =
        remember(uri) {

            uri.toString()
                .substringBefore("?")
                .substringBefore("#")
                .substringAfterLast(
                    '.',
                    ""
                )
                .lowercase()
        }

    when (extension) {

        "pdf" -> {

            Scaffold(

                topBar = {

                    TopAppBar(

                        title = {
                            Text(
                                "PDF Reader"
                            )
                        },

                        navigationIcon = {

                            IconButton(
                                onClick =
                                    onBack
                            ) {

                                Icon(
                                    imageVector =
                                        Icons.Default.ArrowBack,
                                    contentDescription =
                                        "Back"
                                )
                            }
                        }
                    )
                }

            ) { paddingValues ->

                PdfReaderView(
                    context =
                        context,
                    uri =
                        uri,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(
                                paddingValues
                            )
                )
            }
        }

        "epub" -> {

            EpubReaderScreen(
                context =
                    context,
                uri =
                    uri,
                onBack =
                    onBack
            )
        }

        else -> {

            TextReaderScreen(
                uri =
                    uri,
                onBack =
                    onBack
            )
        }
    }
}

@Composable
private fun EpubReaderScreen(
    context: android.content.Context,
    uri: Uri,
    onBack: () -> Unit
) {

    val epubState =
        produceState<Result<EpubDocument>?>(null, uri) {

            value =
                runCatching {

                    withContext(
                        Dispatchers.IO
                    ) {

                        EpubReader.open(
                            context,
                            uri
                        )
                    }
                }
        }

    val result =
        epubState.value

    if (result == null) {

        Scaffold(

            topBar = {

                TopAppBar(

                    title = {
                        Text(
                            "EPUB Reader"
                        )
                    },

                    navigationIcon = {

                        IconButton(
                            onClick =
                                onBack
                        ) {

                            Icon(
                                imageVector =
                                    Icons.Default.ArrowBack,
                                contentDescription =
                                    "Back"
                            )
                        }
                    }
                )
            }

        ) { paddingValues ->

            Text(
                text =
                    "Loading EPUB...",
                modifier =
                    Modifier
                        .padding(
                            paddingValues
                        )
                        .padding(
                            20.dp
                        )
            )
        }

        return
    }

    val document =
        result.getOrNull()

    if (document == null) {

        val message =
            result
                .exceptionOrNull()
                ?.message
                ?: "Unable to read this EPUB."

        Scaffold(

            topBar = {

                TopAppBar(

                    title = {
                        Text(
                            "EPUB Reader"
                        )
                    },

                    navigationIcon = {

                        IconButton(
                            onClick =
                                onBack
                        ) {

                            Icon(
                                imageVector =
                                    Icons.Default.ArrowBack,
                                contentDescription =
                                    "Back"
                            )
                        }
                    }
                )
            }

        ) { paddingValues ->

            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(
                            paddingValues
                        )
                        .padding(
                            20.dp
                        )
            ) {

                Text(
                    text =
                        "Unable to open EPUB",
                    style =
                        MaterialTheme
                            .typography
                            .headlineSmall
                )

                Spacer(
                    modifier =
                        Modifier.height(
                            12.dp
                        )
                )

                Text(
                    text =
                        message
                )
            }
        }

        return
    }

    var currentChapter by remember(uri) {

        mutableIntStateOf(

            context
                .getSharedPreferences(
                    "reader_progress",
                    android.content.Context.MODE_PRIVATE
                )
                .getInt(
                    "epub_$uri",
                    0
                )
                .coerceIn(
                    0,
                    document.chapters.lastIndex
                )
        )
    }

    var showTableOfContents by remember {
        mutableStateOf(false)
    }

    var fontSize by remember {
        mutableFloatStateOf(18f)
    }

    var showFontControls by remember {
        mutableStateOf(false)
    }

    val preferences =
        remember(uri) {

            context.getSharedPreferences(
                "reader_progress",
                android.content.Context.MODE_PRIVATE
            )
        }

    val chapter =
        document.chapters[currentChapter]

    Scaffold(

        topBar = {

            TopAppBar(

                title = {

                    Text(
                        document.title
                    )
                },

                navigationIcon = {

                    IconButton(
                        onClick =
                            onBack
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

                            showTableOfContents =
                                !showTableOfContents
                        }
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.MenuBook,
                            contentDescription =
                                "Table of contents"
                        )
                    }

                    IconButton(
                        onClick = {

                            showFontControls =
                                !showFontControls
                        }
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.TextIncrease,
                            contentDescription =
                                "Font size"
                        )
                    }
                }
            )
        }

    ) { paddingValues ->

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(
                        paddingValues
                    )
        ) {

            if (showFontControls) {

                Column(
                    modifier =
                        Modifier.padding(
                            horizontal = 20.dp,
                            vertical = 8.dp
                        )
                ) {

                    Text(
                        text =
                            "Text Size",
                        style =
                            MaterialTheme
                                .typography
                                .labelLarge
                    )

                    Slider(
                        value =
                            fontSize,
                        onValueChange = {
                            fontSize = it
                        },
                        valueRange =
                            12f..32f
                    )
                }
            }

            if (showTableOfContents) {

                TableOfContents(

                    document =
                        document,

                    currentChapter =
                        currentChapter,

                    onChapterSelected = { index ->

                        currentChapter =
                            index

                        preferences
                            .edit()
                            .putInt(
                                "epub_$uri",
                                index
                            )
                            .apply()

                        showTableOfContents =
                            false
                    }
                )

            } else {

                ChapterContent(

                    document =
                        document,

                    chapter =
                        chapter,

                    chapterIndex =
                        currentChapter,

                    fontSize =
                        fontSize,

                    onPrevious = {

                        if (
                            currentChapter > 0
                        ) {

                            currentChapter--

                            preferences
                                .edit()
                                .putInt(
                                    "epub_$uri",
                                    currentChapter
                                )
                                .apply()
                        }
                    },

                    onNext = {

                        if (
                            currentChapter <
                            document.chapters.lastIndex
                        ) {

                            currentChapter++

                            preferences
                                .edit()
                                .putInt(
                                    "epub_$uri",
                                    currentChapter
                                )
                                .apply()
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun TableOfContents(
    document: EpubDocument,
    currentChapter: Int,
    onChapterSelected: (Int) -> Unit
) {

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(
                    20.dp
                )
    ) {

        Text(
            text =
                "Table of Contents",
            style =
                MaterialTheme
                    .typography
                    .headlineSmall
        )

        Spacer(
            modifier =
                Modifier.height(
                    12.dp
                )
        )

        for (
            index in document.chapters.indices
        ) {

            Card(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            vertical = 4.dp
                        ),
                onClick = {
                    onChapterSelected(
                        index
                    )
                }
            ) {

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                16.dp
                            ),
                    horizontalArrangement =
                        Arrangement.SpaceBetween
                ) {

                    Text(
                        text =
                            "${index + 1}. ${document.chapters[index].title}",
                        style =
                            if (
                                index ==
                                currentChapter
                            ) {
                                MaterialTheme
                                    .typography
                                    .titleMedium
                            } else {
                                MaterialTheme
                                    .typography
                                    .bodyLarge
                            }
                    )
                }
            }
        }
    }
}

@Composable
private fun ChapterContent(
    document: EpubDocument,
    chapter: EpubChapter,
    chapterIndex: Int,
    fontSize: Float,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {

    Column(
        modifier =
            Modifier
                .fillMaxSize()
    ) {

        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .verticalScroll(
                        rememberScrollState()
                    )
                    .padding(
                        20.dp
                    )
        ) {

            Text(
                text =
                    document.title,
                style =
                    MaterialTheme
                        .typography
                        .titleMedium
            )

            document.author?.let {

                Text(
                    text =
                        "By $it",
                    style =
                        MaterialTheme
                            .typography
                            .bodyMedium,
                    modifier =
                        Modifier.padding(
                            top = 4.dp,
                            bottom = 20.dp
                        )
                )
            }

            Text(
                text =
                    chapter.title,
                style =
                    MaterialTheme
                        .typography
                        .headlineMedium,
                modifier =
                    Modifier.padding(
                        bottom = 16.dp
                    )
            )

            Text(
                text =
                    chapter.content,
                fontSize =
                    fontSize.sp,
                lineHeight =
                    (fontSize * 1.55f).sp
            )

            Spacer(
                modifier =
                    Modifier.height(
                        24.dp
                    )
            )
        }

        Divider()

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        12.dp
                    ),
            horizontalArrangement =
                Arrangement.SpaceBetween
        ) {

            Button(
                enabled =
                    chapterIndex > 0,
                onClick =
                    onPrevious
            ) {

                Icon(
                    imageVector =
                        Icons.Default.ArrowBack,
                    contentDescription =
                        "Previous chapter"
                )

                Spacer(
                    modifier =
                        Modifier.padding(
                            horizontal = 4.dp
                        )
                )

                Text(
                    "Previous"
                )
            }

            Text(
                text =
                    "${chapterIndex + 1} / ${document.chapters.size}",
                modifier =
                    Modifier.padding(
                        top = 12.dp
                    )
            )

            Button(
                enabled =
                    chapterIndex <
                    document.chapters.lastIndex,
                onClick =
                    onNext
            ) {

                Text(
                    "Next"
                )

                Spacer(
                    modifier =
                        Modifier.padding(
                            horizontal = 4.dp
                        )
                )

                Icon(
                    imageVector =
                        Icons.Default.ArrowForward,
                    contentDescription =
                        "Next chapter"
                )
            }
        }
    }
}

@Composable
private fun TextReaderScreen(
    uri: Uri,
    onBack: () -> Unit
) {

    val context =
        LocalContext.current

    var fontSize by remember {
        mutableFloatStateOf(18f)
    }

    var showFontControls by remember {
        mutableStateOf(false)
    }

    val documentText =
        produceState(
            initialValue =
                "Loading document...",
            key1 =
                uri
        ) {

            value = try {

                withContext(
                    Dispatchers.IO
                ) {

                    context.contentResolver
                        .openInputStream(
                            uri
                        )
                        ?.bufferedReader()
                        ?.use {
                            it.readText()
                        }
                        ?: "Unable to open this document."
                }

            } catch (
                exception: Exception
            ) {

                "Unable to read this document.\n\n${exception.message}"
            }
        }

    Scaffold(

        topBar = {

            TopAppBar(

                title = {
                    Text(
                        "Reader"
                    )
                },

                navigationIcon = {

                    IconButton(
                        onClick =
                            onBack
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

                            showFontControls =
                                !showFontControls
                        }
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.TextIncrease,
                            contentDescription =
                                "Font size"
                        )
                    }
                }
            )
        }

    ) { paddingValues ->

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(
                        paddingValues
                    )
        ) {

            if (showFontControls) {

                Column(
                    modifier =
                        Modifier.padding(
                            horizontal = 20.dp,
                            vertical = 8.dp
                        )
                ) {

                    Text(
                        text =
                            "Text Size",
                        style =
                            MaterialTheme
                                .typography
                                .labelLarge
                    )

                    Slider(
                        value =
                            fontSize,
                        onValueChange = {
                            fontSize = it
                        },
                        valueRange =
                            12f..32f
                    )
                }
            }

            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(
                            rememberScrollState()
                        )
                        .padding(
                            20.dp
                        )
            ) {

                Text(
                    text =
                        documentText.value,
                    fontSize =
                        fontSize.sp,
                    lineHeight =
                        (fontSize * 1.55f).sp
                )
            }
        }
    }
}