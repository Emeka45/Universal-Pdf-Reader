package com.coeric.universalreader

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class ReaderActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val documentUri = intent.getStringExtra("document_uri")

        setContent {
            MaterialTheme {

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text("Reader")
                            }
                        )
                    }
                ) { paddingValues ->

                    if (documentUri != null) {

                        ReaderContent(
                            uri = Uri.parse(documentUri),
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues)
                        )

                    } else {

                        Text(
                            text = "No document selected.",
                            modifier = Modifier.padding(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
fun ReaderContent(
    uri: Uri,
    modifier: Modifier = Modifier
) {

    val context = androidx.compose.ui.platform.LocalContext.current

    val text = androidx.compose.runtime.produceState(
        initialValue = "Loading document...",
        uri
    ) {
        value = try {

            context.contentResolver
                .openInputStream(uri)
                ?.bufferedReader()
                ?.use { reader ->
                    reader.readText()
                }
                ?: "Unable to open document."

        } catch (exception: Exception) {

            "Unable to read this document.\n\n${exception.message}"
        }
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {

        Text(
            text = text.value,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}