package com.coeric.universalreader

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.TextIncrease
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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

                if (
                    documentUri != null
                ) {

                    ReaderScreen(
                        uri =
                            Uri.parse(
                                documentUri
                            ),
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

                    kotlinx.coroutines
                        .withContext(
                            kotlinx.coroutines.Dispatchers.IO
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

    if (
        result == null
    ) {

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

    if (
        document == null
    ) {

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

                Text(
                    text =
                        message,
                    modifier =
                        Modifier.padding(
                            top = 12.dp
                        )
                )
            }
        }

        return
    }

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
                }
            )
        }

    ) { paddingValues ->

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(
                        rememberScrollState()
                    )
                    .padding(
                        paddingValues
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
                        .headlineMedium
            )

            document.author?.let {

                Text(
                    text =
                        "By $it",
                    style =
                        MaterialTheme
                            .typography
                            .bodyLarge,
                    modifier =
                        Modifier.padding(
                            top = 6.dp,
                            bottom = 20.dp
                        )
                )
            }

            for (
                chapter in document.chapters
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
                            top = 20.dp,
                            bottom = 10.dp
                        )
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
                            bottom = 20.dp
                        )
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
        mutableFloatStateOf(
            18f
        )
    }

    var showFontControls by remember {
        mutableStateOf(
            false
        )
    }

    val documentText =
        produceState(
            initialValue =
                "Loading document...",
            key1 =
                uri
        ) {

            value = try {

                context.contentResolver
                    .openInputStream(
                        uri
                    )
                    ?.bufferedReader()
                    ?.use {
                        it.readText()
                    }
                    ?: "Unable to open this document."

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

            if (
                showFontControls
            ) {

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