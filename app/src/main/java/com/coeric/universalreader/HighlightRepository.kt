package com.coeric.universalreader

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object HighlightRepository {

    private const val PREFS =
        "universal_reader_highlights"

    private const val HIGHLIGHTS =
        "highlights"

    fun getHighlights(
        context: Context,
        documentUri: String
    ): List<Highlight> {

        return getAllHighlights(context)
            .filter {
                it.documentUri ==
                    documentUri
            }
            .sortedByDescending {
                it.createdAt
            }
    }

    fun addHighlight(
        context: Context,
        documentUri: String,
        chapterIndex: Int,
        selectedText: String,
        note: String = ""
    ): Highlight {

        val highlights =
            getAllHighlights(context)
                .toMutableList()

        val highlight =
            Highlight(
                id =
                    UUID.randomUUID()
                        .toString(),
                documentUri =
                    documentUri,
                chapterIndex =
                    chapterIndex,
                selectedText =
                    selectedText,
                note =
                    note
            )

        highlights.add(
            highlight
        )

        save(
            context,
            highlights
        )

        return highlight
    }

    fun removeHighlight(
        context: Context,
        highlightId: String
    ) {

        val remaining =
            getAllHighlights(context)
                .filter {
                    it.id != highlightId
                }

        save(
            context,
            remaining
        )
    }

    private fun getAllHighlights(
        context: Context
    ): List<Highlight> {

        val json =
            context.getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
            ).getString(
                HIGHLIGHTS,
                null
            )
                ?: return emptyList()

        return try {

            val array =
                JSONArray(json)

            val result =
                mutableListOf<Highlight>()

            for (
                index in 0 until array.length()
            ) {

                val item =
                    array.getJSONObject(index)

                result.add(
                    Highlight(
                        id =
                            item.optString(
                                "id"
                            ),
                        documentUri =
                            item.optString(
                                "documentUri"
                            ),
                        chapterIndex =
                            item.optInt(
                                "chapterIndex"
                            ),
                        selectedText =
                            item.optString(
                                "selectedText"
                            ),
                        note =
                            item.optString(
                                "note"
                            ),
                        createdAt =
                            item.optLong(
                                "createdAt"
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

    private fun save(
        context: Context,
        highlights: List<Highlight>
    ) {

        val array =
            JSONArray()

        for (
            highlight in highlights
        ) {

            val item =
                JSONObject()

            item.put(
                "id",
                highlight.id
            )

            item.put(
                "documentUri",
                highlight.documentUri
            )

            item.put(
                "chapterIndex",
                highlight.chapterIndex
            )

            item.put(
                "selectedText",
                highlight.selectedText
            )

            item.put(
                "note",
                highlight.note
            )

            item.put(
                "createdAt",
                highlight.createdAt
            )

            array.put(item)
        }

        context.getSharedPreferences(
            PREFS,
            Context.MODE_PRIVATE
        )
            .edit()
            .putString(
                HIGHLIGHTS,
                array.toString()
            )
            .apply()
    }
}