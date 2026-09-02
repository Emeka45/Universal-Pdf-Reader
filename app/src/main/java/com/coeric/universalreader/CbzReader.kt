package com.coeric.universalreader

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipFile

object CbzReader {

    suspend fun open(
        context: Context,
        uri: Uri
    ): ComicArchive {

        val temporaryFile =
            File.createTempFile(
                "universal_reader_",
                ".zip",
                context.cacheDir
            )

        try {

            copyUriToFile(
                context,
                uri,
                temporaryFile
            )

            val pages =
                mutableListOf<ComicPage>()

            ZipFile(temporaryFile).use { zip ->

                val entries =
                    zip.entries()

                while (entries.hasMoreElements()) {

                    val entry =
                        entries.nextElement()

                    if (entry.isDirectory) {
                        continue
                    }

                    val name =
                        entry.name

                    if (!isImageFile(name)) {
                        continue
                    }

                    val pageFile =
                        File.createTempFile(
                            "comic_page_",
                            getExtension(name),
                            context.cacheDir
                        )

                    try {

                        zip.getInputStream(
                            entry
                        ).use { input ->

                            FileOutputStream(
                                pageFile
                            ).use { output ->

                                input.copyTo(
                                    output,
                                    bufferSize = 64 * 1024
                                )
                            }
                        }

                        pages.add(
                            ComicPage(
                                name = name,
                                file = pageFile
                            )
                        )

                    } catch (
                        exception: Exception
                    ) {

                        pageFile.delete()

                        throw exception
                    }
                }
            }

            pages.sortWith(
                Comparator { first, second ->
                    naturalCompare(
                        first.name,
                        second.name
                    )
                }
            )

            return ComicArchive(
                pages = pages,
                format = ComicArchiveFormat.CBZ
            )

        } finally {

            temporaryFile.delete()
        }
    }

    private fun copyUriToFile(
        context: Context,
        uri: Uri,
        destination: File
    ) {

        val input =
            context.contentResolver
                .openInputStream(uri)
                ?: throw IllegalArgumentException(
                    "Unable to open CBZ file."
                )

        input.use { stream ->

            destination.outputStream().use { output ->

                stream.copyTo(
                    output,
                    bufferSize = 64 * 1024
                )
            }
        }
    }

    private fun isImageFile(
        name: String
    ): Boolean {

        return when (
            name.substringAfterLast(
                '.',
                ""
            ).lowercase()
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

    private fun getExtension(
        name: String
    ): String {

        val extension =
            name.substringAfterLast(
                '.',
                "img"
            )

        return ".$extension"
    }

    private fun naturalCompare(
        first: String,
        second: String
    ): Int {

        val firstParts =
            splitNaturalParts(
                first
            )

        val secondParts =
            splitNaturalParts(
                second
            )

        val size =
            minOf(
                firstParts.size,
                secondParts.size
            )

        for (
            index in 0 until size
        ) {

            val a =
                firstParts[index]

            val b =
                secondParts[index]

            val result =

                if (
                    a is Long &&
                    b is Long
                ) {

                    a.compareTo(b)

                } else {

                    a.toString()
                        .lowercase()
                        .compareTo(
                            b.toString()
                                .lowercase()
                        )
                }

            if (result != 0) {
                return result
            }
        }

        return firstParts.size.compareTo(
            secondParts.size
        )
    }

    private fun splitNaturalParts(
        value: String
    ): List<Any> {

        val result =
            mutableListOf<Any>()

        val regex =
            Regex("\\d+|\\D+")

        for (
            match in regex.findAll(value)
        ) {

            val part =
                match.value

            if (
                part.all {
                    it.isDigit()
                }
            ) {

                result.add(
                    part.toLongOrNull()
                        ?: Long.MAX_VALUE
                )

            } else {

                result.add(part)
            }
        }

        return result
    }
}