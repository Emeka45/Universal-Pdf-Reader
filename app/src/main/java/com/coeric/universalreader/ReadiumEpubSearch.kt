package com.coeric.universalreader

import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.search.SearchService

object ReadiumEpubSearch {

    suspend fun search(
        publication: Publication,
        query: String
    ): Result<List<ReadiumEpubSearchResult>> {

        val trimmedQuery =
            query.trim()

        if (trimmedQuery.isEmpty()) {
            return Result.success(
                emptyList()
            )
        }

        if (!publication.isSearchable) {
            return Result.failure(
                IllegalStateException(
                    "This EPUB does not support text search."
                )
            )
        }

        return try {

            val searchResult =
                publication.search(
                    query = trimmedQuery,
                    options =
                        SearchService.Options(
                            caseSensitive = false,
                            diacriticSensitive = false,
                            wholeWord = false,
                            exact = false
                        )
                )

            val iterator =
                searchResult.getOrElse { error ->
                    return Result.failure(
                        IllegalStateException(
                            error.toString()
                        )
                    )
                }

            val results =
                mutableListOf<ReadiumEpubSearchResult>()

            try {

                while (true) {

                    val pageResult =
                        iterator.next()

                    val collection =
                        pageResult.getOrElse { error ->
                            return Result.failure(
                                IllegalStateException(
                                    error.toString()
                                )
                            )
                        }
                            ?: break

                    collection.locators.forEach { locator ->

                        val title =
                            locator.title
                                ?: locator
                                    .text
                                    ?.highlight
                                    ?.takeIf {
                                        it.isNotBlank()
                                    }
                                ?: "Search result"

                        results.add(
                            ReadiumEpubSearchResult(
                                locator = locator,
                                title = title
                            )
                        )
                    }
                }

                Result.success(
                    results
                )

            } finally {

                iterator.close()
            }

        } catch (
            exception: Exception
        ) {

            Result.failure(
                exception
            )
        }
    }
}