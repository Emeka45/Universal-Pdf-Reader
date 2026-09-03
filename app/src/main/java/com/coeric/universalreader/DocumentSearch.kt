package com.coeric.universalreader

object DocumentSearch {

    fun search(
        document: EpubDocument,
        query: String,
        maxResults: Int = 100
    ): List<SearchResult> {

        val normalizedQuery =
            query.trim().lowercase()

        if (
            normalizedQuery.isBlank()
        ) {
            return emptyList()
        }

        val results =
            mutableListOf<SearchResult>()

        document.chapters.forEachIndexed {
                index,
                chapter ->

            if (
                results.size >= maxResults
            ) {
                return@forEachIndexed
            }

            val text =
                chapter.content

            val normalizedText =
                text.lowercase()

            var searchPosition = 0

            while (
                searchPosition <
                    normalizedText.length
            ) {

                if (
                    results.size >= maxResults
                ) {
                    break
                }

                val matchIndex =
                    normalizedText.indexOf(
                        normalizedQuery,
                        searchPosition
                    )

                if (
                    matchIndex < 0
                ) {
                    break
                }

                val start =
                    (
                        matchIndex - 100
                    ).coerceAtLeast(0)

                val end =
                    (
                        matchIndex +
                            normalizedQuery.length +
                            160
                    ).coerceAtMost(
                        text.length
                    )

                val prefix =
                    if (
                        start > 0
                    ) {
                        "…"
                    } else {
                        ""
                    }

                val suffix =
                    if (
                        end < text.length
                    ) {
                        "…"
                    } else {
                        ""
                    }

                val snippet =
                    prefix +
                        text
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
                            .trim() +
                        suffix

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

                searchPosition =
                    matchIndex +
                        normalizedQuery.length
            }
        }

        return results
    }
}