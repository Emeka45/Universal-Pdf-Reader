package com.coeric.universalreader

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun PdfSearchPanel(
    uri: Uri,
    onClose: () -> Unit,
    onResultSelected: (Int) -> Unit
) {

    val context =
        LocalContext.current

    var query by remember {
        mutableStateOf("")
    }

    var searchQuery by remember {
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

    /*
     * Search after the user pauses typing.
     */
    LaunchedEffect(searchQuery) {

        val cleanQuery =
            searchQuery.trim()

        if (cleanQuery.isEmpty()) {

            results =
                emptyList()

            error =
                null

            searching =
                false

            return@LaunchedEffect
        }

        delay(350)

        searching =
            true

        error =
            null

        try {

            results =
                PdfSearch.search(
                    context =
                        context,

                    uri =
                        uri,

                    query =
                        cleanQuery
                )

        } catch (exception: Exception) {

            results =
                emptyList()

            error =
                exception.message
                    ?.takeIf {
                        it.isNotBlank()
                    }
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
                ),

            verticalAlignment =
                androidx.compose.ui.Alignment.CenterVertically
        ) {

            OutlinedTextField(

                value =
                    query,

                onValueChange = {

                    query =
                        it

                    /*
                     * Empty query immediately clears
                     * the results.
                     */
                    if (it.isBlank()) {

                        searchQuery =
                            ""

                        results =
                            emptyList()

                        error =
                            null
                    } else {

                        // Trigger search on change
                        searchQuery =
                            it
                    }
                },

                modifier =
                    Modifier.weight(1f),

                singleLine =
                    true,

                label = {
                    Text(
                        "Search PDF"
                    )
                },

                placeholder = {
                    Text(
                        "Enter a word or phrase"
                    )
                },

                leadingIcon = {

                    Icon(
                        imageVector =
                            Icons.Default.Search,

                        contentDescription =
                            "Search"
                    )
                },

                trailingIcon = {

                    if (query.isNotEmpty()) {

                        IconButton(
                            onClick = {

                                query = ""

                                searchQuery =
                                    ""

                                results =
                                    emptyList()

                                error =
                                    null
                            },

                            modifier =
                                Modifier.padding(0.dp)
                        ) {

                            Icon(
                                imageVector =
                                    Icons.Default.Close,

                                contentDescription =
                                    "Clear search"
                            )
                        }
                    }
                },

                keyboardOptions =
                    KeyboardOptions(
                        imeAction =
                            ImeAction.Search
                    ),

                keyboardActions =
                    KeyboardActions(
                        onSearch = {

                            searchQuery =
                                query.trim()
                        }
                    )
            )

            IconButton(
                onClick =
                    onClose
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
                        ),

                    verticalAlignment =
                        androidx.compose.ui.Alignment.CenterVertically
                ) {

                    CircularProgressIndicator(
                        modifier =
                            Modifier.padding(
                                4.dp
                            )
                    )

                    Text(
                        text =
                            "Searching PDF..."
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

            searchQuery.isNotBlank() &&
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

            results.isNotEmpty() -> {

                Text(
                    text =
                        "${results.size} result${
                            if (results.size == 1)
                                ""
                            else
                                "s"
                        }",

                    style =
                        MaterialTheme
                            .typography
                            .labelLarge,

                    modifier =
                        Modifier.padding(
                            horizontal = 12.dp,
                            vertical = 4.dp
                        )
                )

                LazyColumn(
                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    items(
                        items =
                            results,

                        key = {
                            "${it.page}:${it.text.hashCode()}"
                        }
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
                                        .bodyMedium,

                                maxLines =
                                    4,

                                overflow =
                                    TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
