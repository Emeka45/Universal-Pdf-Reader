package com.coeric.universalreader

import android.content.Context
import android.net.Uri
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

object DocReader {

    suspend fun open(
        context: Context,
        uri: Uri
    ): ReaderDocument {

        val input =
            context.contentResolver
                .openInputStream(uri)
                ?: throw IllegalStateException(
                    "Unable to open DOC file."
                )

        val bytes =
            input.use {
                it.readBytes()
            }

        if (bytes.isEmpty()) {
            throw IllegalArgumentException(
                "The DOC file is empty."
            )
        }

        if (!isCompoundDocument(bytes)) {
            throw IllegalArgumentException(
                "This file is not a valid legacy Microsoft Word DOC document."
            )
        }

        val text =
            extractReadableText(bytes)

        if (text.isBlank()) {
            throw IllegalArgumentException(
                "No readable text was found in this DOC file."
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

    private fun isCompoundDocument(
        bytes: ByteArray
    ): Boolean {

        if (bytes.size < 8) {
            return false
        }

        val signature =
            byteArrayOf(
                0xD0.toByte(),
                0xCF.toByte(),
                0x11.toByte(),
                0xE0.toByte(),
                0xA1.toByte(),
                0xB1.toByte(),
                0x1A.toByte(),
                0xE1.toByte()
            )

        for (index in signature.indices) {

            if (
                bytes[index] !=
                signature[index]
            ) {
                return false
            }
        }

        return true
    }

    private fun extractReadableText(
        bytes: ByteArray
    ): String {

        val candidates =
            mutableListOf<String>()

        candidates.add(
            extractUtf16Text(
                bytes,
                littleEndian = true
            )
        )

        candidates.add(
            extractUtf16Text(
                bytes,
                littleEndian = false
            )
        )

        candidates.add(
            extractSingleByteText(
                bytes,
                StandardCharsets.UTF_8
            )
        )

        candidates.add(
            extractSingleByteText(
                bytes,
                Charset.forName(
                    "windows-1252"
                )
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

    private fun extractUtf16Text(
        bytes: ByteArray,
        littleEndian: Boolean
    ): String {

        val output =
            StringBuilder()

        var position = 0

        while (
            position + 1 <
            bytes.size
        ) {

            val first =
                bytes[position]
                    .toInt() and 0xFF

            val second =
                bytes[position + 1]
                    .toInt() and 0xFF

            val value =
                if (littleEndian) {
                    first or
                        (second shl 8)
                } else {
                    (first shl 8) or
                        second
                }

            if (
                value == 0x0009 ||
                value == 0x000A ||
                value == 0x000D ||
                value in 0x0020..0x007E ||
                value in 0x00A0..0x00FF
            ) {

                output.append(
                    value.toChar()
                )

                position += 2

            } else {

                position++
            }
        }

        return output.toString()
    }

    private fun extractSingleByteText(
        bytes: ByteArray,
        charset: Charset
    ): String {

        return try {

            val raw =
                String(
                    bytes,
                    charset
                )

            raw.filter { character ->

                character == '\n' ||
                    character == '\r' ||
                    character == '\t' ||
                    character.isLetterOrDigit() ||
                    character.isWhitespace() ||
                    ".,!?;:'\"-()[]{}&/%".contains(
                        character
                    )
            }

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
                "\u0000",
                ""
            )
            .replace(
                Regex(
                    "[\\u0001-\\u0008\\u000B\\u000C\\u000E-\\u001F]"
                ),
                ""
            )
            .replace(
                Regex(
                    "[ \\t]+"
                ),
                " "
            )
            .replace(
                Regex(
                    "\\n[ \\t]+"
                ),
                "\n"
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

        var score = 0

        for (character in text) {

            when {

                character.isLetterOrDigit() ->
                    score += 3

                character.isWhitespace() ->
                    score += 1

                ".,!?;:'\"-()".contains(
                    character
                ) ->
                    score += 1
            }
        }

        return score
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
                it.length in 2..150 &&
                    it.count {
                        character ->
                        character.isLetter()
                    } >= 2
            }
            ?: "DOC Book"
    }

    private fun createChapters(
        text: String
    ): List<ReaderChapter> {

        val paragraphs =
            text
                .replace(
                    "\r\n",
                    "\n"
                )
                .replace(
                    "\r",
                    "\n"
                )
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