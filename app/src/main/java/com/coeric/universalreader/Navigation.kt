package com.coeric.universalreader

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
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

// QuickAccessCard composable
@androidx.compose.runtime.Composable
fun QuickAccessCard(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    androidx.compose.material3.Card(
        modifier = modifier
    ) {
        androidx.compose.material3.TextButton(
            onClick = onClick,
            modifier = Modifier.padding(8.dp)
        ) {
            androidx.compose.material3.Icon(
                imageVector = icon,
                contentDescription = title
            )
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(4.dp))
            androidx.compose.material3.Text(text = title)
        }
    }
}

// ReaderSettingsDialog placeholder and ReaderSettings data class
// Update this to match your ReaderSettingsRepository model as needed

data class ReaderSettings(
    val theme: ReaderTheme = ReaderTheme.LIGHT,
    val fontSize: Int = 16
)

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
