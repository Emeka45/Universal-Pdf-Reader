package com.coeric.universalreader

import android.content.Context
import android.net.Uri
import java.io.File
import java.util.zip.ZipFile

object ZipReader {

    private val supportedExtensions =
        setOf(
            "pdf",
            "epub",
            "mobi",
            "prc",
            "azw",
            "azw3",
            "kfx",
            "fb2",
            "cbz",
            "cbr",
            "txt",
            "html",
            "htm",
            "xhtml",
            "rtf",
            "doc",
            "docx",
            "odt",
            "md",
            "markdown"
        )

    suspend fun open(
        context: Context,
        uri: Uri
    ): ReaderDocument {

        val temporaryZip =
            File.createTempFile(
                "universal_reader_",
                ".zip",
                context.cacheDir
            )

        try {

            copyUriToFile(
                context,
                uri,
                temporaryZip
            )

            ZipFile(
                temporaryZip
            ).use { zip ->

                val entry =
                    findBestDocumentEntry(
                        zip
                    )
                    ?: throw IllegalArgumentException(
                        "No supported document was found inside this ZIP archive."
                    )

                val temporaryDocument =
                    File.createTempFile(
                        "universal_reader_entry_",
                        getSafeExtension(
                            entry.name
                        ),
                        context.cacheDir
                    )

                try {

                    zip.getInputStream(
                        entry
                    ).use { input ->

                        temporaryDocument
                            .outputStream()
                            .use { output ->

                                input.copyTo(
                                    output,
                                    bufferSize = 64 * 1024
                                )
                            }
                    }

                    return openExtractedDocument(
                        context,
                        temporaryDocument,
                        entry.name
                    )

                } finally {

                    temporaryDocument.delete()
                }
            }

        } finally {

            temporaryZip.delete()
        }
    }

    private suspend fun openExtractedDocument(
        context: Context,
        file: File,
        originalName: String
    ): ReaderDocument {

        val extension =
            originalName
                .substringAfterLast(
                    '.',
                    ""
                )
                .lowercase()

        val documentUri =
            Uri.fromFile(file)

        return when (extension) {

            "epub" -> {

                val epub =
                    EpubReader.open(
                        context,
                        documentUri
                    )

                ReaderDocument(
                    title =
                        epub.title,

                    author =
                        epub.author,

                    chapters =
                        epub.chapters.map {
                            ReaderChapter(
                                title =
                                    it.title,

                                content =
                                    it.content
                            )
                        }
                )
            }

            "mobi",
            "prc" -> {

                MobiReader.open(
                    context,
                    documentUri
                )
            }

            "azw",
            "azw3",
            "kfx" -> {

                Kf8Reader.open(
                    context,
                    documentUri
                )
            }

            "fb2" -> {

                Fb2Reader.open(
                    context,
                    documentUri
                )
            }

            "rtf" -> {

                RtfReader.open(
                    context,
                    documentUri
                )
            }

            "doc" -> {

                DocReader.open(
                    context,
                    documentUri
                )
            }

            "docx" -> {

                DocxReader.open(
                    context,
                    documentUri
                )
            }

            "odt" -> {

                OdtReader.open(
                    context,
                    documentUri
                )
            }

            "md",
            "markdown" -> {

                MarkdownReader.open(
                    context,
                    documentUri
                )
            }

            "html",
            "htm",
            "xhtml" -> {

                HtmlReader.open(
                    context,
                    documentUri
                )
            }

            "txt" -> {

                TxtReader.open(
                    context,
                    documentUri
                )
            }

            "pdf" -> {

                throw UnsupportedOperationException(
                    "PDF files inside ZIP archives use the PDF reader directly."
                )
            }

            "cbz",
            "cbr" -> {

                throw UnsupportedOperationException(
                    "Comic archives inside ZIP files use the comic reader directly."
                )
            }

            else -> {

                throw UnsupportedOperationException(
                    "Unsupported document inside ZIP: $extension"
                )
            }
        }
    }

    private fun findBestDocumentEntry(
        zip: ZipFile
    ): java.util.zip.ZipEntry? {

        val entries =
            zip.entries()
                .asSequence()
                .filter {
                    !it.isDirectory
                }
                .filter {
                    hasSupportedExtension(
                        it.name
                    )
                }
                .toList()

        if (entries.isEmpty()) {
            return null
        }

        return entries.minByOrNull {
            documentPriority(
                it.name
            )
        }
    }

    private fun documentPriority(
        name: String
    ): Int {

        return when (
            name.substringAfterLast(
                '.',
                ""
            ).lowercase()
        ) {

            "epub" -> 1
            "mobi",
            "prc" -> 2
            "azw3" -> 3
            "azw" -> 4
            "fb2" -> 5
            "pdf" -> 6
            "docx" -> 7
            "doc" -> 8
            "odt" -> 9
            "rtf" -> 10
            "html",
            "htm",
            "xhtml" -> 11
            "md",
            "markdown" -> 12
            "txt" -> 13
            "cbz" -> 14
            "cbr" -> 15
            else -> 100
        }
    }

    private fun hasSupportedExtension(
        name: String
    ): Boolean {

        val extension =
            name.substringAfterLast(
                '.',
                ""
            ).lowercase()

        return extension in supportedExtensions
    }

    private fun getSafeExtension(
        name: String
    ): String {

        val extension =
            name.substringAfterLast(
                '.',
                ""
            )
            .lowercase()

        return if (
            extension.isBlank()
        ) {
            ".bin"
        } else {
            ".$extension"
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
                    "Unable to open ZIP file."
                )

        input.use { stream ->

            destination
                .outputStream()
                .use { output ->

                    stream.copyTo(
                        output,
                        bufferSize = 64 * 1024
                    )
                }
        }
    }
}