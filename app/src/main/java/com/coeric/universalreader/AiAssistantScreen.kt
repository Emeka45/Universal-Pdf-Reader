package com.coeric.universalreader

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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

private data class AiMessage(val user: Boolean, val text: String)

@Composable
fun AiAssistantScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val messages = remember { mutableStateListOf<AiMessage>() }
    var question by remember { mutableStateOf("") }
    var documentContext by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showSettings by remember { mutableStateOf(false) }

    fun ask() {
        val text = question.trim()
        if (text.isBlank() || loading) return
        messages += AiMessage(true, text)
        question = ""
        loading = true
        scope.launch {
            AiService.ask(context, AiRequest(text, documentContext)).fold(
                onSuccess = { response ->
                    messages += AiMessage(false, response.text + if (response.isDemo) "\n\n[Demo mode]" else "")
                },
                onFailure = { error = it.message ?: "Unable to reach the AI service." }
            )
            loading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Universal Reader AI") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } },
                actions = { IconButton(onClick = { showSettings = true }) { Icon(Icons.Default.Settings, "AI Settings") } }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.AutoAwesome, "AI")
                Column(Modifier.weight(1f)) {
                    Text("Ask your reading assistant", style = MaterialTheme.typography.titleLarge)
                    Text("Summarize, explain, research, study and question your documents.", style = MaterialTheme.typography.bodyMedium)
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = documentContext,
                onValueChange = { documentContext = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 7,
                label = { Text("Document context (optional)") },
                placeholder = { Text("Paste text from a PDF/EPUB here for grounded answers.") }
            )
            Spacer(Modifier.height(12.dp))
            LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (messages.isEmpty()) {
                    item { Text("Try: \"Summarize this passage\", \"Explain this in simple English\", or \"Create five exam questions from it.\"", style = MaterialTheme.typography.bodyMedium) }
                }
                items(messages) { message ->
                    Text(
                        text = if (message.user) "You: ${message.text}" else "AI: ${message.text}",
                        style = if (message.user) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (loading) item { CircularProgressIndicator() }
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = question,
                    onValueChange = { question = it },
                    modifier = Modifier.weight(1f),
                    singleLine = false,
                    maxLines = 4,
                    label = { Text("Ask AI") }
                )
                IconButton(onClick = { ask() }, enabled = question.isNotBlank() && !loading) {
                    Icon(Icons.Default.Send, "Send")
                }
            }
        }
    }

    if (error != null) {
        AlertDialog(
            onDismissRequest = { error = null },
            title = { Text("AI connection error") },
            text = { Text(error!!) },
            confirmButton = { TextButton(onClick = { error = null }) { Text("OK") } }
        )
    }

    if (showSettings) {
        AiSettingsDialog(onDismiss = { showSettings = false })
    }
}

@Composable
private fun AiSettingsDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var endpoint by remember { mutableStateOf(AiService.getEndpoint(context)) }
    var apiKey by remember { mutableStateOf(AiService.getApiKey(context)) }
    var model by remember { mutableStateOf(AiService.getModel(context)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("AI Settings") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("For security, Universal Reader does not ship with an API key. Leave the endpoint empty to use demo mode.", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(endpoint, { endpoint = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("AI endpoint") })
                OutlinedTextField(apiKey, { apiKey = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("API key (optional)") })
                OutlinedTextField(model, { model = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Model") })
            }
        },
        confirmButton = {
            Button(onClick = { AiService.saveSettings(context, endpoint, apiKey, model); onDismiss() }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
