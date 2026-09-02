package com.coeric.universalreader

import android.content.Context
import android.net.Uri
import com.github.junrar.Archive
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object CbrReader {

    suspend fun open(
        context: Context,
        uri: Uri
    ): ComicArchive {

        val temporaryFile =
            File.createTempFile(
                "universal_reader_",
                ".rar",
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

            Archive(
                temporaryFile
            ).use { archive ->

                val headers =
                    archive.fileHeaders

                for (header in headers) {

                    if (header.isDirectory) {
                        continue
                    }

                    val name =
                        header.fileNameString

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

                        FileOutputStream(
                            pageFile
                        ).use { output ->

                            archive.extractFile(
                                header,
                                output
                            )
                        }

                        pages.add(
                            ComicPage(
                                name = name,
                                file = pageFile
                            )
                        )

                    } catch (exception: Exception) {

                        pageFile.delete()

                        throw exception
                    }
                }
            }

            pages.sortWith(
                compareBy {
                    naturalSortKey(
                        it.name
                    )
                }
            )

            return ComicArchive(
                pages = pages,
                format = ComicArchiveFormat.CBR
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
                    "Unable to open CBR file."
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

    private fun naturalSortKey(
        name: String
    ): String {

        return name
            .lowercase()
            .replace(
                Regex("\\d+")
            ) { match ->

                match.value
                    .padStart(
                        12,
                        '0'
                    )
            }
    }
}