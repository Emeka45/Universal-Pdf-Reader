package com.coeric.universalreader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.selection.SelectionState
import androidx.compose.foundation.text.selection.rememberSelectionState
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SelectableHighlightText(
    text: String,
    documentUri: String,
    chapterIndex: Int,
    textStyle: androidx.compose.ui.text.TextStyle,
    onHighlightAdded: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context =
        androidx.compose.ui.platform.LocalContext.current

    val selectionState =
        rememberSelectionState()

    val selectedText =
        selectionState.selectedTexts
            .joinToString("") {
                it.text
            }
            .trim()

    var showHighlightDialog by remember {
        mutableStateOf(false)
    }

    Column(
        modifier =
            modifier.fillMaxWidth()
    ) {

        SelectionContainer(
            state = selectionState
        ) {

            Text(
                text = text,

                style = textStyle,

                modifier =
                    Modifier.fillMaxWidth()
            )
        }

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
                        selectionState.clear()
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

                selectionState.clear()

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

    androidx.compose.material3.AlertDialog(
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

                androidx.compose.material3.TextField(
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
            androidx.compose.material3.TextButton(
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