package com.coeric.universalreader

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.selection.rememberSelectionState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxWidth

@Composable
fun SelectableHighlightText(
    text: String,
    documentUri: String,
    chapterIndex: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
    onHighlightAdded: () -> Unit
) {
    val context =
        androidx.compose.ui.platform.LocalContext.current

    val selectionState =
        rememberSelectionState()

    var showHighlightDialog by remember {
        mutableStateOf(false)
    }

    var selectedText by remember {
        mutableStateOf("")
    }

    LaunchedEffect(
        selectionState.selectedTexts
    ) {
        val selected =
            selectionState.selectedTexts
                .joinToString("") {
                    it.text
                }
                .trim()

        if (
            selected.isNotBlank()
        ) {
            selectedText =
                selected
        }
    }

    SelectionContainer(
        state = selectionState,
        modifier = modifier
    ) {
        content()
    }

    if (
        selectedText.isNotBlank() &&
        showHighlightDialog
    ) {
        HighlightNoteDialog(
            selectedText =
                selectedText,

            onDismiss = {
                showHighlightDialog =
                    false

                selectionState.clear()
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

                selectedText =
                    ""

                selectionState.clear()

                onHighlightAdded()
            }
        )
    }
}