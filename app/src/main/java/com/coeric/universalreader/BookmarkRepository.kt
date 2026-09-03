package com.coeric.universalreader

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object BookmarkRepository {

    private const val PREFS_NAME =
        "universal_reader_bookmarks"

    private const val KEY_BOOKMARKS =
        "bookmarks"

    fun getAll(
        context: Context
    ): List<Bookmark> {

        val preferences =
            context.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )

        val json =
            preferences.getString(
                KEY_BOOKMARKS,
                "[]"
            ) ?: "[]"

        return try {

            val array =
                JSONArray(json)

            val bookmarks =
                mutableListOf<Bookmark>()

            for (
                index in 0 until array.length()
            ) {

                val item =
                    array.getJSONObject(index)

                bookmarks.add(
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
                                "chapterIndex",
                                0
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
                                "createdAt",
                                System.currentTimeMillis()
                            )
                    )
                )
            }

            bookmarks

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

        val bookmarks =
            getAll(context)
                .toMutableList()

        bookmarks.removeAll {
            it.id == bookmark.id
        }

        bookmarks.add(bookmark)

        saveAll(
            context,
            bookmarks
        )
    }

    fun remove(
        context: Context,
        bookmarkId: String
    ) {

        val bookmarks =
            getAll(context)
                .filter {
                    it.id != bookmarkId
                }

        saveAll(
            context,
            bookmarks
        )
    }

    fun removeForDocument(
        context: Context,
        documentUri: String
    ) {

        val bookmarks =
            getAll(context)
                .filter {
                    it.documentUri != documentUri
                }

        saveAll(
            context,
            bookmarks
        )
    }

    fun clear(
        context: Context
    ) {

        saveAll(
            context,
            emptyList()
        )
    }

    private fun saveAll(
        context: Context,
        bookmarks: List<Bookmark>
    ) {

        val array =
            JSONArray()

        for (
            bookmark in bookmarks
        ) {

            val objectJson =
                JSONObject()

            objectJson.put(
                "id",
                bookmark.id
            )

            objectJson.put(
                "documentUri",
                bookmark.documentUri
            )

            objectJson.put(
                "chapterIndex",
                bookmark.chapterIndex
            )

            objectJson.put(
                "title",
                bookmark.title
            )

            objectJson.put(
                "note",
                bookmark.note
            )

            objectJson.put(
                "createdAt",
                bookmark.createdAt
            )

            array.put(
                objectJson
            )
        }

        context
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
            .edit()
            .putString(
                KEY_BOOKMARKS,
                array.toString()
            )
            .apply()
    }
}