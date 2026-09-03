package com.coeric.universalreader

import android.content.Context
import android.net.Uri
import java.nio.charset.Charset

object Kf8Reader {

    suspend fun open(
        context: Context,
        uri: Uri
    ): ReaderDocument {

        val bytes =
            context.contentResolver
                .openInputStream(uri)
                ?.use { it.readBytes() }
                ?: throw IllegalArgumentException(
                    "Unable to open AZW/KF8 file."
                )

        if (bytes.isEmpty()) {
            throw IllegalArgumentException(
                "The AZW/KF8 file is empty."
            )
        }

        val mobiHeaderOffset =
            findMobiHeaderOffset(
                bytes
            )

        val formatInfo =
            MobiFormatDetector.detect(
                bytes,
                mobiHeaderOffset
            )

        val rawText =
            extractReadableText(
                bytes
            )

        if (rawText.isBlank()) {
            throw IllegalArgumentException(
                "No readable text was found in this AZW/KF8 file."
            )
        }

        val cleanedText =
            cleanText(
                rawText
            )

        val chapters =
            createChapters(
                cleanedText
            )

        if (chapters.isEmpty()) {
            throw IllegalArgumentException(
                "Unable to create readable chapters from this AZW/KF8 file."
            )
        }

        return ReaderDocument(
            title =
                formatInfo.title
                    ?: getDocumentName(
                        context,
                        uri
                    ),

            author =
                formatInfo.author,

            chapters =
                chapters
        )
    }

    private fun findMobiHeaderOffset(
        bytes: ByteArray
    ): Int {

        val target =
            "MOBI".toByteArray(
                Charsets.US_ASCII
            )

        if (
            target.isEmpty() ||
            target.size > bytes.size
        ) {
            return -1
        }

        outer@ for (
            index in 0..bytes.size - target.size
        ) {

            for (
                offset in target.indices
            ) {

                if (
                    bytes[index + offset] !=
                    target[offset]
                ) {
                    continue@outer
                }
            }

            return index
        }

        return -1
    }

    private fun extractReadableText(
        bytes: ByteArray
    ): String {

        val candidates =
            mutableListOf<String>()

        candidates.add(
            decode(
                bytes,
                Charsets.UTF_8
            )
        )

        candidates.add(
            decode(
                bytes,
                Charset.forName(
                    "windows-1252"
                )
            )
        )

        val best =
            candidates.maxByOrNull {
                readabilityScore(it)
            }
                ?: ""

        return stripHtml(
            best
        )
    }

    private fun decode(
        bytes: ByteArray,
        charset: Charset
    ): String {

        return try {

            String(
                bytes,
                charset
            )

        } catch (
            exception: Exception
        ) {

            ""
        }
    }

    private fun readabilityScore(
        text: String
    ): Int {

        if (text.isBlank()) {
            return 0
        }

        var score = 0

        for (character in text) {

            when {

                character == '\n' -> {
                    score += 2
                }

                character == ' ' -> {
                    score += 1
                }

                character.isLetterOrDigit() -> {
                    score += 3
                }

                character == '<' ||
                character == '>' -> {
                    score -= 1
                }

                character.code in 0..31 &&
                character != '\n' &&
                character != '\r' &&
                character != '\t' -> {
                    score -= 5
                }
            }
        }

        return score
    }

    private fun stripHtml(
        text: String
    ): String {

        return text
            .replace(
                Regex(
                    "(?is)<script.*?>.*?</script>"
                ),
                ""
            )
            .replace(
                Regex(
                    "(?is)<style.*?>.*?</style>"
                ),
                ""
            )
            .replace(
                Regex(
                    "(?i)<br\\s*/?>"
                ),
                "\n"
            )
            .replace(
                Regex(
                    "(?i)</p>"
                ),
                "\n\n"
            )
            .replace(
                Regex(
                    "(?i)</div>"
                ),
                "\n\n"
            )
            .replace(
                Regex(
                    "(?i)</h[1-6]>"
                ),
                "\n\n"
            )
            .replace(
                Regex(
                    "<[^>]+>"
                ),
                ""
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
                "&quot;",
                "\""
            )
            .replace(
                "&#39;",
                "'"
            )
    }

    private fun cleanText(
        text: String
    ): String {

        return text
            .replace(
                "\u0000",
                ""
            )
            .replace(
                "\r\n",
                "\n"
            )
            .replace(
                '\r',
                '\n'
            )
            .replace(
                Regex(
                    "[ \\t]+"
                ),
                " "
            )
            .replace(
                Regex(
                    "\n{3,}"
                ),
                "\n\n"
            )
            .trim()
    }

    private fun createChapters(
        text: String
    ): List<ReaderChapter> {

        if (text.isBlank()) {
            return emptyList()
        }

        val chapters =
            mutableListOf<ReaderChapter>()

        val detectedSections =
            detectSections(
                text
            )

        if (
            detectedSections.size >= 2
        ) {

            for (
                index in detectedSections.indices
            ) {

                val start =
                    detectedSections[index].first

                val end =
                    if (
                        index + 1 <
                        detectedSections.size
                    ) {
                        detectedSections[
                            index + 1
                        ].first
                    } else {
                        text.length
                    }

                val title =
                    detectedSections[index].second

                val content =
                    text.substring(
                        start,
                        end
                    ).trim()

                if (content.isNotBlank()) {

                    chapters.add(
                        ReaderChapter(
                            title =
                                title.ifBlank {
                                    "Chapter ${index + 1}"
                                },

                            content =
                                content
                        )
                    )
                }
            }

            if (chapters.isNotEmpty()) {
                return chapters
            }
        }

        val chapterSize =
            12_000

        var position = 0
        var chapterNumber = 1

        while (
            position < text.length
        ) {

            val targetEnd =
                minOf(
                    position + chapterSize,
                    text.length
                )

            var end =
                targetEnd

            if (
                targetEnd < text.length
            ) {

                val paragraphBreak =
                    text.lastIndexOf(
                        "\n\n",
                        targetEnd
                    )

                if (
                    paragraphBreak >
                    position + 2_000
                ) {

                    end =
                        paragraphBreak
                }
            }

            val content =
                text.substring(
                    position,
                    end
                ).trim()

            if (content.isNotBlank()) {

                chapters.add(
                    ReaderChapter(
                        title =
                            "Chapter $chapterNumber",

                        content =
                            content
                    )
                )

                chapterNumber++
            }

            position =
                if (end <= position) {
                    targetEnd
                } else {
                    end
                }
        }

        return chapters
    }

    private fun detectSections(
        text: String
    ): List<Pair<Int, String>> {

        val result =
            mutableListOf<Pair<Int, String>>()

        val lines =
            text.split('\n')

        var position = 0

        for (line in lines) {

            val trimmed =
                line.trim()

            if (
                looksLikeChapterHeading(
                    trimmed
                )
            ) {

                result.add(
                    position to trimmed
                )
            }

            position +=
                line.length + 1
        }

        return result
    }

    private fun looksLikeChapterHeading(
        line: String
    ): Boolean {

        if (line.length !in 3..100) {
            return false
        }

        val lower =
            line.lowercase()

        if (
            lower.startsWith("chapter ") ||
            lower.matches(
                Regex(
                    "chapter\\s+[0-9ivxlcdm]+.*"
                )
            )
        ) {
            return true
        }

        if (
            lower.startsWith("part ") ||
            lower.matches(
                Regex(
                    "part\\s+[0-9ivxlcdm]+.*"
                )
            )
        ) {
            return true
        }

        if (
            lower.matches(
                Regex(
                    "(prologue|epilogue|introduction|preface|afterword)"
                )
            )
        ) {
            return true
        }

        val words =
            line.split(
                Regex("\\s+")
            )

        if (
            words.size in 1..8 &&
            line.length <= 60
        ) {

            val uppercaseLetters =
                line.count {
                    it.isUpperCase()
                }

            val letters =
                line.count {
                    it.isLetter()
                }

            if (
                letters > 0 &&
                uppercaseLetters.toFloat() /
                letters.toFloat() >= 0.65f
            ) {
                return true
            }
        }

        return false
    }

    private fun getDocumentName(
        context: Context,
        uri: Uri
    ): String {

        var name =
            "AZW/KF8 Document"

        context.contentResolver
            .query(
                uri,
                arrayOf(
                    android.provider.OpenableColumns.DISPLAY_NAME
                ),
                null,
                null,
                null
            )
            ?.use { cursor ->

                if (cursor.moveToFirst()) {

                    val index =
                        cursor.getColumnIndex(
                            android.provider.OpenableColumns.DISPLAY_NAME
                        )

                    if (index >= 0) {

                        name =
                            cursor.getString(
                                index
                            )
                    }
                }
            }

        return name.substringBeforeLast(
            '.',
            name
        )
    }
}