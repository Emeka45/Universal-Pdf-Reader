package com.coeric.universalreader

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object BookmarkRepository {

    private const val PREFS =
        "universal_reader_bookmarks"

    private const val BOOKMARKS =
        "bookmarks"

    fun getBookmarks(
        context: Context,
        documentUri: String
    ): List<Bookmark> {

        val json =
            context.getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
            ).getString(
                BOOKMARKS,
                null
            )
                ?: return emptyList()

        return try {

            val array =
                JSONArray(json)

            val result =
                mutableListOf<Bookmark>()

            for (
                index in 0 until array.length()
            ) {

                val item =
                    array.getJSONObject(index)

                val bookmark =
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
                                0L
                            )
                    )

                if (
                    bookmark.documentUri ==
                    documentUri
                ) {
                    result.add(bookmark)
                }
            }

            result.sortedByDescending {
                it.createdAt
            }

        } catch (
            exception: Exception
        ) {

            emptyList()
        }
    }

    fun addBookmark(
        context: Context,
        documentUri: String,
        chapterIndex: Int,
        title: String
    ): Bookmark {

        val allBookmarks =
            getAllBookmarks(context)
                .toMutableList()

        val bookmark =
            Bookmark(
                id =
                    UUID.randomUUID()
                        .toString(),
                documentUri =
                    documentUri,
                chapterIndex =
                    chapterIndex,
                title =
                    title
            )

        allBookmarks.add(bookmark)

        save(
            context,
            allBookmarks
        )

        return bookmark
    }

    fun removeBookmark(
        context: Context,
        bookmarkId: String
    ) {

        val remaining =
            getAllBookmarks(context)
                .filter {
                    it.id != bookmarkId
                }

        save(
            context,
            remaining
        )
    }

    private fun getAllBookmarks(
        context: Context
    ): List<Bookmark> {

        val json =
            context.getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
            ).getString(
                BOOKMARKS,
                null
            )
                ?: return emptyList()

        return try {

            val array =
                JSONArray(json)

            val result =
                mutableListOf<Bookmark>()

            for (
                index in 0 until array.length()
            ) {

                val item =
                    array.getJSONObject(index)

                result.add(
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
                                "title"
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
        bookmarks: List<Bookmark>
    ) {

        val array =
            JSONArray()

        for (
            bookmark in bookmarks
        ) {

            val item =
                JSONObject()

            item.put(
                "id",
                bookmark.id
            )

            item.put(
                "documentUri",
                bookmark.documentUri
            )

            item.put(
                "chapterIndex",
                bookmark.chapterIndex
            )

            item.put(
                "title",
                bookmark.title
            )

            item.put(
                "note",
                bookmark.note
            )

            item.put(
                "createdAt",
                bookmark.createdAt
            )

            array.put(item)
        }

        context.getSharedPreferences(
            PREFS,
            Context.MODE_PRIVATE
        )
            .edit()
            .putString(
                BOOKMARKS,
                array.toString()
            )
            .apply()
    }
}