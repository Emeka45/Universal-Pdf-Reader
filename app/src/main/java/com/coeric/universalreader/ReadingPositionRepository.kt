package com.coeric.universalreader

import android.content.Context
import org.json.JSONObject

object ReadingPositionRepository {

    private const val PREFS =
        "universal_reader_positions"

    fun save(
        context: Context,
        documentUri: String,
        position: ReadingPosition
    ) {

        context
            .getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
            )
            .edit()
            .putString(
                key(documentUri),
                JSONObject().apply {

                    put(
                        "documentUri",
                        position.documentUri
                    )

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
                }.toString()
            )
            .apply()
    }

    fun get(
        context: Context,
        documentUri: String
    ): ReadingPosition? {

        val json =
            context
                .getSharedPreferences(
                    PREFS,
                    Context.MODE_PRIVATE
                )
                .getString(
                    key(documentUri),
                    null
                )
                ?: return null

        return try {

            val objectJson =
                JSONObject(json)

            ReadingPosition(
                documentUri =
                    objectJson.optString(
                        "documentUri",
                        documentUri
                    ),
                chapterIndex =
                    objectJson.optInt(
                        "chapterIndex",
                        0
                    ),
                scrollIndex =
                    objectJson.optInt(
                        "scrollIndex",
                        0
                    ),
                scrollOffset =
                    objectJson.optInt(
                        "scrollOffset",
                        0
                    )
            )

        } catch (
            exception: Exception
        ) {
            null
        }
    }

    fun remove(
        context: Context,
        documentUri: String
    ) {

        context
            .getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
            )
            .edit()
            .remove(
                key(documentUri)
            )
            .apply()
    }

    private fun key(
        uri: String
    ): String {

        return "position_${uri.hashCode()}"
    }
}