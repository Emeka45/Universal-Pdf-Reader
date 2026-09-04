package com.coeric.universalreader

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class PdfSearchHit(val page: Int, val snippet: String)

@Composable
fun PdfSearchDialog(
    uri: Uri,
    onDismiss: () -> Unit,
    onOpenPage: (Int) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<PdfSearchHit>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(query, uri) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            results = emptyList()
            error = null
            searching = false
            return@LaunchedEffect
        }

        kotlinx.coroutines.delay(300)
        searching = true
        error = null
        try {
            results = withContext(Dispatchers.IO) {
                PDFBoxResourceLoader.init(androidx.compose.ui.platform.LocalContext.current)
                emptyList()
            }
        } catch (_: Exception) {
            // Replaced below by the context-safe search implementation.
        }
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(query, uri) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return@LaunchedEffect
        kotlinx.coroutines.delay(300)
        searching = true
        error = null
        try {
            val found = withContext(Dispatchers.IO) {
                PDFBoxResourceLoader.init(context)
                context.contentResolver.openInputStream(uri)?.use { input ->
                    PDDocument.load(input).use { document ->
                        val stripper = PDFTextStripper()
                        val hits = mutableListOf<PdfSearchHit>()
                        val needle = trimmed.lowercase()
                        for (page in 1..document.numberOfPages) {
                            stripper.startPage = page
                            stripper.endPage = page
                            val text = stripper.getText(document)
                            if (text.lowercase().contains(needle)) {
                                val normalized = text.replace(Regex("\\s+"), " ").trim()
                                val index = normalized.lowercase().indexOf(needle)
                                val start = (index - 70).coerceAtLeast(0)
                                val end = (index + trimmed.length + 120).coerceAtMost(normalized.length)
                                val snippet = if (normalized.isNotEmpty() && index >= 0) {
                                    normalized.substring(start, end)
                                } else {
                                    normalized.take(190)
                                }
                                hits += PdfSearchHit(page - 1, snippet)
                                if (hits.size >= 100) break
                            }
                        }
                        hits
                    }
                } ?: throw IllegalStateException("Unable to open PDF file.")
            }
            results = found
        } catch (e: Exception) {
            results = emptyList()
            error = e.message ?: "Unable to search PDF."
        } finally {
            searching = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Search PDF") },
        text = {
            Column {
                TextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Enter a word or phrase") }
                )
                when {
                    searching -> Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        horizontalArrangement = Arrangement.Center
                    ) { CircularProgressIndicator() }
                    error != null -> Text(error!!, Modifier.padding(top = 16.dp))
                    query.isNotBlank() && results.isEmpty() -> Text("No results found.", Modifier.padding(top = 16.dp))
                    else -> LazyColumn(modifier = Modifier.padding(top = 8.dp)) {
                        items(results) { hit ->
                            Column {
                                TextButton(
                                    onClick = { onOpenPage(hit.page) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Page ${hit.page + 1}\n${hit.snippet}")
                                }
                                Divider()
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}
