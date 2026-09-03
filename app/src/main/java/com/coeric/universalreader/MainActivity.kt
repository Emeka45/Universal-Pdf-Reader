@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.coeric.universalreader

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.text.DateFormat
import java.util.Date

class MainActivity : ComponentActivity() {

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )

        setContent {

            MaterialTheme {

                UniversalReaderHome()
            }
        }
    }
}

@Composable
fun UniversalReaderHome() {

    val context =
        LocalContext.current

    var libraryBooks by remember {

        mutableStateOf(
            LibraryRepository.getBooks(
                context
            )
        )
    }

    var showFavorites by remember {
        mutableStateOf(false)
    }

    val filePickerLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.OpenDocument()
        ) { uri: Uri? ->

            if (uri != null) {

                try {

                    context.contentResolver
                        .takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )

                } catch (
                    exception: Exception
                ) {
                    // Some providers do not support
                    // persistent URI permissions.
                }

                val document =
                    DocumentDetector.detect(
                        context,
                        uri
                    )

                LibraryRepository.addOrUpdateBook(
                    context,
                    document
                )

                libraryBooks =
                    LibraryRepository.getBooks(
                        context
                    )

                openReader(
                    context,
                    document.uri
                )
            }
        }

    fun openFile() {

        filePickerLauncher.launch(
            arrayOf(
                "application/pdf",
                "application/epub+zip",
                "application/x-mobipocket-ebook",
                "text/plain",
                "text/html",
                "application/rtf",
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "application/vnd.oasis.opendocument.text",
                "application/zip",
                "application/x-rar-compressed",
                "*/*"
            )
        )
    }

    val displayedBooks =
        if (showFavorites) {

            libraryBooks.filter {
                it.isFavorite
            }

        } else {

            libraryBooks
        }

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
                        onClick = {}
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.Menu,
                            contentDescription =
                                "Menu"
                        )
                    }
                },

                actions = {

                    IconButton(
                        onClick = {}
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.Search,
                            contentDescription =
                                "Search"
                        )
                    }

                    IconButton(
                        onClick = {}
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.Settings,
                            contentDescription =
                                "Settings"
                        )
                    }
                }
            )
        }

    ) { paddingValues ->

        LazyColumn(

            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(
                        paddingValues
                    )
                    .padding(
                        horizontal = 16.dp
                    )
        ) {

            item {

                Spacer(
                    modifier =
                        Modifier.height(
                            20.dp
                        )
                )

                Text(
                    text =
                        "Your Library",
                    style =
                        MaterialTheme
                            .typography
                            .headlineMedium
                )

                Spacer(
                    modifier =
                        Modifier.height(
                            8.dp
                        )
                )

                Text(
                    text =
                        "Read books and documents in one place.",
                    style =
                        MaterialTheme
                            .typography
                            .bodyLarge
                )

                Spacer(
                    modifier =
                        Modifier.height(
                            20.dp
                        )
                )
            }

            item {

                Button(
                    onClick = {
                        openFile()
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.UploadFile,
                        contentDescription =
                            null
                    )

                    Spacer(
                        modifier =
                            Modifier.padding(
                                horizontal = 4.dp
                            )
                    )

                    Text(
                        "Open File"
                    )
                }

                Spacer(
                    modifier =
                        Modifier.height(
                            20.dp
                        )
                )
            }

            item {

                Row(

                    modifier =
                        Modifier.fillMaxWidth(),

                    horizontalArrangement =
                        Arrangement.SpaceBetween,

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Text(
                        text =
                            if (showFavorites) {
                                "Favorites"
                            } else {
                                "Recent Books"
                            },
                        style =
                            MaterialTheme
                                .typography
                                .titleLarge
                    )

                    IconButton(
                        onClick = {

                            showFavorites =
                                !showFavorites
                        }
                    ) {

                        Icon(
                            imageVector =
                                if (showFavorites) {
                                    Icons.Default.Favorite
                                } else {
                                    Icons.Default.FavoriteBorder
                                },
                            contentDescription =
                                "Favorites"
                        )
                    }
                }

                Spacer(
                    modifier =
                        Modifier.height(
                            10.dp
                        )
                )
            }

            if (displayedBooks.isEmpty()) {

                item {

                    Card(
                        modifier =
                            Modifier.fillMaxWidth()
                    ) {

                        Column(
                            modifier =
                                Modifier.padding(
                                    20.dp
                                ),
                            horizontalAlignment =
                                Alignment.CenterHorizontally
                        ) {

                            Text(
                                text =
                                    if (
                                        showFavorites
                                    ) {
                                        "No favorite books"
                                    } else {
                                        "Your library is empty"
                                    },
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

                            Text(
                                text =
                                    if (
                                        showFavorites
                                    ) {
                                        "Favorite books will appear here."
                                    } else {
                                        "Open a document to add it to your library."
                                    },
                                style =
                                    MaterialTheme
                                        .typography
                                        .bodyMedium
                            )
                        }
                    }

                    Spacer(
                        modifier =
                            Modifier.height(
                                20.dp
                            )
                    )
                }

            } else {

                items(
                    items =
                        displayedBooks,
                    key = {
                        it.uri
                    }
                ) { book ->

                    LibraryBookCard(

                        book =
                            book,

                        context =
                            context,

                        onFavorite = {

                            LibraryRepository
                                .toggleFavorite(
                                    context,
                                    book.uri
                                )

                            libraryBooks =
                                LibraryRepository
                                    .getBooks(
                                        context
                                    )
                        },

                        onDelete = {

                            LibraryRepository
                                .removeBook(
                                    context,
                                    book.uri
                                )

                            libraryBooks =
                                LibraryRepository
                                    .getBooks(
                                        context
                                    )
                        }
                    )

                    Spacer(
                        modifier =
                            Modifier.height(
                                10.dp
                            )
                    )
                }
            }

            item {

                Spacer(
                    modifier =
                        Modifier.height(
                            20.dp
                        )
                )

                Text(
                    text =
                        "Quick Access",
                    style =
                        MaterialTheme
                            .typography
                            .titleLarge
                )

                Spacer(
                    modifier =
                        Modifier.height(
                            12.dp
                        )
                )

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(
                            10.dp
                        )
                ) {

                    QuickAccessCard(
                        title =
                            "Folders",
                        icon =
                            Icons.Default.Folder,
                        modifier =
                            Modifier.weight(1f)
                    )

                    QuickAccessCard(
                        title =
                            "Favorites",
                        icon =
                            Icons.Default.Favorite,
                        modifier =
                            Modifier.weight(1f)
                    )
                }

                Spacer(
                    modifier =
                        Modifier.height(
                            24.dp
                        )
                )
            }
        }
    }
}

@Composable
private fun LibraryBookCard(
    book: LibraryBook,
    context: android.content.Context,
    onFavorite: () -> Unit,
    onDelete: () -> Unit
) {

    Card(
        modifier =
            Modifier.fillMaxWidth()
    ) {

        Column(
            modifier =
                Modifier.padding(
                    16.dp
                )
        ) {

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Column(
                    modifier =
                        Modifier.weight(1f)
                ) {

                    Text(
                        text =
                            book.name,
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
                            if (
                                book.extension
                                    .isNotBlank()
                            ) {
                                book.extension
                                    .uppercase()
                            } else {
                                book.mimeType
                                    ?: "Unknown format"
                            },
                        style =
                            MaterialTheme
                                .typography
                                .bodySmall
                    )

                    Spacer(
                        modifier =
                            Modifier.height(
                                4.dp
                            )
                    )

                    Text(
                        text =
                            "Opened ${
                                DateFormat
                                    .getDateTimeInstance(
                                        DateFormat
                                            .SHORT,
                                        DateFormat
                                            .SHORT
                                    )
                                    .format(
                                        Date(
                                            book.lastOpened
                                        )
                                    )
                            }",
                        style =
                            MaterialTheme
                                .typography
                                .bodySmall
                    )
                }

                IconButton(
                    onClick =
                        onFavorite
                ) {

                    Icon(
                        imageVector =
                            if (
                                book.isFavorite
                            ) {
                                Icons.Default.Favorite
                            } else {
                                Icons.Default.FavoriteBorder
                            },
                        contentDescription =
                            "Favorite"
                    )
                }

                IconButton(
                    onClick =
                        onDelete
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.Delete,
                        contentDescription =
                            "Remove from library"
                    )
                }
            }

            Spacer(
                modifier =
                    Modifier.height(
                        10.dp
                    )
            )

            Button(
                onClick = {

                    openReader(
                        context,
                        book.uri
                    )
                },
                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Text(
                    "Open"
                )
            }
        }
    }
}

@Composable
fun QuickAccessCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {

    Card(
        modifier =
            modifier
    ) {

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        16.dp
                    ),
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Icon(
                imageVector =
                    icon,
                contentDescription =
                    title
            )

            Spacer(
                modifier =
                    Modifier.height(
                        8.dp
                    )
            )

            Text(
                text =
                    title,
                style =
                    MaterialTheme
                        .typography
                        .titleMedium
            )
        }
    }
}

private fun openReader(
    context: android.content.Context,
    uri: String
) {

    val intent =
        Intent(
            context,
            ReaderActivity::class.java
        ).apply {

            putExtra(
                "uri",
                uri
            )
        }

    context.startActivity(
        intent
    )
}