@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.coeric.universalreader

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.AutoAwesome
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.text.DateFormat
import java.util.Date

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { UniversalReaderHome() } }
    }
}

@Composable
fun UniversalReaderHome() {
    val context = LocalContext.current
    var libraryBooks by remember { mutableStateOf(LibraryRepository.getBooks(context)) }
    var showFavorites by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showAi by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var readerTheme by remember { mutableStateOf(ReaderTheme.LIGHT) }
    var filePickerError by remember { mutableStateOf<String?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val info = try { DocumentDetector.detect(context, uri) } catch (e: Exception) {
            filePickerError = e.message ?: "Unable to open this file."
            return@rememberLauncherForActivityResult
        }
        val extension = info.extension.lowercase()
        val mimeType = info.mimeType?.lowercase() ?: ""
        if (extension == "mp3" || extension == "mp4" || mimeType.startsWith("audio/") || mimeType.startsWith("video/")) {
            filePickerError = "Audio and video files are not supported."
            return@rememberLauncherForActivityResult
        }
        try { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) { }
        LibraryRepository.addOrUpdateBook(context, info)
        libraryBooks = LibraryRepository.getBooks(context)
        openReader(context, info.uri)
    }

    val folderPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        if (uri != null) {
            try { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION) } catch (_: Exception) { }
            Toast.makeText(context, "Folder selected", Toast.LENGTH_SHORT).show()
        }
    }

    fun openFile() {
        filePickerLauncher.launch(arrayOf(
            "application/pdf", "application/epub+zip", "application/x-mobipocket-ebook",
            "text/plain", "text/html", "application/rtf", "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.oasis.opendocument.text", "application/zip", "application/x-rar-compressed", "*/*"
        ))
    }

    val displayedBooks = libraryBooks.filter { if (showFavorites) it.isFavorite else true }.filter {
        searchQuery.isBlank() || it.name.contains(searchQuery, true) || it.extension.contains(searchQuery, true)
    }

    UniversalReaderTheme(readerTheme = readerTheme) {
        Scaffold(topBar = {
            TopAppBar(
                title = { Text("Universal Reader") },
                navigationIcon = { IconButton({ showMenu = true }) { Icon(Icons.Default.Menu, "Menu") } },
                actions = {
                    IconButton({ showAi = true }) { Icon(Icons.Default.AutoAwesome, "AI Assistant") }
                    IconButton({ showSearch = true }) { Icon(Icons.Default.Search, "Search") }
                    IconButton({ showSettings = true }) { Icon(Icons.Default.Settings, "Settings") }
                }
            )
        }) { paddingValues ->
            LazyColumn(Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp)) {
                item {
                    Spacer(Modifier.height(20.dp))
                    Text("Your Library", style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("Read books and documents in one place.", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(20.dp))
                    Button(onClick = { openFile() }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.UploadFile, null)
                        Spacer(Modifier.padding(horizontal = 4.dp))
                        Text("Open File")
                    }
                    Spacer(Modifier.height(20.dp))
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Text(if (showFavorites) "Favorites" else "Recent Books", style = MaterialTheme.typography.titleLarge)
                        IconButton({ showFavorites = !showFavorites }) {
                            Icon(if (showFavorites) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Favorites")
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }
                if (displayedBooks.isEmpty()) {
                    item {
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(if (showFavorites) "No favorite books" else if (searchQuery.isNotBlank()) "No matching books" else "Your library is empty", style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.height(8.dp))
                                Text(if (showFavorites) "Favorite books will appear here." else if (searchQuery.isNotBlank()) "Try a different search term." else "Open a document to add it to your library.", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        Spacer(Modifier.height(20.dp))
                    }
                } else {
                    items(displayedBooks, key = { it.uri }) { book ->
                        LibraryBookCard(book, context, {
                            LibraryRepository.toggleFavorite(context, book.uri)
                            libraryBooks = LibraryRepository.getBooks(context)
                        }, {
                            LibraryRepository.removeBook(context, book.uri)
                            libraryBooks = LibraryRepository.getBooks(context)
                        })
                        Spacer(Modifier.height(10.dp))
                    }
                }
                item {
                    Spacer(Modifier.height(20.dp))
                    Text("Quick Access", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        QuickAccessCard("AI Assistant", Icons.Default.AutoAwesome, Modifier.weight(1f)) { showAi = true }
                        QuickAccessCard("Folders", Icons.Default.Folder, Modifier.weight(1f)) { folderPickerLauncher.launch(null) }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        QuickAccessCard("Favorites", Icons.Default.Favorite, Modifier.weight(1f)) { showFavorites = true }
                        Spacer(Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }

        if (showAi) AiAssistantScreen(onBack = { showAi = false })

        if (showMenu) AlertDialog(onDismissRequest = { showMenu = false }, title = { Text("Menu") }, text = {
            Column {
                TextButton({ showFavorites = true; showMenu = false }, Modifier.fillMaxWidth()) { Text("View Favorites") }
                TextButton({ showFavorites = false; showMenu = false; openFile() }, Modifier.fillMaxWidth()) { Text("Open File") }
                TextButton({ showMenu = false; showSettings = true }, Modifier.fillMaxWidth()) { Text("Settings") }
                TextButton({ showMenu = false; showSearch = true }, Modifier.fillMaxWidth()) { Text("Search Library") }
                TextButton({ showMenu = false; showAi = true }, Modifier.fillMaxWidth()) { Text("AI Assistant") }
            }
        }, confirmButton = { TextButton({ showMenu = false }) { Text("Close") } })

        if (filePickerError != null) AlertDialog(onDismissRequest = { filePickerError = null }, title = { Text("File Not Supported") }, text = { Text(filePickerError!!) }, confirmButton = { TextButton({ filePickerError = null }) { Text("OK") } })

        if (showSearch) AlertDialog(onDismissRequest = { showSearch = false; searchQuery = "" }, title = { Text("Search Library") }, text = {
            OutlinedTextField(searchQuery, { searchQuery = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Book or document name") })
        }, confirmButton = { TextButton({ showSearch = false }) { Text("Done") } }, dismissButton = { TextButton({ searchQuery = "" }) { Text("Clear") } })

        if (showSettings) AlertDialog(onDismissRequest = { showSettings = false }, title = { Text("Reader Settings") }, text = {
            Column {
                Text("Choose the appearance used by the library.", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(readerTheme == ReaderTheme.LIGHT, { readerTheme = ReaderTheme.LIGHT }, label = { Text("Light") })
                    FilterChip(readerTheme == ReaderTheme.DARK, { readerTheme = ReaderTheme.DARK }, label = { Text("Dark") })
                }
                Spacer(Modifier.height(8.dp))
                FilterChip(readerTheme == ReaderTheme.SEPIA, { readerTheme = ReaderTheme.SEPIA }, label = { Text("Sepia") })
            }
        }, confirmButton = { TextButton({ showSettings = false }) { Text("Done") } })
    }
}

@Composable
private fun LibraryBookCard(book: LibraryBook, context: android.content.Context, onFavorite: () -> Unit, onDelete: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(book.name, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(if (book.extension.isNotBlank()) book.extension.uppercase() else book.mimeType ?: "Unknown format", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(4.dp))
                Text("Opened ${DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(book.lastOpened))}", style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onFavorite) { Icon(if (book.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Favorite") }
            IconButton(onDelete) { Icon(Icons.Default.Delete, "Delete") }
        }
    }
}

@Composable
private fun QuickAccessCard(title: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(modifier = modifier.clickable(onClick = onClick)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, title, Modifier.padding(8.dp))
            Spacer(Modifier.height(8.dp))
            Text(title, style = MaterialTheme.typography.titleSmall)
        }
    }
}
