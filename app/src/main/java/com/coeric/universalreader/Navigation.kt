package com.coeric.universalreader

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp

// Navigation helpers
fun openReader(context: Context, documentUri: String) {
    val intent = Intent(context, ReaderActivity::class.java).apply {
        putExtra("uri", documentUri)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}

fun openReader(context: Context, uri: Uri) {
    openReader(context, uri.toString())
}

@Composable
fun ReaderSettingsDialog(
    settings: ReaderSettings,
    onSave: (ReaderSettings) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reader Settings") },
        text = {
            Column(modifier = Modifier.padding(8.dp)) {
                Text("Theme: ${settings.theme}")
                Text("Font size: ${settings.fontSize}")
            }
        },
        confirmButton = {
            Button(onClick = { onSave(settings) }) {
                Text("Save")
            }
        }
    )
}
