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

            val iterator =
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
                    ?: return Result.success(
                        emptyList()
                    )

            val results =
                mutableListOf<ReadiumEpubSearchResult>()

            try {

                while (true) {

                    val page =
                        iterator
                            .next()
                            .getOrElse { error ->

                                return Result.failure(
                                    IllegalStateException(
                                        error.toString()
                                    )
                                )
                            }
                            ?: break

                    page.locators.forEach { locator ->

                        val title =
                            locator
                                .text
                                ?.highlight
                                ?.takeIf {
                                    it.isNotBlank()
                                }
                                ?: locator.title
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