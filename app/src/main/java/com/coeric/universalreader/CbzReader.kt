package com.coeric.universalreader

import android.content.Context
import android.net.Uri
import java.util.zip.ZipInputStream

data class ComicPage(
    val name: String,
    val data: ByteArray
)

object CbzReader {

    suspend fun open(
        context: Context,
        uri: Uri
    ): List<ComicPage> {

        val input =
            context.contentResolver
                .openInputStream(uri)
                ?: throw IllegalStateException(
                    "Unable to open CBZ file."
                )

        val pages =
            mutableListOf<ComicPage>()

        input.use { stream ->

            ZipInputStream(stream).use { zip ->

                while (true) {

                    val entry =
                        zip.nextEntry
                            ?: break

                    if (
                        entry.isDirectory
                    ) {
                        continue
                    }

                    val name =
                        entry.name

                    if (
                        isImageFile(name)
                    ) {

                        val data =
                            zip.readBytes()

                        if (
                            data.isNotEmpty()
                        ) {

                            pages.add(
                                ComicPage(
                                    name = name,
                                    data = data
                                )
                            )
                        }
                    }

                    zip.closeEntry()
                }
            }
        }

        pages.sortWith(
            compareBy {
                naturalSortKey(it.name)
            }
        )

        if (
            pages.isEmpty()
        ) {

            throw IllegalArgumentException(
                "No comic-book images were found in this CBZ file."
            )
        }

        return pages
    }

    private fun isImageFile(
        name: String
    ): Boolean {

        return when (
            name
                .substringAfterLast(
                    '.',
                    ""
                )
                .lowercase()
        ) {

            "jpg",
            "jpeg",
            "png",
            "webp",
            "gif",
            "bmp" -> true

            else -> false
        }
    }

    private fun naturalSortKey(
        value: String
    ): List<Any> {

        val result =
            mutableListOf<Any>()

        val parts =
            Regex(
                "(\\d+|\\D+)"
            )
                .findAll(value.lowercase())
                .map {
                    it.value
                }

        for (part in parts) {

            if (
                part.all {
                    it.isDigit()
                }
            ) {

                result.add(
                    part.toLongOrNull()
                        ?: 0L
                )

            } else {

                result.add(
                    part
                )
            }
        }

        return result
    }
}