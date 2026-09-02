package com.coeric.universalreader

import android.content.Context
import android.net.Uri
import java.nio.charset.StandardCharsets

object Kf8Reader {

    suspend fun open(
        context: Context,
        uri: Uri
    ): ReaderDocument {

        val bytes =
            context.contentResolver
                .openInputStream(uri)
                ?.use { it.readBytes() }
                ?: throw IllegalStateException(
                    "Unable to open KF8/AZW3 file."
                )

        if (bytes.isEmpty()) {
            throw IllegalArgumentException(
                "The KF8/AZW3 file is empty."
            )
        }

        val text =
            extractReadableText(bytes)

        if (text.isBlank()) {
            throw IllegalArgumentException(
                "No readable text was found in this KF8/AZW3 file."
            )
        }

        val title =
            detectTitle(text)
                ?: "KF8 Book"

        return ReaderDocument(
            title = title,
            author = null,
            chapters = createChapters(text)
        )
    }

    private fun extractReadableText(
        bytes: ByteArray
    ): String {

        val candidates =
            mutableListOf<String>()

        candidates.add(
            decode(
                bytes,
                StandardCharsets.UTF_8
            )
        )

        candidates.add(
            decode(
                bytes,
                CharsetFallback.windows1252()
            )
        )

        return candidates
            .map {
                cleanText(it)
            }
            .maxByOrNull {
                readableScore(it)
            }
            ?: ""
    }

    private fun decode(
        bytes: ByteArray,
        charset: java.nio.charset.Charset
    ): String {

        return try {

            String(
                bytes,
                charset
            )

        } catch (
            _: Exception
        ) {

            ""
        }
    }

    private fun cleanText(
        input: String
    ): String {

        return input
            .replace(
                Regex(
                    "<script[\\s\\S]*?</script>",
                    RegexOption.IGNORE_CASE
                ),
                ""
            )
            .replace(
                Regex(
                    "<style[\\s\\S]*?</style>",
                    RegexOption.IGNORE_CASE
                ),
                ""
            )
            .replace(
                Regex(
                    "<[^>]+>"
                ),
                " "
            )
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
                Regex(
                    "[ \\t]+"
                ),
                " "
            )
            .replace(
                Regex(
                    "\\n{3,}"
                ),
                "\n\n"
            )
            .trim()
    }

    private fun readableScore(
        text: String
    ): Int {

        if (text.isBlank()) {
            return 0
        }

        var score =
            0

        for (character in text) {

            if (
                character.isLetterOrDigit() ||
                character.isWhitespace() ||
                ".,!?;:'\"-()".contains(
                    character
                )
            ) {
                score++
            }
        }

        return score
    }

    private fun detectTitle(
        text: String
    ): String? {

        return text
            .lineSequence()
            .map {
                it.trim()
            }
            .firstOrNull {
                it.length in 2..150 &&
                    it.count {
                        character ->
                        character.isLetter()
                    } >= 2
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

    private object CharsetFallback {

        fun windows1252():
            java.nio.charset.Charset {

            return java.nio.charset.Charset.forName(
                "windows-1252"
            )
        }
    }
}