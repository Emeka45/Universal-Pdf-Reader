package com.coeric.universalreader

import android.content.Context
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset

object MobiReader {

    suspend fun open(
        context: Context,
        uri: Uri
    ): ReaderDocument {

        val bytes =
            context.contentResolver
                .openInputStream(uri)
                ?.use { it.readBytes() }
                ?: throw IllegalArgumentException(
                    "Unable to open MOBI file."
                )

        if (bytes.size < 78) {
            throw IllegalArgumentException(
                "Invalid or incomplete MOBI file."
            )
        }

        val records =
            parsePalmRecords(bytes)

        if (records.isEmpty()) {
            throw IllegalArgumentException(
                "No readable records found in MOBI file."
            )
        }

        val mobiHeader =
            findMobiHeader(
                records
            )

        val formatInfo =
            MobiFormatDetector.detect(
                bytes,
                mobiHeader?.offset ?: -1
            )

        val title =
            formatInfo.title
                ?: getDocumentName(
                    context,
                    uri
                )

        val author =
            formatInfo.author

        val textRecords =
            extractTextRecords(
                records,
                mobiHeader
            )

        if (textRecords.isEmpty()) {
            throw IllegalArgumentException(
                "No readable text was found in this MOBI file."
            )
        }

        val text =
            decodeText(
                textRecords,
                mobiHeader?.textEncoding ?: 65001
            )

        val chapters =
            createChapters(text)

        return ReaderDocument(
            title = title,
            author = author,
            chapters = chapters
        )
    }

    private fun parsePalmRecords(
        bytes: ByteArray
    ): List<ByteArray> {

        if (bytes.size < 78) {
            return emptyList()
        }

        val recordCount =
            readUInt16(
                bytes,
                76
            )

        if (recordCount <= 0) {
            return emptyList()
        }

        val tableStart = 78

        val maximumRecords =
            minOf(
                recordCount,
                (bytes.size - tableStart) / 8
            )

        if (maximumRecords <= 0) {
            return emptyList()
        }

        val offsets =
            mutableListOf<Int>()

        for (index in 0 until maximumRecords) {

            val entry =
                tableStart +
                    index * 8

            if (entry + 4 > bytes.size) {
                break
            }

            val offset =
                readUInt32(
                    bytes,
                    entry
                )

            if (
                offset < 0 ||
                offset >= bytes.size
            ) {
                break
            }

            offsets.add(offset)
        }

        val records =
            mutableListOf<ByteArray>()

        for (index in offsets.indices) {

            val start =
                offsets[index]

            val end =
                if (
                    index + 1 <
                    offsets.size
                ) {
                    offsets[index + 1]
                } else {
                    bytes.size
                }

            if (
                start < 0 ||
                end <= start ||
                end > bytes.size
            ) {
                continue
            }

            records.add(
                bytes.copyOfRange(
                    start,
                    end
                )
            )
        }

        return records
    }

    private fun findMobiHeader(
        records: List<ByteArray>
    ): MobiHeader? {

        for (
            record in records
        ) {

            val index =
                indexOfAscii(
                    record,
                    "MOBI"
                )

            if (
                index >= 0 &&
                index + 8 <= record.size
            ) {

                val headerLength =
                    readUInt32(
                        record,
                        index + 4
                    )

                if (
                    headerLength >= 232 &&
                    index + headerLength <=
                    record.size
                ) {

                    val type =
                        readUInt32(
                            record,
                            index + 16
                        )

                    val textEncoding =
                        readUInt32(
                            record,
                            index + 28
                        )

                    val uniqueId =
                        readUInt32(
                            record,
                            index + 32
                        )

                    val firstNonBookIndex =
                        readUInt32(
                            record,
                            index + 80
                        )

                    val compression =
                        if (
                            index >= 16
                        ) {
                            readUInt16(
                                record,
                                index - 16
                            )
                        } else {
                            0
                        }

                    return MobiHeader(
                        offset = index,
                        length = headerLength,
                        type = type,
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
            }
        }

        return null
    }

    private fun extractTextRecords(
        records: List<ByteArray>,
        header: MobiHeader?
    ): List<ByteArray> {

        if (records.size <= 1) {
            return emptyList()
        }

        val startIndex = 1

        val endIndex =
            if (
                header != null &&
                header.firstNonBookIndex > 0
            ) {

                minOf(
                    records.size,
                    header.firstNonBookIndex
                )

            } else {
                records.size
            }

        val result =
            mutableListOf<ByteArray>()

        for (
            index in startIndex until endIndex
        ) {

            val record =
                records[index]

            if (record.isEmpty()) {
                continue
            }

            val data =
                if (
                    header != null &&
                    header.compression == 2
                ) {

                    PalmDocDecompressor.decompress(
                        record
                    )

                } else {

                    record
                }

            if (data.isNotEmpty()) {
                result.add(data)
            }
        }

        return result
    }

    private fun decodeText(
        records: List<ByteArray>,
        encoding: Int
    ): String {

        val output =
            ByteArrayOutputStream()

        for (
            record in records
        ) {

            output.write(
                record
            )

            output.write(
                '\n'.code
            )
        }

        val bytes =
            output.toByteArray()

        return when (encoding) {

            65001 -> {
                decodeSafely(
                    bytes,
                    Charsets.UTF_8
                )
            }

            1252,
            1250,
            1251 -> {
                decodeSafely(
                    bytes,
                    Charset.forName(
                        "windows-$encoding"
                    )
                )
            }

            else -> {
                decodeSafely(
                    bytes,
                    Charsets.UTF_8
                )
            }
        }
    }

    private fun decodeSafely(
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

            String(
                bytes,
                Charsets.UTF_8
            )
        }
    }

    private fun createChapters(
        text: String
    ): List<ReaderChapter> {

        val cleaned =
            cleanMobiText(
                text
            )

        if (cleaned.isBlank()) {
            return emptyList()
        }

        val chapterSize =
            12_000

        val chapters =
            mutableListOf<ReaderChapter>()

        var position = 0
        var chapterNumber = 1

        while (
            position < cleaned.length
        ) {

            val end =
                minOf(
                    position + chapterSize,
                    cleaned.length
                )

            var actualEnd = end

            if (
                end < cleaned.length
            ) {

                val newline =
                    cleaned.lastIndexOf(
                        '\n',
                        end
                    )

                if (
                    newline > position + 2_000
                ) {
                    actualEnd =
                        newline
                }
            }

            val chapterText =
                cleaned.substring(
                    position,
                    actualEnd
                ).trim()

            if (chapterText.isNotBlank()) {

                chapters.add(
                    ReaderChapter(
                        title =
                            "Chapter $chapterNumber",
                        content =
                            chapterText
                    )
                )

                chapterNumber++
            }

            position =
                if (actualEnd <= position) {
                    end
                } else {
                    actualEnd
                }
        }

        return chapters
    }

    private fun cleanMobiText(
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

    private fun getDocumentName(
        context: Context,
        uri: Uri
    ): String {

        var name =
            "MOBI Document"

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

        return name
            .substringBeforeLast(
                '.',
                name
            )
    }

    private fun indexOfAscii(
        bytes: ByteArray,
        value: String
    ): Int {

        val target =
            value.toByteArray(
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

    private fun readUInt16(
        bytes: ByteArray,
        offset: Int
    ): Int {

        if (
            offset < 0 ||
            offset + 2 > bytes.size
        ) {
            return 0
        }

        return (
            (bytes[offset].toInt() and 0xFF) shl 8
        ) or
            (
                bytes[offset + 1].toInt() and 0xFF
            )
    }

    private fun readUInt32(
        bytes: ByteArray,
        offset: Int
    ): Int {

        if (
            offset < 0 ||
            offset + 4 > bytes.size
        ) {
            return 0
        }

        return (
            (bytes[offset].toInt() and 0xFF) shl 24
        ) or
            (
                (bytes[offset + 1].toInt() and 0xFF) shl 16
            ) or
            (
                (bytes[offset + 2].toInt() and 0xFF) shl 8
            ) or
            (
                bytes[offset + 3].toInt() and 0xFF
            )
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
}