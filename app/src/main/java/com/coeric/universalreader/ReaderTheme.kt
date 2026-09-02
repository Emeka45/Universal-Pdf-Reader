package com.coeric.universalreader

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun UniversalReaderTheme(
    readerTheme: ReaderTheme,
    content: @Composable () -> Unit
) {
    val colors =
        when (readerTheme) {

            ReaderTheme.LIGHT ->
                lightColorScheme()

            ReaderTheme.DARK ->
                darkColorScheme()

            ReaderTheme.SEPIA ->
                lightColorScheme(

                    background =
                        Color(0xFFF4ECD8),

                    surface =
                        Color(0xFFF4ECD8),

                    surfaceVariant =
                        Color(0xFFE8DFC7),

                    onBackground =
                        Color(0xFF3E3426),

                    onSurface =
                        Color(0xFF3E3426),

                    onSurfaceVariant =
                        Color(0xFF5C5040)
                )
        }

    MaterialTheme(
        colorScheme = colors,
        content = content
    )
}