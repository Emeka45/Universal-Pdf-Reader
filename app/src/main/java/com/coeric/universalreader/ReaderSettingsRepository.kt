package com.coeric.universalreader

import android.content.Context

object ReaderSettingsRepository {

    private const val PREFS =
        "universal_reader_settings"

    fun get(
        context: Context,
        documentUri: String
    ): ReaderSettings {

        val prefs =
            context.getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
            )

        val prefix =
            documentUri.hashCode()
                .toString()

        val fontSize =
            prefs.getFloat(
                "${prefix}_font_size",
                18f
            )

        val lineSpacing =
            prefs.getFloat(
                "${prefix}_line_spacing",
                1.55f
            )

        val theme =
            try {

                ReaderTheme.valueOf(
                    prefs.getString(
                        "${prefix}_theme",
                        ReaderTheme.LIGHT.name
                    )!!
                )

            } catch (
                exception: Exception
            ) {
                ReaderTheme.LIGHT
            }

        val alignment =
            try {

                ReaderTextAlignment.valueOf(
                    prefs.getString(
                        "${prefix}_alignment",
                        ReaderTextAlignment.LEFT.name
                    )!!
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

        val prefix =
            documentUri.hashCode()
                .toString()

        context
            .getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
            )
            .edit()
            .putFloat(
                "${prefix}_font_size",
                settings.fontSize
            )
            .putFloat(
                "${prefix}_line_spacing",
                settings.lineSpacing
            )
            .putString(
                "${prefix}_theme",
                settings.theme.name
            )
            .putString(
                "${prefix}_alignment",
                settings.textAlignment.name
            )
            .apply()
    }

    fun reset(
        context: Context,
        documentUri: String
    ) {

        val prefix =
            documentUri.hashCode()
                .toString()

        context
            .getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
            )
            .edit()
            .remove(
                "${prefix}_font_size"
            )
            .remove(
                "${prefix}_line_spacing"
            )
            .remove(
                "${prefix}_theme"
            )
            .remove(
                "${prefix}_alignment"
            )
            .apply()
    }
}