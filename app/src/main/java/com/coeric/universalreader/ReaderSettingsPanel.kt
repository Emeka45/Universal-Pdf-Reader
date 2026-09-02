package com.coeric.universalreader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.RadioButton
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

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 16.dp,
                    vertical = 8.dp
                )
    ) {

        Column(
            modifier =
                Modifier.padding(
                    16.dp
                )
        ) {

            Text(
                text = "Reader Settings"
            )

            Spacer(
                modifier =
                    Modifier.height(
                        12.dp
                    )
            )

            Text(
                text =
                    "Font size: ${
                        settings.fontSize.toInt()
                    }sp"
            )

            Slider(
                value =
                    settings.fontSize,
                onValueChange = { value ->

                    onSettingsChanged(
                        settings.copy(
                            fontSize = value
                        )
                    )
                },
                valueRange =
                    12f..36f
            )

            Spacer(
                modifier =
                    Modifier.height(
                        8.dp
                    )
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
                onValueChange = { value ->

                    onSettingsChanged(
                        settings.copy(
                            lineSpacing = value
                        )
                    )
                },
                valueRange =
                    1.0f..2.5f
            )

            Spacer(
                modifier =
                    Modifier.height(
                        12.dp
                    )
            )

            Text(
                text = "Theme"
            )

            ThemeOption(
                text = "Light",
                selected =
                    settings.theme ==
                        ReaderTheme.LIGHT,
                onClick = {

                    onSettingsChanged(
                        settings.copy(
                            theme =
                                ReaderTheme.LIGHT
                        )
                    )
                }
            )

            ThemeOption(
                text = "Dark",
                selected =
                    settings.theme ==
                        ReaderTheme.DARK,
                onClick = {

                    onSettingsChanged(
                        settings.copy(
                            theme =
                                ReaderTheme.DARK
                        )
                    )
                }
            )

            ThemeOption(
                text = "Sepia",
                selected =
                    settings.theme ==
                        ReaderTheme.SEPIA,
                onClick = {

                    onSettingsChanged(
                        settings.copy(
                            theme =
                                ReaderTheme.SEPIA
                        )
                    )
                }
            )

            Spacer(
                modifier =
                    Modifier.height(
                        12.dp
                    )
            )

            Text(
                text = "Text alignment"
            )

            ThemeOption(
                text = "Left",
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
                }
            )

            ThemeOption(
                text = "Justified",
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
                }
            )

            Spacer(
                modifier =
                    Modifier.height(
                        12.dp
                    )
            )

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.End
            ) {

                Button(
                    onClick = {

                        onSettingsChanged(
                            ReaderSettings()
                        )
                    }
                ) {

                    Text(
                        "Reset"
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {

    Row(
        modifier =
            Modifier.fillMaxWidth()
    ) {

        RadioButton(
            selected =
                selected,
            onClick =
                onClick
        )

        Text(
            text =
                text,
            modifier =
                Modifier.padding(
                    top = 12.dp
                )
        )
    }
}