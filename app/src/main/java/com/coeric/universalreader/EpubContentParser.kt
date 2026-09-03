package com.coeric.universalreader

object EpubContentParser {

    private val imagePattern =
        Regex(
            """(?is)<img\b([^>]*?)\bsrc\s*=\s*["']([^"']+)["']([^>]*)>"""
        )

    private val xhtmlImagePattern =
        Regex(
            """(?is)<image\b([^>]*?)\bxlink:href\s*=\s*["']([^"']+)["']([^>]*)>"""
        )

    fun parse(
        html: String
    ): List<EpubContentBlock> {

        if (html.isBlank()) {
            return emptyList()
        }

        val matches =
            mutableListOf<Pair<Int, EpubContentBlock>>()

        imagePattern.findAll(html).forEach { match ->

            val before =
                match.groupValues.getOrNull(1)
                    ?: ""

            val src =
                match.groupValues.getOrNull(2)
                    ?.trim()

            val after =
                match.groupValues.getOrNull(3)
                    ?: ""

            if (!src.isNullOrBlank()) {

                matches.add(
                    match.range.first to
                        EpubContentBlock.Image(
                            path = src,
                            altText =
                                extractAlt(
                                    before + after
                                )
                        )
                )
            }
        }

        xhtmlImagePattern.findAll(html).forEach { match ->

            val before =
                match.groupValues.getOrNull(1)
                    ?: ""

            val href =
                match.groupValues.getOrNull(2)
                    ?.trim()

            val after =
                match.groupValues.getOrNull(3)
                    ?: ""

            if (!href.isNullOrBlank()) {

                matches.add(
                    match.range.first to
                        EpubContentBlock.Image(
                            path = href,
                            altText =
                                extractAlt(
                                    before + after
                                )
                        )
                )
            }
        }

        if (matches.isEmpty()) {
            return listOf(
                EpubContentBlock.Text(html)
            )
        }

        matches.sortBy {
            it.first
        }

        val blocks =
            mutableListOf<EpubContentBlock>()

        var position = 0

        for ((start, block) in matches) {

            if (start > position) {

                val text =
                    html.substring(
                        position,
                        start
                    )

                if (text.isNotBlank()) {

                    blocks.add(
                        EpubContentBlock.Text(
                            text
                        )
                    )
                }
            }

            blocks.add(block)

            val match =
                when (block) {
                    is EpubContentBlock.Image ->
                        findImageEnd(
                            html,
                            start
                        )

                    is EpubContentBlock.Text ->
                        start
                }

            position =
                match.coerceAtLeast(
                    position
                )
        }

        if (position < html.length) {

            val remaining =
                html.substring(position)

            if (remaining.isNotBlank()) {

                blocks.add(
                    EpubContentBlock.Text(
                        remaining
                    )
                )
            }
        }

        return blocks
    }

    private fun findImageEnd(
        html: String,
        start: Int
    ): Int {

        val end =
            html.indexOf(
                ">",
                start
            )

        return if (end >= 0) {
            end + 1
        } else {
            html.length
        }
    }

    private fun extractAlt(
        attributes: String
    ): String? {

        return Regex(
            """(?is)\balt\s*=\s*["']([^"']*)["']"""
        )
            .find(attributes)
            ?.groupValues
            ?.getOrNull(1)
            ?.trim()
            ?.takeIf {
                it.isNotBlank()
            }
    }
}