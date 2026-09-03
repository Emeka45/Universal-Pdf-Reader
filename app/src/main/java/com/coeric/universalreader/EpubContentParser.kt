package com.coeric.universalreader

object EpubContentParser {

    private val imagePattern =
        Regex(
            """(?is)<img\b[^>]*?\bsrc\s*=\s*["']([^"']+)["'][^>]*>"""
        )

    private val blockImagePattern =
        Regex(
            """(?is)<image\b[^>]*?\bxlink:href\s*=\s*["']([^"']+)["'][^>]*>"""
        )

    fun parse(
        html: String
    ): List<EpubContentBlock> {

        if (
            html.isBlank()
        ) {
            return emptyList()
        }

        val blocks =
            mutableListOf<EpubContentBlock>()

        val matches =
            (
                imagePattern.findAll(html) +
                    blockImagePattern.findAll(html)
            ).sortedBy {
                it.range.first
            }

        if (
            matches.isEmpty()
        ) {

            return listOf(
                EpubContentBlock.Text(
                    html
                )
            )
        }

        var position =
            0

        for (
            match in matches
        ) {

            if (
                match.range.first > position
            ) {

                val text =
                    html.substring(
                        position,
                        match.range.first
                    )

                if (
                    text.isNotBlank()
                ) {

                    blocks.add(
                        EpubContentBlock.Text(
                            text
                        )
                    )
                }
            }

            val imagePath =
                match.groupValues
                    .getOrNull(1)
                    ?.trim()

            if (
                !imagePath.isNullOrBlank()
            ) {

                blocks.add(
                    EpubContentBlock.Image(
                        path =
                            imagePath
                    )
                )
            }

            position =
                match.range.last + 1
        }

        if (
            position < html.length
        ) {

            val remaining =
                html.substring(
                    position
                )

            if (
                remaining.isNotBlank()
            ) {

                blocks.add(
                    EpubContentBlock.Text(
                        remaining
                    )
                )
            }
        }

        return blocks
    }
}