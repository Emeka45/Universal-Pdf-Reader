package com.coeric.universalreader

object DocumentSearch {

    fun search(
        document: EpubDocument,
        query: String,
        maxResults: Int = 100
    ): List<SearchResult> {

        val q =
            query.trim().lowercase()

        if (q.isBlank()) {
            return emptyList()
        }

        val results =
            mutableListOf<SearchResult>()

        document.chapters
            .forEachIndexed { index, chapter ->

                if (
                    results.size >= maxResults
                ) {
                    return@forEachIndexed
                }

                val text =
                    chapter.content

                val lower =
                    text.lowercase()

                var position = 0

                while (
                    position < lower.length &&
                    results.size < maxResults
                ) {

                    val found =
                        lower.indexOf(
                            q,
                            position
                        )

                    if (found < 0) {
                        break
                    }

                    val start =
                        (found - 100)
                            .coerceAtLeast(0)

                    val end =
                        (
                            found +
                                q.length +
                                160
                        ).coerceAtMost(
                            text.length
                        )

                    val snippet =
                        (
                            if (start > 0) {
                                "…"
                            } else {
                                ""
                            }
                        ) +
                            text.substring(
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
                            (
                                if (
                                    end < text.length
                                ) {
                                    "…"
                                } else {
                                    ""
                                }
                            )

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

                    position =
                        found + q.length
                }
            }

        return results
    }
}