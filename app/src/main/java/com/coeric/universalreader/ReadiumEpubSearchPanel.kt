package com.coeric.universalreader

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import kotlinx.coroutines.delay
import org.readium.r2.shared.publication.Publication

@Composable
fun ReadiumEpubSearchPanel(
    publication: Publication,
    onResultSelected: (ReadiumEpubSearchResult) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {

    var query by remember {
        mutableStateOf("")
    }

    var results by remember {
        mutableStateOf(
            emptyList<ReadiumEpubSearchResult>()
        )
    }

    var searching by remember {
        mutableStateOf(false)
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    LaunchedEffect(query) {

        val trimmedQuery =
            query.trim()

        if (trimmedQuery.isEmpty()) {

            results = emptyList()
            searching = false
            errorMessage = null

            return@LaunchedEffect
        }

        delay(300)

        searching = true
        errorMessage = null

        val result =
            ReadiumEpubSearch.search(
                publication = publication,
                query = trimmedQuery
            )

        result
            .onSuccess {
                results = it
            }
            .onFailure {
                results = emptyList()

                errorMessage =
                    it.message
                        ?: "Search failed."
            }

        searching = false
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        tonalElevation = 3.dp
    ) {

        Column(
            modifier =
                Modifier.fillMaxSize()
        ) {

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 8.dp,
                            vertical = 8.dp
                        ),

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                OutlinedTextField(
                    value = query,

                    onValueChange = {
                        query = it
                    },

                    modifier =
                        Modifier.weight(1f),

                    singleLine = true,

                    leadingIcon = {

                        Icon(
                            imageVector =
                                Icons.Default.Search,

                            contentDescription =
                                "Search"
                        )
                    },

                    placeholder = {

                        Text(
                            text =
                                "Search this book"
                        )
                    }
                )

                IconButton(
                    onClick = onClose
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.Close,

                        contentDescription =
                            "Close search"
                    )
                }
            }

            when {

                searching -> {

                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(
                                    24.dp
                                ),

                        horizontalAlignment =
                            Alignment.CenterHorizontally
                    ) {

                        CircularProgressIndicator()

                        Spacer(
                            modifier =
                                Modifier.height(
                                    12.dp
                                )
                        )

                        Text(
                            text =
                                "Searching..."
                        )
                    }
                }

                errorMessage != null -> {

                    Text(
                        text =
                            errorMessage
                                ?: "Search failed.",

                        modifier =
                            Modifier.padding(
                                24.dp
                            ),

                        color =
                            MaterialTheme
                                .colorScheme
                                .error
                    )
                }

                query.isNotBlank() &&
                    results.isEmpty() -> {

                    Text(
                        text =
                            "No results found.",

                        modifier =
                            Modifier.padding(
                                24.dp
                            )
                    )
                }

                else -> {

                    LazyColumn(
                        modifier =
                            Modifier.fillMaxSize()
                    ) {

                        items(
                            items = results
                        ) { result ->

                            ReadiumSearchResultRow(
                                result = result,

                                onClick = {
                                    onResultSelected(
                                        result
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
private fun ReadiumSearchResultRow(
    result: ReadiumEpubSearchResult,
    onClick: () -> Unit
) {

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(
                    onClick = onClick
                )
                .padding(
                    horizontal = 16.dp,
                    vertical = 14.dp
                )
    ) {

        Text(
            text = result.title,

            style =
                MaterialTheme
                    .typography
                    .bodyLarge
        )

        val highlight =
            result
                .locator
                .text
                ?.highlight

        if (!highlight.isNullOrBlank()) {

            Spacer(
                modifier =
                    Modifier.height(
                        4.dp
                    )
            )

            Text(
                text = highlight,

                style =
                    MaterialTheme
                        .typography
                        .bodyMedium,

                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )
        }
    }
}