package com.coeric.universalreader

import android.content.Context
import android.net.Uri
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream

object ZipReader {

    suspend fun open(
        context: Context,
        uri: Uri
    ): ReaderDocument {

        val input =
            context.contentResolver
                .openInputStream(uri)
                ?: throw IllegalStateException(
                    "Unable to open ZIP file."
                )

        val bytes =
            input.use {
                it.readBytes()
            }

        if (bytes.isEmpty()) {
            throw IllegalArgumentException(
                "The ZIP file is empty."
            )
        }

        val entries =
            extractEntries(bytes)

        if (entries.isEmpty()) {
            throw IllegalArgumentException(
                "No files were found inside the ZIP archive."
            )
        }

        // Prefer ebook/document formats.
        val preferred =
            entries
                .filter {
                    isSupportedDocument(
                        it.name
                    )
                }
                .sortedBy {
                    documentPriority(
                        it.name
                    )
                }

        if (preferred.isEmpty()) {
            throw IllegalArgumentException(
                "No readable document was found inside this ZIP archive."
            )
        }

        val selected =
            preferred.first()

        val format =
            detectFormat(
                selected.name
            )

        return openEntry(
            context = context,
            entry = selected,
            format = format
        )
    }

    private data class ZipEntryData(
        val name: String,
        val data: ByteArray
    )

    private fun extractEntries(
        bytes: ByteArray
    ): List<ZipEntryData> {

        val result =
            mutableListOf<ZipEntryData>()

        ByteArrayInputStream(bytes).use { input ->

            ZipInputStream(input).use { zip ->

                while (true) {

                    val entry =
                        zip.nextEntry
                            ?: break

                    if (
                        !entry.isDirectory
                    ) {

                        val name =
                            entry.name

                        if (
                            name.isNotBlank()
                        ) {

                            val output =
                                ByteArrayOutputStream()

                            val buffer =
                                ByteArray(
                                    8192
                                )

                            while (true) {

                                val count =
                                    zip.read(
                                        buffer
                                    )

                                if (count <= 0) {
                                    break
                                }

                                output.write(
                                    buffer,
                                    0,
                                    count
                                )
                            }

                            val data =
                                output.toByteArray()

                            if (data.isNotEmpty()) {

                                result.add(
                                    ZipEntryData(
                                        name = name,
                                        data = data
                                    )
                                )
                            }
                        }
                    }

                    zip.closeEntry()
                }
            }
        }

        return result
    }

    private fun isSupportedDocument(
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

            "pdf",
            "epub",
            "mobi",
            "prc",
            "azw",
            "azw3",
            "kfx",
            "fb2",
            "txt",
            "html",
            "htm",
            "xhtml",
            "rtf",
            "doc",
            "docx",
            "odt",
            "md",
            "markdown" -> true

            else -> false
        }
    }

    private fun documentPriority(
        name: String
    ): Int {

        return when (
            name
                .substringAfterLast(
                    '.',
                    ""
                )
                .lowercase()
        ) {

            "epub" -> 1
            "pdf" -> 2
            "mobi", "prc" -> 3
            "azw3", "azw", "kfx" -> 4
            "fb2" -> 5
            "docx" -> 6
            "odt" -> 7
            "doc" -> 8
            "rtf" -> 9
            "html", "htm", "xhtml" -> 10
            "md", "markdown" -> 11
            "txt" -> 12

            else -> 100
        }
    }

    private fun detectFormat(
        name: String
    ): DocumentFormat {

        return when (
            name
                .substringAfterLast(
                    '.',
                    ""
                )
                .lowercase()
        ) {

            "pdf" ->
                DocumentFormat.PDF

            "epub" ->
                DocumentFormat.EPUB

            "mobi",
            "prc" ->
                DocumentFormat.MOBI

            "azw" ->
                DocumentFormat.AZW

            "azw3",
            "kfx" ->
                DocumentFormat.AZW3

            "fb2" ->
                DocumentFormat.FB2

            "txt" ->
                DocumentFormat.TXT

            "html",
            "htm" ->
                DocumentFormat.HTML

            "xhtml" ->
                DocumentFormat.XHTML

            "rtf" ->
                DocumentFormat.RTF

            "doc" ->
                DocumentFormat.DOC

            "docx" ->
                DocumentFormat.DOCX

            "odt" ->
                DocumentFormat.ODT

            "md",
            "markdown" ->
                DocumentFormat.MARKDOWN

            else ->
                DocumentFormat.UNKNOWN
        }
    }

    private suspend fun openEntry(
        context: Context,
        entry: ZipEntryData,
        format: DocumentFormat
    ): ReaderDocument {

        /*
         * The existing readers work with Android Uri objects.
         * A ZIP entry is already in memory, so we use a
         * temporary content URI through an in-memory provider
         * only where possible.
         *
         * For now, ZIP acts as an archive detector and reports
         * the selected document rather than pretending that an
         * arbitrary ZIP entry can be passed directly to another
         * reader.
         */

        throw UnsupportedOperationException(
            "ZIP contains a readable ${format.name} file: " +
                entry.name +
                ". Direct archive-entry reading will be connected in the integration pass."
        )
    }
}