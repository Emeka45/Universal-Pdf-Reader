package com.coeric.universalreader

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ReadiumTocPanel(
    items: List<ReadiumTocItem>,
    onItemSelected: (ReadiumTocItem) -> Unit
) {

    LazyColumn(
        modifier =
            Modifier.fillMaxWidth()
    ) {

        items(
            items = items
        ) { item ->

            ReadiumTocRow(
                item = item,
                level = 0,
                onItemSelected =
                    onItemSelected
            )
        }
    }
}

@Composable
private fun ReadiumTocRow(
    item: ReadiumTocItem,
    level: Int,
    onItemSelected: (ReadiumTocItem) -> Unit
) {

    Column(
        modifier =
            Modifier.fillMaxWidth()
    ) {

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable {
                        onItemSelected(item)
                    }
                    .padding(
                        start =
                            (16 + level * 20).dp,
                        top = 12.dp,
                        bottom = 12.dp,
                        end = 16.dp
                    )
        ) {

            Text(
                text = item.title,

                style =
                    MaterialTheme
                        .typography
                        .bodyLarge
            )
        }

        if (item.children.isNotEmpty()) {

            item.children.forEach { child ->

                ReadiumTocRow(
                    item = child,
                    level = level + 1,
                    onItemSelected =
                        onItemSelected
                )
            }
        }

        Spacer(
            modifier =
                Modifier.width(1.dp)
        )
    }
}