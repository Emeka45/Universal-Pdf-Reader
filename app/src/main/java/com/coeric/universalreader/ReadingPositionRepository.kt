package com.coeric.universalreader

import android.content.Context
import org.json.JSONObject

object ReadingPositionRepository {

    private const val PREFS_NAME =
        "universal_reader_positions"

    private const val KEY_POSITIONS =
        "positions"

    fun save(
        context: Context,
        position: ReadingPosition
    ) {

        val preferences =
            context.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )

        val existing =
            JSONObject(
                preferences.getString(
                    KEY_POSITIONS,
                    "{}"
                ) ?: "{}"
            )

        existing.put(
            position.documentUri,
            JSONObject().apply {

                put(
                    "chapterIndex",
                    position.chapterIndex
                )

                put(
                    "scrollIndex",
                    position.scrollIndex
                )

                put(
                    "scrollOffset",
                    position.scrollOffset
                )
            }
        )

        preferences
            .edit()
            .putString(
                KEY_POSITIONS,
                existing.toString()
            )
            .apply()
    }

    fun get(
        context: Context,
        documentUri: String
    ): ReadingPosition? {

        val preferences =
            context.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )

        val positions =
            JSONObject(
                preferences.getString(
                    KEY_POSITIONS,
                    "{}"
                ) ?: "{}"
            )

        if (
            !positions.has(
                documentUri
            )
        ) {
            return null
        }

        val value =
            positions.getJSONObject(
                documentUri
            )

        return ReadingPosition(
            documentUri =
                documentUri,

            chapterIndex =
                value.optInt(
                    "chapterIndex",
                    0
                ),

            scrollIndex =
                value.optInt(
                    "scrollIndex",
                    0
                ),

            scrollOffset =
                value.optInt(
                    "scrollOffset",
                    0
                )
        )
    }

    fun remove(
        context: Context,
        documentUri: String
    ) {

        val preferences =
            context.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )

        val positions =
            JSONObject(
                preferences.getString(
                    KEY_POSITIONS,
                    "{}"
                ) ?: "{}"
            )

        positions.remove(
            documentUri
        )

        preferences
            .edit()
            .putString(
                KEY_POSITIONS,
                positions.toString()
            )
            .apply()
    }
}