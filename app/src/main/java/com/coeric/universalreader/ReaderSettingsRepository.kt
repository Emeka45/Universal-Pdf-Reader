package com.coeric.universalreader

import android.content.Context

object ReaderSettingsRepository {

    private const val PREFS_NAME =
        "universal_reader_settings"

    private const val FONT_SIZE =
        "font_size"

    private const val LINE_SPACING =
        "line_spacing"

    private const val THEME =
        "theme"

    private const val TEXT_ALIGNMENT =
        "text_alignment"

    fun get(
        context: Context,
        documentUri: String
    ): ReaderSettings {

        val preferences =
            context.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )

        val fontSize =
            preferences.getFloat(
                "${documentUri}_$FONT_SIZE",
                18f
            )

        val lineSpacing =
            preferences.getFloat(
                "${documentUri}_$LINE_SPACING",
                1.55f
            )

        val themeName =
            preferences.getString(
                "${documentUri}_$THEME",
                ReaderTheme.LIGHT.name
            )

        val alignmentName =
            preferences.getString(
                "${documentUri}_$TEXT_ALIGNMENT",
                ReaderTextAlignment.LEFT.name
            )

        val theme =
            try {
                ReaderTheme.valueOf(
                    themeName
                        ?: ReaderTheme.LIGHT.name
                )
            } catch (
                exception: Exception
            ) {
                ReaderTheme.LIGHT
            }

        val alignment =
            try {
                ReaderTextAlignment.valueOf(
                    alignmentName
                        ?: ReaderTextAlignment.LEFT.name
                )
            } catch (
                exception: Exception
            ) {
                ReaderTextAlignment.LEFT
            }

        return ReaderSettings(
            fontSize = fontSize,
            lineSpacing = lineSpacing,
            theme = theme,
            textAlignment = alignment
        )
    }

    fun save(
        context: Context,
        documentUri: String,
        settings: ReaderSettings
    ) {

        context
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
            .edit()
            .putFloat(
                "${documentUri}_$FONT_SIZE",
                settings.fontSize
            )
            .putFloat(
                "${documentUri}_$LINE_SPACING",
                settings.lineSpacing
            )
            .putString(
                "${documentUri}_$THEME",
                settings.theme.name
            )
            .putString(
                "${documentUri}_$TEXT_ALIGNMENT",
                settings.textAlignment.name
            )
            .apply()
    }

    fun reset(
        context: Context,
        documentUri: String
    ) {

        context
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
            .edit()
            .remove(
                "${documentUri}_$FONT_SIZE"
            )
            .remove(
                "${documentUri}_$LINE_SPACING"
            )
            .remove(
                "${documentUri}_$THEME"
            )
            .remove(
                "${documentUri}_$TEXT_ALIGNMENT"
            )
            .apply()
    }
}