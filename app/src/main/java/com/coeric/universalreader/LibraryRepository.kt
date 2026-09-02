package com.coeric.universalreader

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object LibraryRepository {

    private const val PREFS_NAME = "universal_reader_library"
    private const val BOOKS_KEY = "books"

    private fun preferences(
        context: Context
    ) =
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

    fun getBooks(
        context: Context
    ): List<LibraryBook> {

        val json =
            preferences(context)
                .getString(
                    BOOKS_KEY,
                    null
                )
                ?: return emptyList()

        return try {

            val array =
                JSONArray(json)

            val books =
                mutableListOf<LibraryBook>()

            for (
                index in 0 until array.length()
            ) {

                val item =
                    array.getJSONObject(index)

                books.add(
                    LibraryBook(
                        uri =
                            item.optString(
                                "uri"
                            ),
                        name =
                            item.optString(
                                "name",
                                "Unknown document"
                            ),
                        mimeType =
                            item.optString(
                                "mimeType"
                            ).takeIf {
                                it.isNotBlank()
                            },
                        extension =
                            item.optString(
                                "extension"
                            ),
                        lastOpened =
                            item.optLong(
                                "lastOpened",
                                0L
                            ),
                        isFavorite =
                            item.optBoolean(
                                "isFavorite",
                                false
                            )
                    )
                )
            }

            books.sortedByDescending {
                it.lastOpened
            }

        } catch (
            exception: Exception
        ) {

            emptyList()
        }
    }

    fun addOrUpdateBook(
        context: Context,
        document: DocumentInfo
    ) {

        val books =
            getBooks(context)
                .toMutableList()

        val existingIndex =
            books.indexOfFirst {
                it.uri == document.uri
            }

        val existingFavorite =
            if (existingIndex >= 0) {
                books[existingIndex].isFavorite
            } else {
                false
            }

        val book =
            LibraryBook(
                uri =
                    document.uri,
                name =
                    document.name,
                mimeType =
                    document.mimeType,
                extension =
                    document.extension,
                lastOpened =
                    System.currentTimeMillis(),
                isFavorite =
                    existingFavorite
            )

        if (existingIndex >= 0) {

            books[existingIndex] =
                book

        } else {

            books.add(book)
        }

        saveBooks(
            context,
            books
        )
    }

    fun toggleFavorite(
        context: Context,
        uri: String
    ) {

        val books =
            getBooks(context)
                .toMutableList()

        val index =
            books.indexOfFirst {
                it.uri == uri
            }

        if (index < 0) {
            return
        }

        val oldBook =
            books[index]

        books[index] =
            oldBook.copy(
                isFavorite =
                    !oldBook.isFavorite
            )

        saveBooks(
            context,
            books
        )
    }

    fun removeBook(
        context: Context,
        uri: String
    ) {

        val books =
            getBooks(context)
                .filter {
                    it.uri != uri
                }

        saveBooks(
            context,
            books
        )
    }

    fun getFavorites(
        context: Context
    ): List<LibraryBook> {

        return getBooks(context)
            .filter {
                it.isFavorite
            }
    }

    private fun saveBooks(
        context: Context,
        books: List<LibraryBook>
    ) {

        val array =
            JSONArray()

        for (
            book in books
        ) {

            val item =
                JSONObject()

            item.put(
                "uri",
                book.uri
            )

            item.put(
                "name",
                book.name
            )

            item.put(
                "mimeType",
                book.mimeType ?: ""
            )

            item.put(
                "extension",
                book.extension
            )

            item.put(
                "lastOpened",
                book.lastOpened
            )

            item.put(
                "isFavorite",
                book.isFavorite
            )

            array.put(item)
        }

        preferences(context)
            .edit()
            .putString(
                BOOKS_KEY,
                array.toString()
            )
            .apply()
    }
}