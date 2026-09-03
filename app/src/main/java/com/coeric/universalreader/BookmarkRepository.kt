package com.coeric.universalreader

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object BookmarkRepository {

    private const val PREFS =
        "universal_reader_bookmarks"

    private const val KEY =
        "bookmarks"

    fun getAll(
        context: Context
    ): List<Bookmark> {

        val json =
            context
                .getSharedPreferences(
                    PREFS,
                    Context.MODE_PRIVATE
                )
                .getString(
                    KEY,
                    "[]"
                )
                ?: "[]"

        return try {

            val array =
                JSONArray(json)

            buildList {

                for (
                    i in 0 until array.length()
                ) {

                    val item =
                        array.getJSONObject(i)

                    add(
                        Bookmark(
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
                            title =
                                item.optString(
                                    "title",
                                    "Bookmark"
                                ),
                            note =
                                item.optString(
                                    "note",
                                    ""
                                ),
                            createdAt =
                                item.optLong(
                                    "createdAt"
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

    fun getForDocument(
        context: Context,
        documentUri: String
    ): List<Bookmark> {

        return getAll(context)
            .filter {
                it.documentUri == documentUri
            }
            .sortedByDescending {
                it.createdAt
            }
    }

    fun add(
        context: Context,
        bookmark: Bookmark
    ) {

        val list =
            getAll(context)
                .toMutableList()

        list.removeAll {
            it.id == bookmark.id
        }

        list.add(bookmark)

        saveAll(
            context,
            list
        )
    }

    fun remove(
        context: Context,
        id: String
    ) {

        saveAll(
            context,
            getAll(context)
                .filter {
                    it.id != id
                }
        )
    }

    private fun saveAll(
        context: Context,
        bookmarks: List<Bookmark>
    ) {

        val array =
            JSONArray()

        bookmarks.forEach { bookmark ->

            array.put(
                JSONObject().apply {

                    put(
                        "id",
                        bookmark.id
                    )

                    put(
                        "documentUri",
                        bookmark.documentUri
                    )

                    put(
                        "chapterIndex",
                        bookmark.chapterIndex
                    )

                    put(
                        "title",
                        bookmark.title
                    )

                    put(
                        "note",
                        bookmark.note
                    )

                    put(
                        "createdAt",
                        bookmark.createdAt
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
                KEY,
                array.toString()
            )
            .apply()
    }
}