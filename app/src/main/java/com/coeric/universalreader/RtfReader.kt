package com.coeric.universalreader

import android.content.Context
import android.net.Uri
import java.nio.charset.Charset

object RtfReader {

    suspend fun open(
        context: Context,
        uri: Uri
    ): ReaderDocument {

        val input =
            context.contentResolver
                .openInputStream(uri)
                ?: throw IllegalStateException(
                    "Unable to open RTF file."
                )

        val bytes =
            input.use {
                it.readBytes()
            }

        if (bytes.isEmpty()) {
            throw IllegalArgumentException(
                "The RTF file is empty."
            )
        }

        val rawText =
            decodeBytes(bytes)

        val text =
            convertRtfToPlainText(rawText)

        if (text.isBlank()) {
            throw IllegalArgumentException(
                "No readable text was found in this RTF file."
            )
        }

        val title =
            detectTitle(text)

        val chapters =
            createChapters(text)

        return ReaderDocument(
            title = title,
            author = null,
            chapters = chapters
        )
    }

    private fun decodeBytes(
        bytes: ByteArray
    ): String {

        return try {

            String(
                bytes,
                Charsets.UTF_8
            )

        } catch (
            _: Exception
        ) {

            String(
                bytes,
                Charset.forName(
                    "windows-1252"
                )
            )
        }
    }

    private fun convertRtfToPlainText(
        rtf: String
    ): String {

        var text =
            rtf

        text =
            text.replace(
                Regex(
                    "\\\\par\\s?"
                ),
                "\n\n"
            )

        text =
            text.replace(
                Regex(
                    "\\\\line\\s?"
                ),
                "\n"
            )

        text =
            text.replace(
                Regex(
                    "\\\\tab\\s?"
                ),
                "\t"
            )

        text =
            text.replace(
                Regex(
                    "\\\\'[0-9a-fA-F]{2}"
                )
            ) { match ->

                val hex =
                    match.value
                        .substring(2)

                try {

                    String(
                        byteArrayOf(
                            hex
                                .substring(
                                    0,
                                    2
                                )
                                .toInt(
                                    16
                                )
                                .toByte()
                        ),
                        Charset.forName(
                            "windows-1252"
                        )
                    )

                } catch (
                    _: Exception
                ) {

                    ""
                }
            }

        text =
            text.replace(
                Regex(
                    "\\\\u(-?\\d+)\\??"
                )
            ) { match ->

                try {

                    val value =
                        match.groupValues[1]
                            .toInt()

                    value
                        .and(
                            0xFFFF
                        )
                        .toChar()
                        .toString()

                } catch (
                    _: Exception
                ) {

                    ""
                }
            }

        text =
            text.replace(
                Regex(
                    "\\\\[a-zA-Z]+-?\\d* ?"
                ),
                ""
            )

        text =
            text.replace(
                Regex(
                    "[{}]"
                ),
                ""
            )

        text =
            text.replace(
                "\\~",
                " "
            )

        text =
            text.replace(
                "\\_",
                "-"
            )

        text =
            text.replace(
                "\\-",
                "-"
            )

        text =
            text.replace(
                "\\\\",
                "\\"
            )

        text =
            text.replace(
                Regex(
                    "[ \\t]+"
                ),
                " "
            )

        text =
            text.replace(
                Regex(
                    "\\n[ \\t]+"
                ),
                "\n"
            )

        text =
            text.replace(
                Regex(
                    "\\n{3,}"
                ),
                "\n\n"
            )

        return text.trim()
    }

    private fun detectTitle(
        text: String
    ): String {

        return text
            .lineSequence()
            .map {
                it.trim()
            }
            .firstOrNull {
                it.length in 2..150
            }
            ?: "RTF Book"
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