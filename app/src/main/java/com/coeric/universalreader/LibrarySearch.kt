package com.coeric.universalreader

object LibrarySearch {

    fun filter(
        books: List<LibraryBook>,
        query: String
    ): List<LibraryBook> {

        val cleanQuery =
            query.trim()

        if (cleanQuery.isBlank()) {
            return books
        }

        return books.filter { book ->

            book.name.contains(
                cleanQuery,
                ignoreCase = true
            ) ||
                book.extension.contains(
                    cleanQuery,
                    ignoreCase = true
                ) ||
                (
                    book.mimeType
                        ?.contains(
                            cleanQuery,
                            ignoreCase = true
                        )
                        == true
                )
        }
    }

    fun favorites(
        books: List<LibraryBook>
    ): List<LibraryBook> {

        return books.filter {
            it.isFavorite
        }
    }
}