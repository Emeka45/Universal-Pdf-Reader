package com.coeric.universalreader

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height

@Composable
fun PdfSearchPanel(
    uri: android.net.Uri,
    onClose: () -> Unit,
    onResultSelected: (Int) -> Unit
) {

    val context =
        LocalContext.current

    var query by remember {
        mutableStateOf("")
    }

    var results by remember {
        mutableStateOf<List<PdfSearchResult>>(
            emptyList()
        )
    }

    var searching by remember {
        mutableStateOf(false)
    }

    var error by remember {
        mutableStateOf<String?>(null)
    }

    LaunchedEffect(query) {

        if (
            query.trim().isEmpty()
        ) {

            results =
                emptyList()

            error =
                null

            return@LaunchedEffect
        }

        searching =
            true

        error =
            null

        try {

            results =
                PdfSearch.search(
                    context,
                    uri,
                    query
                )

        } catch (
            exception: Exception
        ) {

            results =
                emptyList()

            error =
                exception.message
                    ?: "PDF search failed."

        } finally {

            searching =
                false
        }
    }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(16.dp)
    ) {

        Row(
            modifier =
                Modifier.fillMaxWidth(),

            horizontalArrangement =
                Arrangement.spacedBy(
                    8.dp
                )
        ) {

            OutlinedTextField(
                value = query,

                onValueChange = {
                    query = it
                },

                modifier =
                    Modifier.weight(1f),

                singleLine = true,

                label = {
                    Text("Search PDF")
                },

                leadingIcon = {

                    Icon(
                        imageVector =
                            Icons.Default.Search,

                        contentDescription =
                            "Search"
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

        Spacer(
            modifier =
                Modifier.height(12.dp)
        )

        when {

            searching -> {

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(12.dp),

                    horizontalArrangement =
                        Arrangement.spacedBy(
                            12.dp
                        )
                ) {

                    CircularProgressIndicator()

                    Text(
                        text =
                            "Searching..."
                    )
                }
            }

            error != null -> {

                Text(
                    text =
                        error
                            ?: "Search failed.",

                    color =
                        MaterialTheme
                            .colorScheme
                            .error,

                    modifier =
                        Modifier.padding(
                            12.dp
                        )
                )
            }

            query.isNotBlank() &&
                results.isEmpty() -> {

                Text(
                    text =
                        "No results found.",

                    modifier =
                        Modifier.padding(
                            12.dp
                        )
                )
            }

            else -> {

                LazyColumn {

                    items(
                        items = results
                    ) { result ->

                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onResultSelected(
                                            result.page
                                        )
                                    }
                                    .padding(
                                        12.dp
                                    )
                        ) {

                            Text(
                                text =
                                    "Page ${
                                        result.page + 1
                                    }",

                                style =
                                    MaterialTheme
                                        .typography
                                        .titleSmall
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(
                                        4.dp
                                    )
                            )

                            Text(
                                text =
                                    result.text,

                                style =
                                    MaterialTheme
                                        .typography
                                        .bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}