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

            /*
             * MOBI header:
             *
             * +00  "MOBI"
             * +04  Header length
             * +08  MOBI type
             * +12  Text encoding
             *
             * Because the MOBI header starts with the
             * 4-byte "MOBI" identifier, the MOBI type
             * is at offset +16 from the beginning of
             * the MOBI identifier.
             *
             * In the existing reader code we use the
             * absolute MOBI-header offset, so the type
             * field is read at +16.
             */
            val mobiType =
                readUInt32(
                    bytes,
                    mobiHeaderOffset + 16
                )

            /*
             * MOBI type 2 = Mobipocket book / PalmDOC
             * MOBI type 8 = KF8
             */
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

            /*
             * EXTH 503 = title
             * EXTH 100 = author
             */
            title =
                exthRecords[503]
                    ?.takeIf {
                        it.isNotBlank()
                    }

            author =
                exthRecords[100]
                    ?.takeIf {
                        it.isNotBlank()
                    }
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

        if (
            mobiHeaderOffset < 0 ||
            mobiHeaderOffset + 36 >
            bytes.size
        ) {
            return -1
        }

        /*
         * The EXTH flag is stored in the MOBI header.
         *
         * MOBI header + 0x80 (128) contains flags in
         * the full MOBI header. However, older/smaller
         * headers may not contain the complete field.
         *
         * For compatibility with the files currently
         * supported by Universal Reader, perform a
         * bounded search rather than scanning the whole
         * file.
         */
        val start =
            mobiHeaderOffset

        val end =
            minOf(
                bytes.size - 4,
                mobiHeaderOffset + 4096
            )

        if (end < start) {
            return -1
        }

        for (
            position in start..end
        ) {

            if (
                bytes[position] ==
                'E'.code.toByte() &&
                bytes[position + 1] ==
                'X'.code.toByte() &&
                bytes[position + 2] ==
                'T'.code.toByte() &&
                bytes[position + 3] ==
                'H'.code.toByte()
            ) {

                /*
                 * Validate the EXTH header before accepting
                 * the location.
                 *
                 * EXTH:
                 * +00 "EXTH"
                 * +04 header length
                 * +08 record count
                 */
                if (
                    position + 12 <=
                    bytes.size
                ) {

                    val headerLength =
                        readUInt32(
                            bytes,
                            position + 4
                        )

                    val recordCount =
                        readUInt32(
                            bytes,
                            position + 8
                        )

                    if (
                        headerLength >= 12 &&
                        recordCount >= 0 &&
                        position + headerLength <=
                        bytes.size
                    ) {
                        return position
                    }
                }
            }
        }

        return -1
    }

    private fun parseExthRecords(
        bytes: ByteArray,
        offset: Int
    ): Map<Int, String> {

        if (
            offset < 0 ||
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

        /*
         * Protect against malformed files containing
         * an unreasonable record count.
         */
        if (
            recordCount <= 0 ||
            recordCount > 10_000
        ) {
            return emptyMap()
        }

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
                !result.containsKey(type) &&
                value.isNotBlank()
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

        if (bytes.isEmpty()) {
            return ""
        }

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