package com.coeric.universalreader

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

class ReaderActivity : ComponentActivity() {

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        val uri =
            intent.getStringExtra("uri")
                ?.let {
                    Uri.parse(it)
                }
                ?: intent.data

        if (uri == null) {
            finish()
            return
        }

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
        LocalContext.current

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

            CbrReaderContent(
                uri = uri
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
        LocalContext.current

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
                document = document!!,
                documentUri = uri.toString()
            )
        }
    }
}

@Composable
private fun CbzReaderContent(
    uri: Uri
) {

    val context =
        LocalContext.current

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
private fun CbrReaderContent(
    uri: Uri
) {

    val context =
        LocalContext.current

    var archive by remember {
        mutableStateOf<ComicArchive?>(null)
    }

    var error by remember {
        mutableStateOf<String?>(null)
    }

    LaunchedEffect(uri) {

        try {

            archive =
                CbrReader.open(
                    context,
                    uri
                )

        } catch (
            exception: Exception
        ) {

            error =
                exception.message
                    ?: "Unable to open CBR file."
        }
    }

    when {

        error != null -> {

            ErrorScreen(
                message =
                    error
                        ?: "Unable to open CBR."
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
            text =
                message,

            modifier =
                Modifier.padding(
                    24.dp
                )
        )
    }
}