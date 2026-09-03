package com.coeric.universalreader

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun DocumentSearchScreen(
    uri: android.net.Uri,
    onBack: () -> Unit,
    onResultSelected: (Int) -> Unit
) {

    val context =
        LocalContext.current

    var query by remember {
        mutableStateOf("")
    }

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

    val document =
        epubState.value
            ?.getOrNull()

    val results =
        remember(
            document,
            query
        ) {

            if (
                document != null &&
                query.isNotBlank()
            ) {

                DocumentSearch.search(
                    document,
                    query
                )

            } else {
                emptyList()
            }
        }

    Scaffold(

        topBar = {

            TopAppBar(

                title = {
                    Text(
                        "Search"
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
                        16.dp
                    )
        ) {

            OutlinedTextField(

                value =
                    query,

                onValueChange = {
                    query = it
                },

                modifier =
                    Modifier.fillMaxWidth(),

                label = {
                    Text(
                        "Search this book"
                    )
                },

                leadingIcon = {

                    Icon(
                        imageVector =
                            Icons.Default.Search,
                        contentDescription =
                            null
                    )
                },

                singleLine = true
            )

            Spacer(
                modifier =
                    Modifier.height(
                        16.dp
                    )
            )

            when {

                epubState.value == null -> {

                    Text(
                        "Loading book..."
                    )
                }

                document == null -> {

                    Text(
                        "Unable to search this document."
                    )
                }

                query.isBlank() -> {

                    Text(
                        "Enter a word or phrase to search."
                    )
                }

                results.isEmpty() -> {

                    Text(
                        "No results found."
                    )
                }

                else -> {

                    Text(
                        text =
                            "${results.size} result(s)",
                        style =
                            MaterialTheme
                                .typography
                                .titleMedium
                    )

                    Spacer(
                        modifier =
                            Modifier.height(
                                8.dp
                            )
                    )

                    LazyColumn {

                        items(
                            results
                        ) { result ->

                            SearchResultCard(
                                result =
                                    result,
                                onClick = {

                                    onResultSelected(
                                        result.chapterIndex
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultCard(
    result: SearchResult,
    onClick: () -> Unit
) {

    Column(

        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(
                    onClick =
                        onClick
                )
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
                    .titleMedium
        )

        Spacer(
            modifier =
                Modifier.height(
                    4.dp
                )
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