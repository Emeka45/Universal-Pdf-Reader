package com.coeric.universalreader

import java.io.ByteArrayOutputStream

object PalmDocDecompressor {

    fun decompress(
        input: ByteArray
    ): ByteArray {

        val output =
            ByteArrayOutputStream()

        var position = 0

        while (
            position < input.size
        ) {

            val value =
                input[position]
                    .toInt() and 0xFF

            position++

            when {

                value == 0 -> {

                    output.write(0)
                }

                value in 1..8 -> {

                    val count =
                        value

                    repeat(count) {

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

                value in 0x09..0x7F -> {

                    output.write(
                        value
                    )
                }

                value in 0x80..0xBF -> {

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
                            (value and 0x3F)
                                shl 5
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

                value in 0xC0..0xFF -> {

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
            distance <= 0
        ) {
            return
        }

        val current =
            output.toByteArray()

        var source =
            current.size - distance

        if (
            source < 0
        ) {
            return
        }

        repeat(length) {

            val data =
                output.toByteArray()

            if (
                source < 0 ||
                source >= data.size
            ) {
                return
            }

            output.write(
                data[source]
                    .toInt()
            )

            source++
        }
    }
}