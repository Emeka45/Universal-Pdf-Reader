package com.coeric.universalreader

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

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

            val cleanQuery =
                query.trim()

            if (cleanQuery.isEmpty()) {
                return@withContext emptyList()
            }

            PDFBoxResourceLoader.init(context)

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
                        "Unable to open the PDF."
                    )

                PDDocument.load(
                    temporaryFile
                ).use { document ->

                    val results =
                        mutableListOf<PdfSearchResult>()

                    val stripper =
                        PDFTextStripper()

                    val normalizedQuery =
                        cleanQuery.lowercase(
                            Locale.ROOT
                        )

                    /*
                     * Search one page at a time.
                     *
                     * This gives us the exact page that
                     * contains the result.
                     */
                    for (
                        pageIndex in
                        0 until document.numberOfPages
                    ) {

                        ensureActive()

                        stripper.startPage =
                            pageIndex + 1

                        stripper.endPage =
                            pageIndex + 1

                        val pageText =
                            stripper
                                .getText(document)
                                .trim()

                        if (pageText.isEmpty()) {
                            continue
                        }

                        val normalizedPage =
                            pageText.lowercase(
                                Locale.ROOT
                            )

                        if (
                            normalizedPage.contains(
                                normalizedQuery
                            )
                        ) {

                            results +=
                                PdfSearchResult(

                                    page =
                                        pageIndex,

                                    text =
                                        createSnippet(
                                            pageText,
                                            normalizedPage,
                                            normalizedQuery
                                        )
                                )

                            /*
                             * Prevent an enormous result list.
                             */
                            if (
                                results.size >= MAX_RESULTS
                            ) {
                                break
                            }
                        }
                    }

                    results
                }

            } finally {

                if (temporaryFile.exists()) {
                    temporaryFile.delete()
                }
            }
        }

    private fun createSnippet(
        originalText: String,
        normalizedText: String,
        normalizedQuery: String
    ): String {

        val index =
            normalizedText.indexOf(
                normalizedQuery
            )

        if (index < 0) {

            return originalText
                .replace(
                    Regex("\\s+"),
                    " "
                )
                .trim()
                .take(MAX_SNIPPET_LENGTH)
        }

        val start =
            (index - 100)
                .coerceAtLeast(0)

        val end =
            (
                index +
                    normalizedQuery.length +
                    140
            ).coerceAtMost(
                originalText.length
            )

        val prefix =
            if (start > 0) {
                "…"
            } else {
                ""
            }

        val suffix =
            if (end < originalText.length) {
                "…"
            } else {
                ""
            }

        return (
            prefix +
                originalText.substring(
                    start,
                    end
                ) +
                suffix
            )
            .replace(
                Regex("\\s+"),
                " "
            )
            .trim()
    }

    private const val MAX_RESULTS = 100

    private const val MAX_SNIPPET_LENGTH = 300
}