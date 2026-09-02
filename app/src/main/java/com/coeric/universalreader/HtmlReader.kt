package com.coeric.universalreader

import android.content.Context
import android.net.Uri
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

object HtmlReader {

    suspend fun open(
        context: Context,
        uri: Uri
    ): ReaderDocument {

        val input =
            context.contentResolver
                .openInputStream(uri)
                ?: throw IllegalStateException(
                    "Unable to open HTML file."
                )

        val bytes =
            input.use {
                it.readBytes()
            }

        if (bytes.isEmpty()) {
            throw IllegalArgumentException(
                "The HTML file is empty."
            )
        }

        val html =
            decodeHtml(bytes)

        val title =
            extractTitle(html)

        val text =
            convertHtmlToPlainText(html)

        if (text.isBlank()) {
            throw IllegalArgumentException(
                "No readable text was found in this HTML file."
            )
        }

        return ReaderDocument(
            title =
                title ?: "HTML Document",
            author = null,
            chapters =
                createChapters(text)
        )
    }

    private fun decodeHtml(
        bytes: ByteArray
    ): String {

        val utf8 =
            String(
                bytes,
                StandardCharsets.UTF_8
            )

        if (
            utf8.contains(
                "<html",
                ignoreCase = true
            ) ||
            utf8.contains(
                "<!doctype",
                ignoreCase = true
            )
        ) {
            return utf8
        }

        return try {
            String(
                bytes,
                Charset.forName(
                    "windows-1252"
                )
            )
        } catch (
            _: Exception
        ) {
            utf8
        }
    }

    private fun extractTitle(
        html: String
    ): String? {

        val match =
            Regex(
                "<title[^>]*>([\\s\\S]*?)</title>",
                RegexOption.IGNORE_CASE
            ).find(html)

        return match
            ?.groupValues
            ?.getOrNull(1)
            ?.let {
                decodeEntities(
                    it
                )
            }
            ?.replace(
                Regex("\\s+"),
                " "
            )
            ?.trim()
            ?.takeIf {
                it.isNotBlank()
            }
    }

    private fun convertHtmlToPlainText(
        html: String
    ): String {

        var text = html

        // Remove scripts and styles.
        text =
            text.replace(
                Regex(
                    "<script[\\s\\S]*?</script>",
                    RegexOption.IGNORE_CASE
                ),
                ""
            )

        text =
            text.replace(
                Regex(
                    "<style[\\s\\S]*?</style>",
                    RegexOption.IGNORE_CASE
                ),
                ""
            )

        // Convert common block elements to paragraphs.
        text =
            text.replace(
                Regex(
                    "<br\\s*/?>",
                    RegexOption.IGNORE_CASE
                ),
                "\n"
            )

        text =
            text.replace(
                Regex(
                    "</p>",
                    RegexOption.IGNORE_CASE
                ),
                "\n\n"
            )

        text =
            text.replace(
                Regex(
                    "</div>",
                    RegexOption.IGNORE_CASE
                ),
                "\n\n"
            )

        text =
            text.replace(
                Regex(
                    "</section>",
                    RegexOption.IGNORE_CASE
                ),
                "\n\n"
            )

        text =
            text.replace(
                Regex(
                    "</article>",
                    RegexOption.IGNORE_CASE
                ),
                "\n\n"
            )

        text =
            text.replace(
                Regex(
                    "</li>",
                    RegexOption.IGNORE_CASE
                ),
                "\n"
            )

        text =
            text.replace(
                Regex(
                    "</h[1-6]>",
                    RegexOption.IGNORE_CASE
                ),
                "\n\n"
            )

        // Turn list items into readable bullets.
        text =
            text.replace(
                Regex(
                    "<li[^>]*>",
                    RegexOption.IGNORE_CASE
                ),
                "• "
            )

        // Remove remaining HTML tags.
        text =
            text.replace(
                Regex("<[^>]+>"),
                " "
            )

        // Decode common HTML entities.
        text =
            decodeEntities(
                text
            )

        // Normalize whitespace while
        // preserving paragraph breaks.
        text =
            text.replace(
                "\r\n",
                "\n"
            )

        text =
            text.replace(
                "\r",
                "\n"
            )

        text =
            text.replace(
                Regex("[ \\t]+"),
                " "
            )

        text =
            text.replace(
                Regex("[ ]*\\n[ ]*"),
                "\n"
            )

        text =
            text.replace(
                Regex("\\n{3,}"),
                "\n\n"
            )

        return text.trim()
    }

    private fun decodeEntities(
        input: String
    ): String {

        return input
            .replace(
                "&nbsp;",
                " "
            )
            .replace(
                "&amp;",
                "&"
            )
            .replace(
                "&lt;",
                "<"
            )
            .replace(
                "&gt;",
                ">"
            )
            .replace(
                "&quot;",
                "\""
            )
            .replace(
                "&#39;",
                "'"
            )
            .replace(
                "&apos;",
                "'"
            )
            .replace(
                "&#8217;",
                "’"
            )
            .replace(
                "&#8216;",
                "‘"
            )
            .replace(
                "&#8220;",
                "“"
            )
            .replace(
                "&#8221;",
                "”"
            )
            .replace(
                "&#8211;",
                "–"
            )
            .replace(
                "&#8212;",
                "—"
            )
            .replace(
                Regex(
                    "&#(\\d+);"
                )
            ) {
                try {
                    it.groupValues[1]
                        .toInt()
                        .toChar()
                        .toString()
                } catch (
                    _: Exception
                ) {
                    it.value
                }
            }
            .replace(
                Regex(
                    "&#x([0-9a-fA-F]+);"
                )
            ) {
                try {
                    it.groupValues[1]
                        .toInt(16)
                        .toChar()
                        .toString()
                } catch (
                    _: Exception
                ) {
                    it.value
                }
            }
    }

    private fun createChapters(
        text: String
    ): List<ReaderChapter> {

        val paragraphs =
            text
                .split(
                    Regex(
                        "\\n\\s*\\n"
                    )
                )
                .map {
                    it.trim()
                }
                .filter {
                    it.isNotBlank()
                }

        if (paragraphs.isEmpty()) {

            return listOf(
                ReaderChapter(
                    title = "Chapter 1",
                    content = text
                )
            )
        }

        val chapters =
            mutableListOf<ReaderChapter>()

        val builder =
            StringBuilder()

        var chapterNumber =
            1

        for (
            paragraph in paragraphs
        ) {

            if (
                builder.isNotEmpty() &&
                builder.length +
                paragraph.length >
                12000
            ) {

                chapters.add(
                    ReaderChapter(
                        title =
                            "Chapter $chapterNumber",
                        content =
                            builder
                                .toString()
                                .trim()
                    )
                )

                chapterNumber++

                builder.clear()
            }

            if (
                builder.isNotEmpty()
            ) {

                builder.append(
                    "\n\n"
                )
            }

            builder.append(
                paragraph
            )
        }

        if (
            builder.isNotEmpty()
        ) {

            chapters.add(
                ReaderChapter(
                    title =
                        "Chapter $chapterNumber",
                    content =
                        builder
                            .toString()
                            .trim()
                )
            )
        }

        return chapters
    }
}