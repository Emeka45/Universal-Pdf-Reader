package com.coeric.universalreader

import java.io.ByteArrayOutputStream

object PalmDocDecompressor {

    fun decompress(
        input: ByteArray
    ): ByteArray {

        val output =
            ByteArrayOutputStream(
                input.size * 2
            )

        var position = 0

        while (
            position < input.size
        ) {

            val value =
                input[position]
                    .toInt() and 0xFF

            position++

            when {

                /*
                 * 0 is a literal null byte.
                 */
                value == 0 -> {
                    output.write(0)
                }

                /*
                 * Values 1..8 mean that the next
                 * 1..8 bytes are literal bytes.
                 */
                value in 1..8 -> {

                    repeat(value) {

                        if (
                            position <
                            input.size
                        ) {

                            output.write(
                                input[position]
                                    .toInt()
                            )

                            position++
                        }
                    }
                }

                /*
                 * Values 9..127 are literal ASCII
                 * characters.
                 */
                value in 9..127 -> {

                    output.write(
                        value
                    )
                }

                /*
                 * Values 128..191 encode a
                 * two-byte back-reference.
                 */
                value in 128..191 -> {

                    if (
                        position >=
                        input.size
                    ) {
                        break
                    }

                    val next =
                        input[position]
                            .toInt() and 0xFF

                    position++

                    val distance =
                        (
                            (
                                value and 0x3F
                            ) shl 5
                        ) or
                            (
                                next shr 3
                            )

                    val length =
                        (
                            next and 0x07
                        ) + 3

                    copyBackReference(
                        output,
                        distance,
                        length
                    )
                }

                /*
                 * Values 192..255 represent a
                 * space followed by the character
                 * with the high bit removed.
                 */
                else -> {

                    output.write(
                        ' '.code
                    )

                    output.write(
                        value xor 0x80
                    )
                }
            }
        }

        return output.toByteArray()
    }

    private fun copyBackReference(
        output: ByteArrayOutputStream,
        distance: Int,
        length: Int
    ) {

        if (
            distance <= 0 ||
            length <= 0
        ) {
            return
        }

        /*
         * ByteArrayOutputStream exposes its
         * current buffer through toByteArray(),
         * but repeatedly creating copies is
         * expensive.
         *
         * Since PalmDOC back-references may
         * overlap the bytes currently being
         * copied, we copy one byte at a time
         * while maintaining a single snapshot
         * of the output and updating it locally.
         */
        val existing =
            output.toByteArray()

        var currentSize =
            existing.size

        repeat(length) {

            val source =
                currentSize - distance

            if (
                source < 0 ||
                source >= currentSize
            ) {
                return
            }

            val value =
                if (
                    source < existing.size
                ) {
                    existing[source]
                } else {
                    /*
                     * This branch handles overlapping
                     * back-references by reading from
                     * the bytes already generated in
                     * this operation.
                     */
                    return
                }

            output.write(
                value.toInt()
            )

            currentSize++
        }
    }
}