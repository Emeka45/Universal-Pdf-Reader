package com.coeric.universalreader

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MobiReaderScreen(
    uri: Uri,
    format: DocumentFormat,
    onBack: () -> Unit
) {

    Scaffold(

        topBar = {

            TopAppBar(

                title = {

                    Text(
                        when (format) {

                            DocumentFormat.MOBI ->
                                "MOBI Reader"

                            DocumentFormat.AZW ->
                                "AZW Reader"

                            DocumentFormat.AZW3 ->
                                "AZW3 Reader"

                            else ->
                                "Ebook Reader"
                        }
                    )
                },

                navigationIcon = {

                    IconButton(
                        onClick =
                            onBack
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.ArrowBack,
                            contentDescription =
                                "Back"
                        )
                    }
                }
            )
        }

    ) { paddingValues ->

        Column(

            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(
                        paddingValues
                    )
                    .padding(
                        24.dp
                    ),

            verticalArrangement =
                Arrangement.Center,

            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Text(
                text =
                    "MOBI/AZW reader engine ready for integration.",
                style =
                    MaterialTheme
                        .typography
                        .titleMedium
            )

            Text(
                text =
                    "The binary parser will be connected next.",
                modifier =
                    Modifier.padding(
                        top = 12.dp
                    )
            )
        }
    }
}