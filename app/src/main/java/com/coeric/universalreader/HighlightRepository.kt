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
                it.documentUri == documentUri
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
                    UUID.randomUUID().toString(),

                documentUri =
                    documentUri,

                chapterIndex =
                    chapterIndex,

                selectedText =
                    selectedText.trim(),

                note =
                    note.trim()
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
            context
                .getSharedPreferences(
                    PREFS,
                    Context.MODE_PRIVATE
                )
                .getString(
                    HIGHLIGHTS,
                    null
                )
                ?: return emptyList()

        return try {

            val array =
                JSONArray(json)

            buildList {

                for (
                    index in 0 until array.length()
                ) {

                    val item =
                        array.optJSONObject(index)
                            ?: continue

                    val id =
                        item.optString(
                            "id"
                        )

                    val documentUri =
                        item.optString(
                            "documentUri"
                        )

                    val selectedText =
                        item.optString(
                            "selectedText"
                        )

                    if (
                        id.isBlank() ||
                        documentUri.isBlank() ||
                        selectedText.isBlank()
                    ) {
                        continue
                    }

                    add(
                        Highlight(
                            id = id,

                            documentUri =
                                documentUri,

                            chapterIndex =
                                item.optInt(
                                    "chapterIndex",
                                    0
                                ),

                            selectedText =
                                selectedText,

                            note =
                                item.optString(
                                    "note",
                                    ""
                                ),

                            createdAt =
                                item.optLong(
                                    "createdAt",
                                    System.currentTimeMillis()
                                )
                        )
                    )
                }
            }

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

        highlights.forEach { highlight ->

            array.put(
                JSONObject().apply {

                    put(
                        "id",
                        highlight.id
                    )

                    put(
                        "documentUri",
                        highlight.documentUri
                    )

                    put(
                        "chapterIndex",
                        highlight.chapterIndex
                    )

                    put(
                        "selectedText",
                        highlight.selectedText
                    )

                    put(
                        "note",
                        highlight.note
                    )

                    put(
                        "createdAt",
                        highlight.createdAt
                    )
                }
            )
        }

        context
            .getSharedPreferences(
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