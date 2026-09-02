package com.coeric.universalreader

import java.nio.charset.StandardCharsets

object MobiFormatDetector {

    fun detect(
        bytes: ByteArray,
        mobiHeaderOffset: Int
    ): MobiFormatInfo {

        var title: String? = null
        var author: String? = null
        var hasExth = false
        var isKf8 = false
        var isPalmDoc = false

        if (
            mobiHeaderOffset >= 0 &&
            mobiHeaderOffset + 68 <= bytes.size
        ) {

            val mobiType =
                readUInt32(
                    bytes,
                    mobiHeaderOffset + 24
                )

            isKf8 =
                mobiType == 0x00000008

            isPalmDoc =
                mobiType == 0x00000002
        }

        val exthOffset =
            findExthHeader(
                bytes,
                mobiHeaderOffset
            )

        if (exthOffset >= 0) {

            hasExth = true

            val exthRecords =
                parseExthRecords(
                    bytes,
                    exthOffset
                )

            title =
                exthRecords[503]

            author =
                exthRecords[100]
        }

        return MobiFormatInfo(
            isKf8 = isKf8,
            isPalmDoc = isPalmDoc,
            hasExth = hasExth,
            title = title,
            author = author
        )
    }

    private fun findExthHeader(
        bytes: ByteArray,
        mobiHeaderOffset: Int
    ): Int {

        if (mobiHeaderOffset < 0) {
            return -1
        }

        val start =
            mobiHeaderOffset

        val end =
            minOf(
                bytes.size - 4,
                mobiHeaderOffset + 4096
            )

        for (
            position in start..end
        ) {

            if (
                bytes[position] == 'E'.code.toByte() &&
                bytes[position + 1] == 'X'.code.toByte() &&
                bytes[position + 2] == 'T'.code.toByte() &&
                bytes[position + 3] == 'H'.code.toByte()
            ) {

                return position
            }
        }

        return -1
    }

    private fun parseExthRecords(
        bytes: ByteArray,
        offset: Int
    ): Map<Int, String> {

        if (
            offset + 12 >
            bytes.size
        ) {
            return emptyMap()
        }

        val recordCount =
            readUInt32(
                bytes,
                offset + 8
            )

        val result =
            mutableMapOf<Int, String>()

        var position =
            offset + 12

        repeat(recordCount) {

            if (
                position + 8 >
                bytes.size
            ) {
                return@repeat
            }

            val type =
                readUInt32(
                    bytes,
                    position
                )

            val length =
                readUInt32(
                    bytes,
                    position + 4
                )

            if (
                length < 8 ||
                position + length >
                bytes.size
            ) {
                return@repeat
            }

            val valueStart =
                position + 8

            val valueLength =
                length - 8

            val valueBytes =
                bytes.copyOfRange(
                    valueStart,
                    valueStart + valueLength
                )

            val value =
                decodeText(
                    valueBytes
                )

            if (
                !result.containsKey(type)
            ) {
                result[type] = value
            }

            position += length
        }

        return result
    }

    private fun decodeText(
        bytes: ByteArray
    ): String {

        return try {

            String(
                bytes,
                StandardCharsets.UTF_8
            ).trim()

        } catch (
            _: Exception
        ) {

            String(
                bytes,
                Charsets.ISO_8859_1
            ).trim()
        }
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