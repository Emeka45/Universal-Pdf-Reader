package com.coeric.universalreader

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object ComicReadingPositionRepository {

    private const val PREFS =
        "universal_reader_comic_positions"

    private const val POSITIONS =
        "positions"

    fun save(
        context: Context,
        position: ComicReadingPosition
    ) {

        val all =
            getAll(context)
                .toMutableList()

        val existingIndex =
            all.indexOfFirst {
                it.documentUri ==
                    position.documentUri
            }

        if (existingIndex >= 0) {

            all[existingIndex] =
                position

        } else {

            all.add(position)
        }

        write(
            context,
            all
        )
    }

    fun get(
        context: Context,
        documentUri: String
    ): ComicReadingPosition? {

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

        write(
            context,
            getAll(context)
                .filter {
                    it.documentUri !=
                        documentUri
                }
        )
    }

    private fun getAll(
        context: Context
    ): List<ComicReadingPosition> {

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
                mutableListOf<ComicReadingPosition>()

            for (
                index in 0 until array.length()
            ) {

                val item =
                    array.getJSONObject(index)

                result.add(
                    ComicReadingPosition(
                        documentUri =
                            item.optString(
                                "documentUri"
                            ),

                        pageIndex =
                            item.optInt(
                                "pageIndex",
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

    private fun write(
        context: Context,
        positions: List<ComicReadingPosition>
    ) {

        val array =
            JSONArray()

        for (
            position in positions
        ) {

            val item =
                JSONObject()

            item.put(
                "documentUri",
                position.documentUri
            )

            item.put(
                "pageIndex",
                position.pageIndex
            )

            array.put(item)
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
}