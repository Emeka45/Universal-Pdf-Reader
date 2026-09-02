package com.coeric.universalreader

object DocumentSearch {

    fun searchEpub(
        document: EpubDocument,
        query: String
    ): List<SearchResult> {

        if (query.isBlank()) {
            return emptyList()
        }

        val searchQuery =
            query.trim()

        val results =
            mutableListOf<SearchResult>()

        document.chapters.forEachIndexed { index, chapter ->

            val content =
                chapter.content

            val lowerContent =
                content.lowercase()

            val lowerQuery =
                searchQuery.lowercase()

            var position =
                lowerContent.indexOf(
                    lowerQuery
                )

            while (position >= 0) {

                val start =
                    (position - 80)
                        .coerceAtLeast(0)

                val end =
                    (position +
                        searchQuery.length +
                        120)
                        .coerceAtMost(
                            content.length
                        )

                val snippet =
                    content
                        .substring(
                            start,
                            end
                        )
                        .replace(
                            Regex(
                                "\\s+"
                            ),
                            " "
                        )
                        .trim()

                results.add(
                    SearchResult(
                        chapterIndex =
                            index,
                        chapterTitle =
                            chapter.title,
                        matchingText =
                            snippet
                    )
                )

                if (
                    results.size >= 100
                ) {
                    return results
                }

                position =
                    lowerContent.indexOf(
                        lowerQuery,
                        position +
                            searchQuery.length
                    )
            }
        }

        return results
    }
}