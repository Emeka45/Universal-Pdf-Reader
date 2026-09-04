package com.coeric.universalreader

import android.content.Context
import android.net.Uri

object DocumentFormatDetector {

    fun detect(
        context: Context,
        uri: Uri
    ): DocumentFormat {

        val info =
            DocumentDetector.detect(
                context,
                uri
            )

        return when (info.extension.lowercase()) {

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

            "cbz" ->
                DocumentFormat.CBZ

            "cbr" ->
                DocumentFormat.CBR

            "zip" ->
                DocumentFormat.ZIP

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

            // Reject audio and video formats
            "mp3",
            "mp4" ->
                DocumentFormat.UNKNOWN

            else ->
                detectFromMimeType(
                    info.mimeType
                )
        }
    }

    private fun detectFromMimeType(
        mimeType: String?
    ): DocumentFormat {

        val lowerMime = mimeType?.lowercase() ?: return DocumentFormat.UNKNOWN

        // Reject audio and video MIME types
        if (lowerMime.startsWith("audio/") ||
            lowerMime.startsWith("video/")) {
            return DocumentFormat.UNKNOWN
        }

        return when (lowerMime) {

            "application/pdf" ->
                DocumentFormat.PDF

            "application/epub+zip" ->
                DocumentFormat.EPUB

            "application/x-mobipocket-ebook" ->
                DocumentFormat.MOBI

            "application/vnd.amazon.ebook" ->
                DocumentFormat.AZW3

            "application/x-fictionbook+xml" ->
                DocumentFormat.FB2

            "application/zip" ->
                DocumentFormat.ZIP

            "text/plain" ->
                DocumentFormat.TXT

            "text/html" ->
                DocumentFormat.HTML

            "application/xhtml+xml" ->
                DocumentFormat.XHTML

            "application/rtf",
            "text/rtf" ->
                DocumentFormat.RTF

            "application/msword" ->
                DocumentFormat.DOC

            "application/vnd.openxmlformats-officedocument.wordprocessingml.document" ->
                DocumentFormat.DOCX

            "application/vnd.oasis.opendocument.text" ->
                DocumentFormat.ODT

            "text/markdown" ->
                DocumentFormat.MARKDOWN

            else ->
                DocumentFormat.UNKNOWN
        }
    }
}
