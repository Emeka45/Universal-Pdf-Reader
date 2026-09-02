package com.coeric.universalreader

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object ReadingPositionRepository {

    private const val PREFS =
        "universal_reader_positions"

    private const val POSITIONS =
        "positions"

    fun save(
        context: Context,
        position: ReadingPosition
    ) {

        val allPositions =
            getAll(context)
                .toMutableList()

        val existingIndex =
            allPositions.indexOfFirst {
                it.documentUri ==
                    position.documentUri
            }

        if (
            existingIndex >= 0
        ) {

            allPositions[existingIndex] =
                position

        } else {

            allPositions.add(
                position
            )
        }

        val array =
            JSONArray()

        for (
            item in allPositions
        ) {

            val json =
                JSONObject()

            json.put(
                "documentUri",
                item.documentUri
            )

            json.put(
                "chapterIndex",
                item.chapterIndex
            )

            json.put(
                "scrollIndex",
                item.scrollIndex
            )

            json.put(
                "scrollOffset",
                item.scrollOffset
            )

            array.put(json)
        }

        context
            .getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
            )
            .edit()
            .putString(
                POSITIONS,
                array.toString()
            )
            .apply()
    }

    fun get(
        context: Context,
        documentUri: String
    ): ReadingPosition? {

        return getAll(context)
            .firstOrNull {
                it.documentUri ==
                    documentUri
            }
    }

    fun remove(
        context: Context,
        documentUri: String
    ) {

        val remaining =
            getAll(context)
                .filter {
                    it.documentUri !=
                        documentUri
                }

        val array =
            JSONArray()

        for (
            item in remaining
        ) {

            val json =
                JSONObject()

            json.put(
                "documentUri",
                item.documentUri
            )

            json.put(
                "chapterIndex",
                item.chapterIndex
            )

            json.put(
                "scrollIndex",
                item.scrollIndex
            )

            json.put(
                "scrollOffset",
                item.scrollOffset
            )

            array.put(json)
        }

        context
            .getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
            )
            .edit()
            .putString(
                POSITIONS,
                array.toString()
            )
            .apply()
    }

    private fun getAll(
        context: Context
    ): List<ReadingPosition> {

        val json =
            context
                .getSharedPreferences(
                    PREFS,
                    Context.MODE_PRIVATE
                )
                .getString(
                    POSITIONS,
                    null
                )
                ?: return emptyList()

        return try {

            val array =
                JSONArray(json)

            val result =
                mutableListOf<ReadingPosition>()

            for (
                index in 0 until array.length()
            ) {

                val item =
                    array.getJSONObject(index)

                result.add(

                    ReadingPosition(

                        documentUri =
                            item.optString(
                                "documentUri"
                            ),

                        chapterIndex =
                            item.optInt(
                                "chapterIndex",
                                0
                            ),

                        scrollIndex =
                            item.optInt(
                                "scrollIndex",
                                0
                            ),

                        scrollOffset =
                            item.optInt(
                                "scrollOffset",
                                0
                            )
                    )
                )
            }

            result

        } catch (
            exception: Exception
        ) {

            emptyList()
        }
    }
}