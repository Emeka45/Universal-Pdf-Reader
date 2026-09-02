package com.coeric.universalreader

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset

object MobiReader : ReaderEngine {

    override suspend fun open(
        context: Context,
        uri: Uri
    ): ReaderDocument {

        return withContext(Dispatchers.IO) {

            val bytes =
                context.contentResolver
                    .openInputStream(uri)
                    ?.use {
                        it.readBytes()
                    }
                    ?: throw IllegalStateException(
                        "Unable to open MOBI file."
                    )

            parse(bytes)
        }
    }

    private fun parse(
        data: ByteArray
    ): ReaderDocument {

        if (data.size < 78) {
            throw IllegalStateException(
                "Invalid or incomplete MOBI file."
            )
        }

        val header =
            PalmDatabaseHeader.parse(data)

        if (header.recordCount <= 0) {
            throw IllegalStateException(
                "MOBI file contains no records."
            )
        }

        val records =
            readPalmRecords(
                data,
                header
            )

        if (records.isEmpty()) {
            throw IllegalStateException(
                "Unable to read MOBI records."
            )
        }

        val firstRecord =
            records.first()

        val mobiOffset =
            findMobiHeader(
                firstRecord
            )

        if (mobiOffset < 0) {

            throw IllegalStateException(
                "This file does not contain a readable MOBI header."
            )
        }

        val mobiHeader =
            parseMobiHeader(
                firstRecord,
                mobiOffset
            )

        val title =
            extractTitle(
                firstRecord,
                mobiOffset,
                mobiHeader
            )

        val author =
            extractAuthor(
                firstRecord,
                mobiOffset,
                mobiHeader
            )

        val text =
            extractBookText(
                records,
                mobiHeader
            )

        if (text.isBlank()) {

            throw IllegalStateException(
                "No readable text was found. The file may use an unsupported MOBI/KF8 structure or DRM."
            )
        }

        val chapters =
            splitIntoChapters(
                text
            )

        if (chapters.isEmpty()) {

            throw IllegalStateException(
                "No readable chapters were found."
            )
        }

        return ReaderDocument(
            title =
                title
                    .takeIf {
                        it.isNotBlank()
                    }
                    ?: "Untitled",

            author =
                author,

            chapters =
                chapters
        )
    }

    private fun readPalmRecords(
        data: ByteArray,
        header: PalmDatabaseHeader
    ): List<ByteArray> {

        val result =
            mutableListOf<ByteArray>()

        var offset =
            header.recordListOffset

        val recordOffsets =
            mutableListOf<Int>()

        for (
            index in 0 until header.recordCount
        ) {

            if (
                offset + 8 >
                data.size
            ) {
                break
            }

            val recordOffset =
                readUInt32(
                    data,
                    offset
                ).toInt()

            if (
                recordOffset >= 0 &&
                recordOffset < data.size
            ) {

                recordOffsets.add(
                    recordOffset
                )
            }

            offset += 8
        }

        for (
            index in recordOffsets.indices
        ) {

            val start =
                recordOffsets[index]

            val end =
                if (
                    index + 1 <
                    recordOffsets.size
                ) {

                    recordOffsets[
                        index + 1
                    ]

                } else {

                    data.size
                }

            if (
                start < 0 ||
                end <= start ||
                start >= data.size
            ) {
                continue
            }

            val safeEnd =
                end.coerceAtMost(
                    data.size
                )

            result.add(
                data.copyOfRange(
                    start,
                    safeEnd
                )
            )
        }

        return result
    }

    private fun findMobiHeader(
        record: ByteArray
    ): Int {

        val signature =
            byteArrayOf(
                'M'.code.toByte(),
                'O'.code.toByte(),
                'B'.code.toByte(),
                'I'.code.toByte()
            )

        if (
            record.size < 4
        ) {
            return -1
        }

        for (
            index in 0..record.size - 4
        ) {

            if (
                record[index] ==
                signature[0] &&

                record[index + 1] ==
                signature[1] &&

                record[index + 2] ==
                signature[2] &&

                record[index + 3] ==
                signature[3]
            ) {

                return index
            }
        }

        return -1
    }

    private fun parseMobiHeader(
        record: ByteArray,
        mobiOffset: Int
    ): MobiHeader {

        if (
            mobiOffset + 84 >
            record.size
        ) {

            throw IllegalStateException(
                "MOBI header is incomplete."
            )
        }

        val headerLength =
            readUInt32(
                record,
                mobiOffset + 4
            ).toInt()

        val type =
            readUInt32(
                record,
                mobiOffset + 8
            ).toInt()

        val textEncoding =
            readUInt32(
                record,
                mobiOffset + 28
            ).toInt()

        val uniqueId =
            readUInt32(
                record,
                mobiOffset + 32
            ).toInt()

        val firstNonBookIndex =
            readUInt32(
                record,
                mobiOffset + 80
            ).toInt()

        val compression =
            readUInt16(
                record,
                mobiOffset - 16
            )

        return MobiHeader(
            offset =
                mobiOffset,

            length =
                headerLength,

            type =
                type,

            textEncoding =
                textEncoding,

            uniqueId =
                uniqueId,

            firstNonBookIndex =
                firstNonBookIndex,

            compression =
                compression
        )
    }

    private fun extractTitle(
        record: ByteArray,
        mobiOffset: Int,
        header: MobiHeader
    ): String {

        val exth =
            findExthHeader(
                record,
                mobiOffset,
                header
            )

        if (exth != null) {

            val title =
                exth.records[503]

            if (
                !title.isNullOrBlank()
            ) {

                return decodeString(
                    title,
                    header.textEncoding
                ).trim()
            }
        }

        return "Untitled"
    }

    private fun extractAuthor(
        record: ByteArray,
        mobiOffset: Int,
        header: MobiHeader
    ): String? {

        val exth =
            findExthHeader(
                record,
                mobiOffset,
                header
            )
                ?: return null

        val author =
            exth.records[100]

        return author
            ?.let {

                decodeString(
                    it,
                    header.textEncoding
                ).trim()
            }
            ?.takeIf {
                it.isNotBlank()
            }
    }

    private fun findExthHeader(
        record: ByteArray,
        mobiOffset: Int,
        header: MobiHeader
    ): ExthHeader? {

        val signature =
            byteArrayOf(
                'E'.code.toByte(),
                'X'.code.toByte(),
                'T'.code.toByte(),
                'H'.code.toByte()
            )

        val searchStart =
            (
                mobiOffset +
                    16 +
                    header.length
                )
                .coerceAtLeast(
                    mobiOffset
                )

        if (
            searchStart >=
            record.size - 4
        ) {
            return null
        }

        for (
            index in searchStart until
                record.size - 4
        ) {

            if (
                record[index] ==
                signature[0] &&

                record[index + 1] ==
                signature[1] &&

                record[index + 2] ==
                signature[2] &&

                record[index + 3] ==
                signature[3]
            ) {

                return parseExth(
                    record,
                    index
                )
            }
        }

        return null
    }

    private fun parseExth(
        record: ByteArray,
        offset: Int
    ): ExthHeader? {

        if (
            offset + 12 >
            record.size
        ) {
            return null
        }

        val recordCount =
            readUInt32(
                record,
                offset + 8
            ).toInt()

        var position =
            offset + 12

        val values =
            mutableMapOf<Int, ByteArray>()

        repeat(
            recordCount.coerceAtMost(
                10000
            )
        ) {

            if (
                position + 8 >
                record.size
            ) {
                return@repeat
            }

            val type =
                readUInt32(
                    record,
                    position
                ).toInt()

            val length =
                readUInt32(
                    record,
                    position + 4
                ).toInt()

            if (
                length < 8 ||
                position + length >
                record.size
            ) {
                return@repeat
            }

            val value =
                record.copyOfRange(
                    position + 8,
                    position + length
                )

            values[type] =
                value

            position += length
        }

        return ExthHeader(
            records =
                values
        )
    }

    private fun extractBookText(
        records: List<ByteArray>,
        header: MobiHeader
    ): String {

        if (
            records.size <= 1
        ) {
            return ""
        }

        val textRecords =
            records.drop(1)

        val output =
            StringBuilder()

        for (
            record in textRecords
        ) {

            if (
                record.isEmpty()
            ) {
                continue
            }

            val decodedRecord =
                when (
                    header.compression
                ) {

                    1 -> {

                        record
                    }

                    2 -> {

                        PalmDocDecompressor
                            .decompress(
                                record
                            )
                    }

                    else -> {

                        record
                    }
                }

            val text =
                decodeTextRecord(
                    decodedRecord,
                    header.textEncoding
                )

            if (
                text.isNotBlank()
            ) {

                output
                    .append(
                        text
                    )
                    .append(
                        "\n"
                    )
            }
        }

        return output
            .toString()
            .trim()
    }

    private fun decodeTextRecord(
        record: ByteArray,
        encoding: Int
    ): String {

        if (
            record.isEmpty()
        ) {
            return ""
        }

        return try {

            when (encoding) {

                65001 -> {

                    String(
                        record,
                        Charsets.UTF_8
                    )
                }

                else -> {

                    String(
                        record,
                        Charset.forName(
                            "windows-1252"
                        )
                    )
                }
            }

        } catch (
            exception: Exception
        ) {

            String(
                record,
                Charsets.UTF_8
            )
        }
    }

    private fun splitIntoChapters(
        text: String
    ): List<ReaderChapter> {

        val cleanText =
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

        if (
            cleanText.isBlank()
        ) {
            return emptyList()
        }

        val paragraphs =
            cleanText
                .split(
                    Regex(
                        "\n{3,}"
                    )
                )
                .map {
                    it.trim()
                }
                .filter {
                    it.isNotBlank()
                }

        if (
            paragraphs.isEmpty()
        ) {

            return listOf(
                ReaderChapter(
                    title =
                        "Book",
                    content =
                        cleanText
                )
            )
        }

        val chapters =
            mutableListOf<ReaderChapter>()

        var chapterNumber =
            1

        val current =
            StringBuilder()

        for (
            paragraph in paragraphs
        ) {

            current
                .append(
                    paragraph
                )
                .append(
                    "\n\n"
                )

            if (
                current.length >=
                12000
            ) {

                chapters.add(
                    ReaderChapter(
                        title =
                            "Chapter $chapterNumber",

                        content =
                            current
                                .toString()
                                .trim()
                    )
                )

                current.clear()

                chapterNumber++
            }
        }

        if (
            current.isNotEmpty()
        ) {

            chapters.add(
                ReaderChapter(
                    title =
                        "Chapter $chapterNumber",

                    content =
                        current
                            .toString()
                            .trim()
                )
            )
        }

        return chapters
    }

    private fun decodeString(
        bytes: ByteArray,
        encoding: Int
    ): String {

        return try {

            if (
                encoding == 65001
            ) {

                String(
                    bytes,
                    Charsets.UTF_8
                )

            } else {

                String(
                    bytes,
                    Charset.forName(
                        "windows-1252"
                    )
                )
            }

        } catch (
            exception: Exception
        ) {

            String(
                bytes,
                Charsets.UTF_8
            )
        }
    }

    private fun readUInt16(
        data: ByteArray,
        offset: Int
    ): Int {

        if (
            offset < 0 ||
            offset + 2 >
            data.size
        ) {
            return 0
        }

        return (
            (
                data[offset]
                    .toInt() and 0xFF
            ) shl 8
        ) or
            (
                data[offset + 1]
                    .toInt() and 0xFF
            )
    }

    private fun readUInt32(
        data: ByteArray,
        offset: Int
    ): Long {

        if (
            offset < 0 ||
            offset + 4 >
            data.size
        ) {
            return 0L
        }

        return (
            (
                data[offset]
                    .toLong() and 0xFF
            ) shl 24
        ) or
            (
                (
                    data[offset + 1]
                        .toLong() and 0xFF
                ) shl 16
            ) or
            (
                (
                    data[offset + 2]
                        .toLong() and 0xFF
                ) shl 8
            ) or
            (
                data[offset + 3]
                    .toLong() and 0xFF
            )
    }

    private data class PalmDatabaseHeader(
        val recordListOffset: Int,
        val recordCount: Int
    ) {

        companion object {

            fun parse(
                data: ByteArray
            ): PalmDatabaseHeader {

                if (
                    data.size < 78
                ) {

                    throw IllegalStateException(
                        "Invalid Palm database."
                    )
                }

                val recordListOffset =
                    readUInt32Static(
                        data,
                        76
                    ).toInt()

                if (
                    recordListOffset <= 0 ||
                    recordListOffset >=
                    data.size
                ) {

                    throw IllegalStateException(
                        "Invalid MOBI record list."
                    )
                }

                val recordCountOffset =
                    recordListOffset + 8

                if (
                    recordCountOffset + 2 >
                    data.size
                ) {

                    throw IllegalStateException(
                        "Invalid MOBI record count."
                    )
                }

                val recordCount =
                    (
                        (
                            data[
                                recordCountOffset
                            ].toInt() and 0xFF
                        ) shl 8
                    ) or
                        (
                            data[
                                recordCountOffset + 1
                            ].toInt() and 0xFF
                        )

                return PalmDatabaseHeader(
                    recordListOffset =
                        recordListOffset + 8,

                    recordCount =
                        recordCount
                )
            }

            private fun readUInt32Static(
                data: ByteArray,
                offset: Int
            ): Long {

                return (
                    (
                        data[offset]
                            .toLong() and 0xFF
                    ) shl 24
                ) or
                    (
                        (
                            data[offset + 1]
                                .toLong() and 0xFF
                        ) shl 16
                    ) or
                    (
                        (
                            data[offset + 2]
                                .toLong() and 0xFF
                        ) shl 8
                    ) or
                    (
                        data[offset + 3]
                            .toLong() and 0xFF
                    )
            }
        }
    }

    private data class MobiHeader(
        val offset: Int,
        val length: Int,
        val type: Int,
        val textEncoding: Int,
        val uniqueId: Int,
        val firstNonBookIndex: Int,
        val compression: Int
    )

    private data class ExthHeader(
        val records: Map<Int, ByteArray>
    )
}