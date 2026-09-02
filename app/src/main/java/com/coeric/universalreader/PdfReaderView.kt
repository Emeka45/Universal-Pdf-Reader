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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.TextIncrease
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class ReaderActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val documentUri = intent.getStringExtra("document_uri")

        setContent {
            MaterialTheme {

                if (documentUri != null) {

                    ReaderScreen(
                        uri = Uri.parse(documentUri),
                        onBack = {
                            finish()
                        }
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

@Composable
fun ReaderScreen(
    uri: Uri,
    onBack: () -> Unit
) {

    val context = LocalContext.current

    var fontSize by remember {
        mutableFloatStateOf(18f)
    }

    var showFontControls by remember {
        mutableStateOf(false)
    }

    val documentText = produceState(
        initialValue = "Loading document...",
        key1 = uri
    ) {

        value = try {

            context.contentResolver
                .openInputStream(uri)
                ?.bufferedReader()
                ?.use { reader ->
                    reader.readText()
                }
                ?: "Unable to open this document."

        } catch (exception: Exception) {

            "Unable to read this document.\n\n${exception.message}"
        }
    }

    Scaffold(

        topBar = {

            TopAppBar(

                title = {
                    Text("Reader")
                },

                navigationIcon = {

                    IconButton(
                        onClick = onBack
                    ) {

                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },

                actions = {

                    IconButton(
                        onClick = {
                            showFontControls = !showFontControls
                        }
                    ) {

                        Icon(
                            imageVector = Icons.Default.TextIncrease,
                            contentDescription = "Font size"
                        )
                    }
                }
            )
        }

    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            if (showFontControls) {

                Column(
                    modifier = Modifier.padding(
                        horizontal = 20.dp,
                        vertical = 8.dp
                    )
                ) {

                    Text(
                        text = "Text Size",
                        style = MaterialTheme.typography.labelLarge
                    )

                    Slider(
                        value = fontSize,
                        onValueChange = {
                            fontSize = it
                        },
                        valueRange = 12f..32f
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(
                        rememberScrollState()
                    )
                    .padding(20.dp)
            ) {

                Text(
                    text = documentText.value,
                    style = TextStyle(
                        fontSize = fontSize.sp,
                        lineHeight = (fontSize * 1.55f).sp
                    )
                )
            }
        }
    }
}