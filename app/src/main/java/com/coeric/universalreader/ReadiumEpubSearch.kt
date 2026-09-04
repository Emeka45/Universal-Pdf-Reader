package com.coeric.universalreader

import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.search.SearchService
import org.readium.r2.shared.publication.services.search.search

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

                    val searchResult =
                        iterator.next()

                    val collection =
                        searchResult.fold(
                            onSuccess = {
                                it
                            },
                            onFailure = {
                                return Result.failure(
                                    IllegalStateException(
                                        it.toString()
                                    )
                                )
                            }
                        )
                            ?: break

                    val locators =
                        collection.locators

                    for (
                        locator in locators
                    ) {

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
                                locator =
                                    locator,
                                title =
                                    title
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