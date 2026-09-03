package com.coeric.universalreader

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MobiReaderScreen(
    uri: Uri,
    format: DocumentFormat,
    onBack: () -> Unit
) {

    val context =
        androidx.compose.ui.platform.LocalContext.current

    var document by remember(uri) {
        mutableStateOf<ReaderDocument?>(null)
    }

    var errorMessage by remember(uri) {
        mutableStateOf<String?>(null)
    }

    var loading by remember(uri) {
        mutableStateOf(true)
    }

    LaunchedEffect(uri) {

        loading = true
        errorMessage = null

        try {

            document =
                MobiReader.open(
                    context,
                    uri
                )

        } catch (exception: Exception) {

            errorMessage =
                exception.message
                    ?: "Unable to open this ebook."

        } finally {

            loading = false
        }
    }

    Scaffold(

        topBar = {

            TopAppBar(

                title = {

                    Text(
                        when (format) {

                            DocumentFormat.MOBI ->
                                "MOBI Reader"

                            DocumentFormat.AZW ->
                                "AZW Reader"

                            DocumentFormat.AZW3 ->
                                "AZW3 Reader"

                            else ->
                                "Ebook Reader"
                        }
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

        when {

            loading -> {

                Column(

                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(
                                paddingValues
                            )
                            .padding(
                                24.dp
                            ),

                    verticalArrangement =
                        Arrangement.Center,

                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {

                    Text(
                        text =
                            "Opening ebook...",
                        style =
                            MaterialTheme
                                .typography
                                .titleMedium
                    )
                }
            }

            errorMessage != null -> {

                Column(

                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(
                                paddingValues
                            )
                            .padding(
                                24.dp
                            ),

                    verticalArrangement =
                        Arrangement.Center,

                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {

                    Text(
                        text =
                            "Unable to open ebook.",

                        style =
                            MaterialTheme
                                .typography
                                .titleMedium
                    )

                    Text(
                        text =
                            errorMessage
                                ?: "Unknown error.",

                        modifier =
                            Modifier.padding(
                                top = 12.dp
                            )
                    )
                }
            }

            document != null -> {

                val loadedDocument =
                    document!!

                LazyColumn(

                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(
                                paddingValues
                            )
                            .padding(
                                horizontal = 20.dp
                            ),

                    verticalArrangement =
                        Arrangement.spacedBy(
                            18.dp
                        )
                ) {

                    item {

                        Column(
                            modifier =
                                Modifier.padding(
                                    top = 20.dp
                                )
                        ) {

                            Text(
                                text =
                                    loadedDocument.title,

                                style =
                                    MaterialTheme
                                        .typography
                                        .headlineMedium
                            )

                            loadedDocument.author?.let {
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

                    items(
                        loadedDocument.chapters
                    ) { chapter ->

                        Column {

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
                                        top = 10.dp,
                                        bottom = 20.dp
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}