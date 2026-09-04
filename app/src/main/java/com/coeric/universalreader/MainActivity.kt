@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.coeric.universalreader

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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

    var showMenu by remember {
        mutableStateOf(false)
    }

    var showSearch by remember {
        mutableStateOf(false)
    }

    var showSettings by remember {
        mutableStateOf(false)
    }

    var searchQuery by remember {
        mutableStateOf("")
    }

    var readerTheme by remember {
        mutableStateOf(
            ReaderTheme.LIGHT
        )
    }

    var filePickerError by remember {
        mutableStateOf<String?>(null)
    }

    val filePickerLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.OpenDocument()
        ) { uri: Uri? ->

            if (uri != null) {

                try {

                    val info =
                        DocumentDetector.detect(
                            context,
                            uri
                        )

                    // Reject MP3 and MP4 files
                    val extension = info.extension.lowercase()
                    if (extension == "mp3" || extension == "mp4") {
                        filePickerError =
                            "Audio and video files (MP3, MP4) are not supported."
                        return@rememberLauncherForActivityResult
                    }

                    // Check MIME type
                    val mimeType = info.mimeType?.lowercase() ?: ""
                    if (mimeType.startsWith("audio/") ||
                        mimeType.startsWith("video/")) {
                        filePickerError =
                            "Audio and video files are not supported."
                        return@rememberLauncherForActivityResult
                    }

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

    val folderPickerLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.OpenDocumentTree()
        ) { uri: Uri? ->

            if (uri != null) {

                try {

                    context.contentResolver
                        .takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        )

                } catch (
                    exception: Exception
                ) {
                    // Some providers do not support
                    // persistent tree permissions.
                }

                Toast.makeText(
                    context,
                    "Folder selected",
                    Toast.LENGTH_SHORT
                ).show()
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
        libraryBooks
            .filter { book ->

                if (showFavorites) {
                    book.isFavorite
                } else {
                    true
                }
            }
            .filter { book ->

                if (searchQuery.isBlank()) {
                    true
                } else {

                    book.name.contains(
                        searchQuery,
                        ignoreCase = true
                    ) ||
                        book.extension.contains(
                            searchQuery,
                            ignoreCase = true
                        )
                }
            }

    UniversalReaderTheme(
        readerTheme = readerTheme
    ) {

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
                                showMenu = true
                            }
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
                            onClick = {
                                showSearch = true
                            }
                        ) {

                            Icon(
                                imageVector =
                                    Icons.Default.Search,
                                contentDescription =
                                    "Search"
                            )
                        }

                        IconButton(
                            onClick = {
                                showSettings = true
                            }
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
                                        } else if (
                                            searchQuery.isNotBlank()
                                        ) {
                                            "No matching books"
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
                                        } else if (
                                            searchQuery.isNotBlank()
                                        ) {
                                            "Try a different search term."
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
                                Modifier.weight(1f),
                            onClick = {
                                folderPickerLauncher.launch(
                                    null
                                )
                            }
                        )

                        QuickAccessCard(
                            title =
                                "Favorites",
                            icon =
                                Icons.Default.Favorite,
                            modifier =
                                Modifier.weight(1f),
                            onClick = {

                                showFavorites =
                                    true
                            }
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

        if (showMenu) {

            AlertDialog(

                onDismissRequest = {
                    showMenu = false
                },

                title = {
                    Text("Menu")
                },

                text = {

                    Column {

                        TextButton(
                            onClick = {

                                showFavorites = true
                                showMenu = false
                            },
                            modifier =
                                Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "View Favorites"
                            )
                        }

                        TextButton(
                            onClick = {

                                showFavorites = false
                                showMenu = false
                                openFile()
                            },
                            modifier =
                                Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "Open File"
                            )
                        }

                        TextButton(
                            onClick = {

                                showMenu = false
                                showSettings = true
                            },
                            modifier =
                                Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "Settings"
                            )
                        }

                        TextButton(
                            onClick = {

                                showMenu = false
                                showSearch = true
                            },
                            modifier =
                                Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "Search Library"
                            )
                        }
                    }
                },

                confirmButton = {

                    TextButton(
                        onClick = {
                            showMenu = false
                        }
                    ) {
                        Text("Close")
                    }
                }
            )
        }

        if (filePickerError != null) {

            AlertDialog(

                onDismissRequest = {
                    filePickerError = null
                },

                title = {
                    Text("File Not Supported")
                },

                text = {
                    Text(
                        filePickerError ?: "This file type is not supported."
                    )
                },

                confirmButton = {

                    TextButton(
                        onClick = {
                            filePickerError = null
                        }
                    ) {
                        Text("OK")
                    }
                }
            )
        }

        if (showSearch) {

            AlertDialog(

                onDismissRequest = {
                    showSearch = false
                    searchQuery = ""
                },

                title = {
                    Text("Search Library")
                },

                text = {

                    OutlinedTextField(
                        value =
                            searchQuery,
                        onValueChange = {
                            searchQuery = it
                        },
                        modifier =
                            Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = {
                            Text(
                                "Book or document name"
                            )
                        }
                    )
                },

                confirmButton = {

                    TextButton(
                        onClick = {
                            showSearch = false
                        }
                    ) {
                        Text("Done")
                    }
                },

                dismissButton = {

                    TextButton(
                        onClick = {
                            searchQuery = ""
                        }
                    ) {
                        Text("Clear")
                    }
                }
            )
        }

        if (showSettings) {

            AlertDialog(

                onDismissRequest = {
                    showSettings = false
                },

                title = {
                    Text("Reader Settings")
                },

                text = {

                    Column {

                        Text(
                            text =
                                "Choose the appearance used by the library.",
                            style =
                                MaterialTheme
                                    .typography
                                    .bodyMedium
                        )

                        Spacer(
                            modifier =
                                Modifier.height(
                                    16.dp
                                )
                        )

                        Row(
                            horizontalArrangement =
                                Arrangement.spacedBy(
                                    8.dp
                                )
                        ) {

                            FilterChip(
                                selected =
                                    readerTheme ==
                                        ReaderTheme.LIGHT,
                                onClick = {
                                    readerTheme =
                                        ReaderTheme.LIGHT
                                },
                                label = {
                                    Text("Light")
                                }
                            )

                            FilterChip(
                                selected =
                                    readerTheme ==
                                        ReaderTheme.DARK,
                                onClick = {
                                    readerTheme =
                                        ReaderTheme.DARK
                                },
                                label = {
                                    Text("Dark")
                                }
                            )
                        }

                        Spacer(
                            modifier =
                                Modifier.height(
                                    8.dp
                                )
                        )

                        FilterChip(
                            selected =
                                readerTheme ==
                                    ReaderTheme.SEPIA,
                            onClick = {
                                readerTheme =
                                    ReaderTheme.SEPIA
                            },
                            label = {
                                Text("Sepia")
                            }
                        )
                    }
                },

                confirmButton = {

                    TextButton(
                        onClick = {
                            showSettings = false
                        }
                    ) {
                        Text("Done")
                    }
                }
            )
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
                    onClick = onFavorite
                ) {

                    Icon(
                        imageVector =
                            if (book.isFavorite) {
                                Icons.Default.Favorite
                            } else {
                                Icons.Default.FavoriteBorder
                            },
                        contentDescription =
                            if (book.isFavorite) {
                                "Remove from favorites"
                            } else {
                                "Add to favorites"
                            }
                    )
                }

                IconButton(
                    onClick = onDelete
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.Delete,
                        contentDescription =
                            "Delete"
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickAccessCard(
    title: String,
    icon: androidx.compose.material.icons.Icons,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {

    Card(
        modifier =
            modifier
    ) {

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            horizontalAlignment =
                Alignment.CenterHorizontally,
            verticalArrangement =
                Arrangement.Center
        ) {

            Icon(
                imageVector =
                    icon,
                contentDescription =
                    title,
                modifier =
                    Modifier.padding(
                        8.dp
                    )
            )

            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )

            Text(
                text = title,
                style =
                    MaterialTheme
                        .typography
                        .titleSmall
            )
        }
    }
}
