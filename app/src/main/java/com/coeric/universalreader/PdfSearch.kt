package com.coeric.universalreader

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class PdfSearchResult(
    val page: Int,
    val text: String
)

object PdfSearch {

    suspend fun search(
        context: Context,
        uri: Uri,
        query: String
    ): List<PdfSearchResult> =
        withContext(Dispatchers.IO) {

            if (
                query.isBlank()
            ) {
                return@withContext emptyList()
            }

            PDFBoxResourceLoader.init(
                context
            )

            val temporaryFile =
                File.createTempFile(
                    "universal_pdf_search_",
                    ".pdf",
                    context.cacheDir
                )

            try {

                context.contentResolver
                    .openInputStream(uri)
                    ?.use { input ->

                        temporaryFile
                            .outputStream()
                            .use { output ->

                                input.copyTo(
                                    output,
                                    bufferSize = 64 * 1024
                                )
                            }
                    }
                    ?: throw IllegalArgumentException(
                        "Unable to open PDF."
                    )

                PDDocument
                    .load(temporaryFile)
                    .use { document ->

                        val results =
                            mutableListOf<PdfSearchResult>()

                        val stripper =
                            PDFTextStripper()

                        val normalizedQuery =
                            query
                                .trim()
                                .lowercase()

                        for (
                            pageIndex in
                            0 until document.numberOfPages
                        ) {

                            stripper.startPage =
                                pageIndex + 1

                            stripper.endPage =
                                pageIndex + 1

                            val pageText =
                                stripper
                                    .getText(document)
                                    .trim()

                            if (
                                pageText
                                    .lowercase()
                                    .contains(
                                        normalizedQuery
                                    )
                            ) {

                                results.add(
                                    PdfSearchResult(
                                        page =
                                            pageIndex,

                                        text =
                                            createSnippet(
                                                pageText,
                                                normalizedQuery
                                            )
                                    )
                                )
                            }

                            if (
                                results.size >= 100
                            ) {
                                break
                            }
                        }

                        results
                    }

            } finally {

                temporaryFile.delete()
            }
        }

    private fun createSnippet(
        text: String,
        query: String
    ): String {

        val lower =
            text.lowercase()

        val index =
            lower.indexOf(query)

        if (
            index < 0
        ) {

            return text
                .take(240)
                .trim()
        }

        val start =
            (index - 100)
                .coerceAtLeast(0)

        val end =
            (index + query.length + 140)
                .coerceAtMost(
                    text.length
                )

        val prefix =
            if (start > 0) {
                "…"
            } else {
                ""
            }

        val suffix =
            if (end < text.length) {
                "…"
            } else {
                ""
            }

        return prefix +
            text.substring(
                start,
                end
            ).replace(
                Regex(
                    "\\s+"
                ),
                " "
            ).trim() +
            suffix
    }
}