package com.coeric.universalreader

import android.content.Context
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

object MobiReader {

    private data class PalmRecord(
        val offset: Int,
        val length: Int
    )

    private data class MobiHeader(
        val offset: Int,
        val length: Int,
        val type: Int,
        val textEncoding: Int,
        val uniqueId: Int,
        val firstNonBookIndex: Int,
        val compression: Int
    )

    suspend fun open(
        context: Context,
        uri: Uri
    ): ReaderDocument {

        val bytes =
            context.contentResolver
                .openInputStream(uri)
                ?.use { it.readBytes() }
                ?: throw IllegalStateException(
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
                "No Palm database records found."
            )
        }

        val mobiHeader =
            findMobiHeader(
                bytes,
                records
            )
                ?: throw IllegalArgumentException(
                    "MOBI header not found."
                )

        val formatInfo =
            MobiFormatDetector.detect(
                bytes,
                mobiHeader.offset
            )

        val title =
            formatInfo.title
                ?: detectTitle(
                    bytes,
                    records,
                    mobiHeader
                )
                ?: "MOBI Book"

        val author =
            formatInfo.author

        val text =
            extractBookText(
                bytes,
                records,
                mobiHeader
            )

        if (text.isBlank()) {
            throw IllegalArgumentException(
                "No readable text was found in this MOBI file."
            )
        }

        val chapters =
            splitIntoChapters(text)

        return ReaderDocument(
            title = title,
            author = author,
            chapters = chapters
        )
    }

    private fun parsePalmRecords(
        bytes: ByteArray
    ): List<PalmRecord> {

        if (bytes.size < 78) {
            return emptyList()
        }

        val recordCount =
            readUInt16(
                bytes,
                76
            )

        if (
            recordCount <= 0 ||
            recordCount > 100000
        ) {
            return emptyList()
        }

        val recordListStart = 78

        if (
            recordListStart +
            recordCount * 8 >
            bytes.size
        ) {
            return emptyList()
        }

        val records =
            mutableListOf<PalmRecord>()

        for (
            index in 0 until recordCount
        ) {

            val entry =
                recordListStart +
                    index * 8

            val offset =
                readUInt32(
                    bytes,
                    entry
                )

            val nextOffset =
                if (
                    index + 1 <
                    recordCount
                ) {

                    readUInt32(
                        bytes,
                        entry + 8
                    )

                } else {

                    bytes.size
                }

            if (
                offset < 0 ||
                offset >= bytes.size
            ) {
                continue
            }

            val end =
                when {

                    nextOffset > offset &&
                    nextOffset <= bytes.size ->
                        nextOffset

                    else ->
                        bytes.size
                }

            records.add(
                PalmRecord(
                    offset = offset,
                    length = end - offset
                )
            )
        }

        return records
    }

    private fun findMobiHeader(
        bytes: ByteArray,
        records: List<PalmRecord>
    ): MobiHeader? {

        for (record in records) {

            if (record.length < 20) {
                continue
            }

            val start =
                record.offset

            val end =
                record.offset +
                    record.length -
                    4

            for (
                position in start..end
            ) {

                if (
                    bytes[position] == 'M'.code.toByte() &&
                    bytes[position + 1] == 'O'.code.toByte() &&
                    bytes[position + 2] == 'B'.code.toByte() &&
                    bytes[position + 3] == 'I'.code.toByte()
                ) {

                    if (
                        position + 68 >
                        bytes.size
                    ) {
                        continue
                    }

                    return parseMobiHeader(
                        bytes,
                        position
                    )
                }
            }
        }

        return null
    }

    private fun parseMobiHeader(
        bytes: ByteArray,
        mobiOffset: Int
    ): MobiHeader {

        val length =
            readUInt32(
                bytes,
                mobiOffset + 4
            )

        val type =
            readUInt32(
                bytes,
                mobiOffset + 8
            )

        val textEncoding =
            readUInt32(
                bytes,
                mobiOffset + 28
            )

        val uniqueId =
            readUInt32(
                bytes,
                mobiOffset + 32
            )

        val firstNonBookIndex =
            readUInt32(
                bytes,
                mobiOffset + 80
            )

        val compression =
            if (
                mobiOffset >= 16
            ) {

                readUInt16(
                    bytes,
                    mobiOffset - 16
                )

            } else {

                0
            }

        return MobiHeader(
            offset = mobiOffset,
            length = length,
            type = type,
            textEncoding = textEncoding,
            uniqueId = uniqueId,
            firstNonBookIndex = firstNonBookIndex,
            compression = compression
        )
    }

    private fun extractBookText(
        bytes: ByteArray,
        records: List<PalmRecord>,
        mobiHeader: MobiHeader
    ): String {

        if (records.size <= 1) {
            return ""
        }

        val output =
            ByteArrayOutputStream()

        val firstTextRecord =
            1

        val lastTextRecord =
            when {

                mobiHeader.firstNonBookIndex > 0 &&
                mobiHeader.firstNonBookIndex <= records.size ->
                    mobiHeader.firstNonBookIndex - 1

                else ->
                    minOf(
                        records.size - 1,
                        firstTextRecord + 5000
                    )
            }

        for (
            index in firstTextRecord..lastTextRecord
        ) {

            if (
                index >= records.size
            ) {
                break
            }

            val record =
                records[index]

            if (
                record.offset < 0 ||
                record.offset >= bytes.size
            ) {
                continue
            }

            val end =
                minOf(
                    bytes.size,
                    record.offset +
                        record.length
                )

            if (
                end <= record.offset
            ) {
                continue
            }

            val recordBytes =
                bytes.copyOfRange(
                    record.offset,
                    end
                )

            val decoded =
                when (
                    mobiHeader.compression
                ) {

                    2 ->
                        PalmDocDecompressor
                            .decompress(
                                recordBytes
                            )

                    else ->
                        recordBytes
                }

            output.write(
                decoded
            )
        }

        return decodeText(
            output.toByteArray(),
            mobiHeader.textEncoding
        )
            .replace("\u0000", "")
            .trim()
    }

    private fun detectTitle(
        bytes: ByteArray,
        records: List<PalmRecord>,
        mobiHeader: MobiHeader
    ): String? {

        if (
            records.isEmpty()
        ) {
            return null
        }

        val firstRecord =
            records.firstOrNull()
                ?: return null

        val start =
            firstRecord.offset

        val end =
            minOf(
                bytes.size,
                start + firstRecord.length
            )

        if (
            end <= start
        ) {
            return null
        }

        val sample =
            bytes.copyOfRange(
                start,
                end
            )

        val text =
            decodeText(
                sample,
                mobiHeader.textEncoding
            )

        return text
            .lineSequence()
            .map { it.trim() }
            .firstOrNull {
                it.length in 2..200 &&
                    it.none {
                        char ->
                        char.code < 32 &&
                            char != '\t'
                    }
            }
    }

    private fun splitIntoChapters(
        text: String
    ): List<ReaderChapter> {

        val cleanText =
            text
                .replace("\r\n", "\n")
                .replace("\r", "\n")
                .trim()

        if (cleanText.isBlank()) {
            return emptyList()
        }

        val paragraphs =
            cleanText
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
                    content = cleanText
                )
            )
        }

        val chapters =
            mutableListOf<ReaderChapter>()

        val builder =
            StringBuilder()

        var chapterNumber =
            1

        for (paragraph in paragraphs) {

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

            if (builder.isNotEmpty()) {
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

    private fun decodeText(
        bytes: ByteArray,
        encoding: Int
    ): String {

        val charset =
            when (encoding) {

                65001 ->
                    StandardCharsets.UTF_8

                1252 ->
                    Charset.forName(
                        "windows-1252"
                    )

                0 ->
                    Charset.forName(
                        "windows-1252"
                    )

                else ->
                    Charset.forName(
                        "windows-1252"
                    )
            }

        return try {

            String(
                bytes,
                charset
            )

        } catch (
            _: Exception
        ) {

            String(
                bytes,
                StandardCharsets.UTF_8
            )
        }
    }

    private fun readUInt16(
        bytes: ByteArray,
        offset: Int
    ): Int {

        if (
            offset < 0 ||
            offset + 2 >
            bytes.size
        ) {
            return 0
        }

        return (
            ((bytes[offset].toInt() and 0xFF) shl 8) or
            (bytes[offset + 1].toInt() and 0xFF)
        )
    }

    private fun readUInt32(
        bytes: ByteArray,
        offset: Int
    ): Int {

        if (
            offset < 0 ||
            offset + 4 >
            bytes.size
        ) {
            return 0
        }

        return (
            ((bytes[offset].toInt() and 0xFF) shl 24) or
            ((bytes[offset + 1].toInt() and 0xFF) shl 16) or
            ((bytes[offset + 2].toInt() and 0xFF) shl 8) or
            (bytes[offset + 3].toInt() and 0xFF)
        )
    }
}