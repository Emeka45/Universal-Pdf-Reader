package com.coeric.universalreader

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class ReaderActivity : ComponentActivity() {

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        val uriString =
            intent.getStringExtra("uri")

        if (uriString.isNullOrBlank()) {
            finish()
            return
        }

        val uri =
            Uri.parse(uriString)

        setContent {
            ReaderContent(
                uri = uri
            )
        }
    }
}

@Composable
private fun ReaderContent(
    uri: Uri
) {

    val context =
        androidx.compose.ui.platform.LocalContext.current

    var format by remember {
        mutableStateOf<DocumentFormat?>(null)
    }

    var error by remember {
        mutableStateOf<String?>(null)
    }

    LaunchedEffect(uri) {

        try {

            format =
                DocumentFormatDetector.detect(
                    context,
                    uri
                )

        } catch (
            exception: Exception
        ) {

            error =
                exception.message
                    ?: "Unable to detect document format."
        }
    }

    when {

        error != null -> {

            ErrorScreen(
                message =
                    error
                        ?: "Unable to open document."
            )
        }

        format == null -> {

            LoadingScreen()
        }

        format == DocumentFormat.PDF -> {

            PdfReaderView(
                uri = uri
            )
        }

        format == DocumentFormat.EPUB -> {

            EpubReaderScreen(
                uri = uri
            )
        }

        format == DocumentFormat.CBZ -> {

            CbzReaderContent(
                uri = uri
            )
        }

        format == DocumentFormat.CBR -> {

            ErrorScreen(
                message =
                    "CBR/RAR reading is not implemented yet."
            )
        }

        else -> {

            RoutedDocumentContent(
                uri = uri
            )
        }
    }
}

@Composable
private fun RoutedDocumentContent(
    uri: Uri
) {

    val context =
        androidx.compose.ui.platform.LocalContext.current

    var document by remember {
        mutableStateOf<ReaderDocument?>(null)
    }

    var error by remember {
        mutableStateOf<String?>(null)
    }

    LaunchedEffect(uri) {

        try {

            document =
                ReaderFormatRouter.open(
                    context,
                    uri
                )

        } catch (
            exception: Exception
        ) {

            error =
                exception.message
                    ?: "Unable to open this document."
        }
    }

    when {

        error != null -> {

            ErrorScreen(
                message =
                    error
                        ?: "Unable to open document."
            )
        }

        document == null -> {

            LoadingScreen()
        }

        else -> {

            ReaderDocumentScreen(
                document = document!!
            )
        }
    }
}

@Composable
private fun CbzReaderContent(
    uri: Uri
) {

    val context =
        androidx.compose.ui.platform.LocalContext.current

    var archive by remember {
        mutableStateOf<ComicArchive?>(null)
    }

    var error by remember {
        mutableStateOf<String?>(null)
    }

    LaunchedEffect(uri) {

        try {

            archive =
                CbzReader.open(
                    context,
                    uri
                )

        } catch (
            exception: Exception
        ) {

            error =
                exception.message
                    ?: "Unable to open CBZ file."
        }
    }

    when {

        error != null -> {

            ErrorScreen(
                message =
                    error
                        ?: "Unable to open CBZ."
            )
        }

        archive == null -> {

            LoadingScreen()
        }

        else -> {

            ComicReaderScreen(
                pages =
                    archive!!.pages
            )
        }
    }
}

@Composable
private fun ReaderDocumentScreen(
    document: ReaderDocument
) {

    LazyColumn(
        modifier =
            Modifier.fillMaxSize()
    ) {

        item {

            Column(
                modifier =
                    Modifier.padding(
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
                        text = it,

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
                    Modifier.padding(
                        horizontal = 20.dp,
                        vertical = 12.dp
                    )
            ) {

                Text(
                    text =
                        chapter.title,

                    style =
                        MaterialTheme
                            .typography
                            .titleLarge
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
                            top = 8.dp
                        )
                )
            }
        }
    }
}

@Composable
private fun LoadingScreen() {

    Box(
        modifier =
            Modifier.fillMaxSize(),

        contentAlignment =
            Alignment.Center
    ) {

        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorScreen(
    message: String
) {

    Box(
        modifier =
            Modifier.fillMaxSize(),

        contentAlignment =
            Alignment.Center
    ) {

        Text(
            text = message,

            modifier =
                Modifier.padding(
                   24.dp
                )
        )
    }
}