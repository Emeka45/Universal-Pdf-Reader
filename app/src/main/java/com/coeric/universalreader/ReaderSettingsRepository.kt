package com.coeric.universalreader

import android.content.Context

object ReaderSettingsRepository {

    private const val PREFS =
        "universal_reader_settings"

    private const val FONT_SIZE =
        "font_size"

    private const val LINE_SPACING =
        "line_spacing"

    private const val THEME =
        "theme"

    private const val ALIGNMENT =
        "alignment"

    fun load(
        context: Context
    ): ReaderSettings {

        val preferences =
            context.getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
            )

        val theme =
            runCatching {
                ReaderTheme.valueOf(
                    preferences.getString(
                        THEME,
                        ReaderTheme.LIGHT.name
                    ) ?: ReaderTheme.LIGHT.name
                )
            }.getOrDefault(
                ReaderTheme.LIGHT
            )

        val alignment =
            runCatching {
                ReaderTextAlignment.valueOf(
                    preferences.getString(
                        ALIGNMENT,
                        ReaderTextAlignment.LEFT.name
                    ) ?: ReaderTextAlignment.LEFT.name
                )
            }.getOrDefault(
                ReaderTextAlignment.LEFT
            )

        return ReaderSettings(
            fontSize =
                preferences.getFloat(
                    FONT_SIZE,
                    18f
                ),
            lineSpacing =
                preferences.getFloat(
                    LINE_SPACING,
                    1.55f
                ),
            theme =
                theme,
            textAlignment =
                alignment
        )
    }

    fun save(
        context: Context,
        settings: ReaderSettings
    ) {

        context.getSharedPreferences(
            PREFS,
            Context.MODE_PRIVATE
        )
            .edit()
            .putFloat(
                FONT_SIZE,
                settings.fontSize
            )
            .putFloat(
                LINE_SPACING,
                settings.lineSpacing
            )
            .putString(
                THEME,
                settings.theme.name
            )
            .putString(
                ALIGNMENT,
                settings.textAlignment.name
            )
            .apply()
    }
}