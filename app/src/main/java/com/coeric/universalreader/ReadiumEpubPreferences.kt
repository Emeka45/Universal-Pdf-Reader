package com.coeric.universalreader

import org.readium.r2.navigator.preferences.TextAlign
import org.readium.r2.navigator.preferences.Theme
import org.readium.r2.navigator.epub.EpubPreferences

object ReadiumEpubPreferences {

    fun fromReaderSettings(
        settings: ReaderSettings
    ): EpubPreferences {

        val theme =
            when (settings.theme) {

                ReaderTheme.LIGHT ->
                    Theme.LIGHT

                ReaderTheme.DARK ->
                    Theme.DARK

                ReaderTheme.SEPIA ->
                    Theme.LIGHT
            }

        val textAlign =
            when (settings.textAlignment) {

                ReaderTextAlignment.LEFT ->
                    TextAlign.START

                ReaderTextAlignment.JUSTIFY ->
                    TextAlign.JUSTIFY
            }

        return EpubPreferences(

            fontSize =
                settings.fontSize
                    .toDouble(),

            lineHeight =
                settings.lineSpacing
                    .toDouble(),

            pageMargins = 1.2,

            scroll = true,

            textAlign =
                textAlign,

            theme =
                theme,

            publisherStyles = false
        )
    }
}