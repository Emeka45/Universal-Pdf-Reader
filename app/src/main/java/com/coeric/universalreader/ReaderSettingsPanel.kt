package com.coeric.universalreader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ReaderSettingsPanel(
    settings: ReaderSettings,
    onSettingsChanged: (ReaderSettings) -> Unit
) {

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
        verticalArrangement =
            Arrangement.spacedBy(16.dp)
    ) {

        Text(
            text =
                "Font size: ${
                    settings.fontSize.toInt()
                }sp"
        )

        Slider(
            value =
                settings.fontSize,
            onValueChange = {
                onSettingsChanged(
                    settings.copy(
                        fontSize = it
                    )
                )
            },
            valueRange =
                12f..32f
        )

        Text(
            text =
                "Line spacing: ${
                    String.format(
                        "%.2f",
                        settings.lineSpacing
                    )
                }"
        )

        Slider(
            value =
                settings.lineSpacing,
            onValueChange = {
                onSettingsChanged(
                    settings.copy(
                        lineSpacing = it
                    )
                )
            },
            valueRange =
                1.0f..2.5f
        )

        Text("Theme")

        Row(
            modifier =
                Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {

            ReaderTheme.values().forEach {
                theme ->

                FilterChip(
                    selected =
                        settings.theme == theme,
                    onClick = {

                        onSettingsChanged(
                            settings.copy(
                                theme = theme
                            )
                        )
                    },
                    label = {
                        Text(
                            theme.name
                                .lowercase()
                                .replaceFirstChar {
                                    it.uppercase()
                                }
                        )
                    }
                )
            }
        }

        Text("Text alignment")

        Row(
            modifier =
                Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {

            FilterChip(
                selected =
                    settings.textAlignment ==
                        ReaderTextAlignment.LEFT,
                onClick = {

                    onSettingsChanged(
                        settings.copy(
                            textAlignment =
                                ReaderTextAlignment.LEFT
                        )
                    )
                },
                label = {
                    Text("Left")
                }
            )

            FilterChip(
                selected =
                    settings.textAlignment ==
                        ReaderTextAlignment.JUSTIFY,
                onClick = {

                    onSettingsChanged(
                        settings.copy(
                            textAlignment =
                                ReaderTextAlignment.JUSTIFY
                        )
                    )
                },
                label = {
                    Text("Justify")
                }
            )
        }

        Button(
            onClick = {
                onSettingsChanged(
                    ReaderSettings()
                )
            }
        ) {
            Text("Reset")
        }
    }
}