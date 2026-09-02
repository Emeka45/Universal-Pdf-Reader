package com.coeric.universalreader

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

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
            ReaderContent(uri)
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
        } catch (exception: Exception) {
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

        format == DocumentFormat.CBZ -> {
            CbzReaderContent(uri)
        }

        format == DocumentFormat.CBR -> {
            ErrorScreen(
                message =
                    "CBR support is not enabled yet."
            )
        }

        else -> {
            ExistingFormatContent(
                uri = uri,
                format = format!!
            )
        }
    }
}

@Composable
private fun ExistingFormatContent(
    uri: Uri,
    format: DocumentFormat
) {
    when (format) {

        DocumentFormat.PDF -> {
            PdfReaderView(
                uri = uri
            )
        }

        DocumentFormat.EPUB -> {
            EpubReaderScreen(
                uri = uri
            )
        }

        DocumentFormat.MOBI,
        DocumentFormat.AZW,
        DocumentFormat.AZW3 -> {
            MobiReaderScreen(
                uri = uri
            )
        }

        DocumentFormat.FB2 -> {
            Fb2ReaderContent(
                uri = uri
            )
        }

        else -> {
            TextReaderScreen(
                uri = uri
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

    var pages by remember {
        mutableStateOf<List<ComicPage>?>(null)
    }

    var error by remember {
        mutableStateOf<String?>(null)
    }

    LaunchedEffect(uri) {
        try {
            pages =
                CbzReader.open(
                    context,
                    uri
                )
        } catch (exception: Exception) {
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

        pages == null -> {
            LoadingScreen()
        }

        else -> {
            ComicReaderScreen(
                pages = pages!!
            )
        }
    }
}

@Composable
private fun Fb2ReaderContent(
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
                Fb2Reader.open(
                    context,
                    uri
                )
        } catch (exception: Exception) {
            error =
                exception.message
                    ?: "Unable to open FB2 file."
        }
    }

    when {
        error != null -> {
            ErrorScreen(
                message =
                    error
                        ?: "Unable to open FB2."
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
private fun ReaderDocumentScreen(
    document: ReaderDocument
) {
    androidx.compose.foundation.lazy.LazyColumn(
        modifier =
            Modifier.fillMaxSize()
    ) {
        item {
            Text(
                text =
                    document.title,

                style =
                    androidx.compose.material3.MaterialTheme
                        .typography
                        .headlineMedium,

                modifier =
                    Modifier.padding(
                        20.dp
                    )
            )

            document.author?.let {
                Text(
                    text = it,

                    style =
                        androidx.compose.material3.MaterialTheme
                            .typography
                            .bodyLarge,

                    modifier =
                        Modifier.padding(
                            horizontal = 20.dp
                        )
                )
            }
        }

        items(
            document.chapters.size
        ) { index ->

            val chapter =
                document.chapters[index]

            Text(
                text =
                    chapter.title,

                style =
                    androidx.compose.material3.MaterialTheme
                        .typography
                        .titleLarge,

                modifier =
                    Modifier.padding(
                        horizontal = 20.dp,
                        vertical = 12.dp
                    )
            )

            Text(
                text =
                    chapter.content,

                style =
                    androidx.compose.material3.MaterialTheme
                        .typography
                        .bodyLarge,

                modifier =
                    Modifier.padding(
                        horizontal = 20.dp,
                        vertical = 8.dp
                    )
            )
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