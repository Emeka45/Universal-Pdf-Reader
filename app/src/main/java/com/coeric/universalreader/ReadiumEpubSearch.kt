package com.coeric.universalreader

import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.search.SearchService

object ReadiumEpubSearch {

    suspend fun search(
        publication: Publication,
        query: String
    ): List<ReadiumEpubSearchResult> {

        val trimmedQuery =
            query.trim()

        if (trimmedQuery.isEmpty()) {
            return emptyList()
        }

        if (!publication.isSearchable) {
            return emptyList()
        }

        val iterator =
            publication
                .search(
                    query = trimmedQuery,
                    options =
                        SearchService.Options()
                )
                ?: return emptyList()

        val results =
            mutableListOf<ReadiumEpubSearchResult>()

        try {

            iterator.forEach { collection ->

                collection.forEach { locator ->

                    val title =
                        locator
                            .title
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

        } finally {

            iterator.close()
        }

        return results
    }
}