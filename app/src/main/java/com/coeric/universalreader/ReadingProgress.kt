package com.coeric.universalreader

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun ReadingProgress(
    currentItem: Int,
    totalItems: Int,
    modifier: Modifier = Modifier
) {
    val progress =
        if (totalItems <= 1) {
            0f
        } else {
            (
                currentItem.toFloat() /
                    (totalItems - 1).toFloat()
            ).coerceIn(0f, 1f)
        }

    val percentage =
        (progress * 100f).roundToInt()

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 16.dp,
                    vertical = 4.dp
                )
    ) {

        LinearProgressIndicator(
            progress = {
                progress
            },

            modifier =
                Modifier.fillMaxWidth()
        )

        Text(
            text =
                "$percentage%",

            style =
                MaterialTheme
                    .typography
                    .labelSmall,

            modifier =
                Modifier.padding(
                    top = 2.dp
                )
        )
    }
}