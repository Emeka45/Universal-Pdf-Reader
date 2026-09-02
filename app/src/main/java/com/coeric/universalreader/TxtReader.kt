package com.coeric.universalreader

import android.content.Context
import android.net.Uri
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

object TxtReader {

    suspend fun open(
        context: Context,
        uri: Uri
    ): ReaderDocument {

        val input =
            context.contentResolver
                .openInputStream(uri)
                ?: throw IllegalStateException(
                    "Unable to open TXT file."
                )

        val bytes =
            input.use {
                it.readBytes()
            }

        if (bytes.isEmpty()) {
            throw IllegalArgumentException(
                "The TXT file is empty."
            )
        }

        val text =
            decodeText(bytes)

        if (text.isBlank()) {
            throw IllegalArgumentException(
                "No readable text was found in this TXT file."
            )
        }

        return ReaderDocument(
            title =
                detectTitle(text),
            author = null,
            chapters =
                createChapters(text)
        )
    }

    private fun decodeText(
        bytes: ByteArray
    ): String {

        // UTF-8 BOM
        if (
            bytes.size >= 3 &&
            bytes[0] == 0xEF.toByte() &&
            bytes[1] == 0xBB.toByte() &&
            bytes[2] == 0xBF.toByte()
        ) {

            return String(
                bytes,
                3,
                bytes.size - 3,
                StandardCharsets.UTF_8
            )
        }

        // UTF-16 little-endian BOM
        if (
            bytes.size >= 2 &&
            bytes[0] == 0xFF.toByte() &&
            bytes[1] == 0xFE.toByte()
        ) {

            return String(
                bytes,
                2,
                bytes.size - 2,
                StandardCharsets.UTF_16LE
            )
        }

        // UTF-16 big-endian BOM
        if (
            bytes.size >= 2 &&
            bytes[0] == 0xFE.toByte() &&
            bytes[1] == 0xFF.toByte()
        ) {

            return String(
                bytes,
                2,
                bytes.size - 2,
                StandardCharsets.UTF_16BE
            )
        }

        val utf8 =
            String(
                bytes,
                StandardCharsets.UTF_8
            )

        if (
            !containsReplacementCharacters(
                utf8
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

    private fun containsReplacementCharacters(
        text: String
    ): Boolean {

        return text.count {
            it == '\uFFFD'
        } > 3
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
            ?: "Text Document"
    }

    private fun createChapters(
        text: String
    ): List<ReaderChapter> {

        val normalized =
            text
                .replace(
                    "\r\n",
                    "\n"
                )
                .replace(
                    "\r",
                    "\n"
                )
                .trim()

        if (normalized.isBlank()) {

            return emptyList()
        }

        val paragraphs =
            normalized
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
                    content = normalized
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