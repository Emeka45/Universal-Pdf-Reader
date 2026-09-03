package com.coeric.universalreader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

@Composable
fun SelectableHighlightText(
    text: String,
    documentUri: String,
    chapterIndex: Int,
    textStyle: TextStyle,
    onHighlightAdded: () -> Unit,
    modifier: Modifier = Modifier
) {

    val context =
        LocalContext.current

    var textFieldValue by remember(
        text
    ) {
        mutableStateOf(
            TextFieldValue(
                text = text,
                selection = TextRange.Zero
            )
        )
    }

    var showHighlightDialog by remember {
        mutableStateOf(false)
    }

    val selection =
        textFieldValue.selection

    val selectedText =
        if (
            selection.start !=
                selection.end &&
            selection.start >= 0 &&
            selection.end <=
                textFieldValue.text.length
        ) {

            val start =
                minOf(
                    selection.start,
                    selection.end
                )

            val end =
                maxOf(
                    selection.start,
                    selection.end
                )

            textFieldValue.text
                .substring(
                    start,
                    end
                )
                .trim()

        } else {
            ""
        }

    Column(
        modifier =
            modifier.fillMaxWidth()
    ) {

        BasicTextField(

            value =
                textFieldValue,

            onValueChange = {
                newValue ->

                textFieldValue =
                    newValue.copy(
                        text = text
                    )
            },

            readOnly = true,

            textStyle =
                textStyle,

            keyboardOptions =
                KeyboardOptions.Default,

            modifier =
                Modifier.fillMaxWidth(),

            decorationBox = {
                innerTextField ->

                innerTextField()
            }
        )

        if (
            selectedText.isNotBlank()
        ) {

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            top = 8.dp
                        ),

                horizontalArrangement =
                    Arrangement.spacedBy(
                        8.dp
                    )
            ) {

                Button(
                    onClick = {
                        showHighlightDialog = true
                    }
                ) {

                    Text(
                        text =
                            "Highlight"
                    )
                }

                OutlinedButton(
                    onClick = {

                        textFieldValue =
                            textFieldValue.copy(
                                selection =
                                    TextRange.Zero
                            )
                    }
                ) {

                    Text(
                        text =
                            "Clear Selection"
                    )
                }
            }
        }
    }

    if (
        showHighlightDialog
    ) {

        HighlightNoteDialog(
            selectedText =
                selectedText,

            onDismiss = {

                showHighlightDialog =
                    false
            },

            onSave = {
                note ->

                HighlightRepository.addHighlight(
                    context =
                        context,

                    documentUri =
                        documentUri,

                    chapterIndex =
                        chapterIndex,

                    selectedText =
                        selectedText,

                    note =
                        note
                )

                showHighlightDialog =
                    false

                textFieldValue =
                    textFieldValue.copy(
                        selection =
                            TextRange.Zero
                    )

                onHighlightAdded()
            }
        )
    }
}

@Composable
private fun HighlightNoteDialog(
    selectedText: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {

    var note by remember {
        mutableStateOf("")
    }

    AlertDialog(

        onDismissRequest =
            onDismiss,

        title = {

            Text(
                text =
                    "Highlight Text"
            )
        },

        text = {

            Column(
                verticalArrangement =
                    Arrangement.spacedBy(
                        12.dp
                    )
            ) {

                Text(
                    text =
                        "\"$selectedText\""
                )

                TextField(

                    value =
                        note,

                    onValueChange = {
                        note = it
                    },

                    label = {

                        Text(
                            text =
                                "Note (optional)"
                        )
                    },

                    modifier =
                        Modifier.fillMaxWidth()
                )
            }
        },

        confirmButton = {

            Button(
                onClick = {
                    onSave(note)
                }
            ) {

                Text(
                    text =
                        "Save Highlight"
                )
            }
        },

        dismissButton = {

            TextButton(
                onClick =
                    onDismiss
            ) {

                Text(
                    text =
                        "Cancel"
                )
            }
        }
    )
}