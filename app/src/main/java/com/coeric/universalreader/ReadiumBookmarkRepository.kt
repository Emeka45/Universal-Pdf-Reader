package com.coeric.universalreader

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object ReadiumBookmarkRepository {

    private const val PREFS =
        "readium_bookmarks"

    private const val KEY_BOOKMARKS =
        "bookmarks"

    private fun readBookmarks(
        context: Context
    ): MutableList<ReadiumBookmark> {

        val jsonString =
            context
                .getSharedPreferences(
                    PREFS,
                    Context.MODE_PRIVATE
                )
                .getString(
                    KEY_BOOKMARKS,
                    null
                )
                ?: return mutableListOf()

        return try {

            val array =
                JSONArray(jsonString)

            val bookmarks =
                mutableListOf<ReadiumBookmark>()

            for (index in 0 until array.length()) {

                val item =
                    array.getJSONObject(index)

                val locatorJson =
                    item.getJSONObject(
                        "locator"
                    )

                val locator =
                    org.readium.r2.shared.publication.Locator
                        .fromJSON(locatorJson)
                        ?: continue

                bookmarks.add(
                    ReadiumBookmark(
                        id =
                            item.getString("id"),

                        documentUri =
                            item.getString(
                                "documentUri"
                            ),

                        locator =
                            locator,

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

            mutableListOf()
        }
    }

    private fun writeBookmarks(
        context: Context,
        bookmarks: List<ReadiumBookmark>
    ) {

        val array =
            JSONArray()

        bookmarks.forEach { bookmark ->

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
                "locator",
                bookmark.locator.toJSON()
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

        context
            .getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
            )
            .edit()
            .putString(
                KEY_BOOKMARKS,
                array.toString()
            )
            .apply()
    }

    fun getAll(
        context: Context
    ): List<ReadiumBookmark> {

        return readBookmarks(
            context
        ).sortedByDescending {
            it.createdAt
        }
    }

    fun getForDocument(
        context: Context,
        documentUri: String
    ): List<ReadiumBookmark> {

        return getAll(context)
            .filter {
                it.documentUri ==
                    documentUri
            }
    }

    fun add(
        context: Context,
        bookmark: ReadiumBookmark
    ) {

        val bookmarks =
            readBookmarks(context)

        bookmarks.removeAll {
            it.id == bookmark.id
        }

        bookmarks.add(
            bookmark
        )

        writeBookmarks(
            context,
            bookmarks
        )
    }

    fun remove(
        context: Context,
        bookmarkId: String
    ) {

        val bookmarks =
            readBookmarks(context)

        bookmarks.removeAll {
            it.id == bookmarkId
        }

        writeBookmarks(
            context,
            bookmarks
        )
    }

    fun removeForDocument(
        context: Context,
        documentUri: String
    ) {

        val bookmarks =
            readBookmarks(context)

        bookmarks.removeAll {
            it.documentUri ==
                documentUri
        }

        writeBookmarks(
            context,
            bookmarks
        )
    }
}