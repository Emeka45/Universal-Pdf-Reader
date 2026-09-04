package com.coeric.universalreader

import android.content.Context
import android.net.Uri

object ReaderFormatRouter {

    suspend fun open(
        context: Context,
        uri: Uri
    ): ReaderDocument {

        val format =
            DocumentFormatDetector.detect(
                context,
                uri
            )

        return when (format) {

            DocumentFormat.MOBI ->
                MobiReader.open(
                    context,
                    uri
                )

            DocumentFormat.AZW ->
                Kf8Reader.open(
                    context,
                    uri
                )

            DocumentFormat.AZW3 ->
                Kf8Reader.open(
                    context,
                    uri
                )

            DocumentFormat.FB2 ->
                Fb2Reader.open(
                    context,
                    uri
                )

            DocumentFormat.RTF ->
                RtfReader.open(
                    context,
                    uri
                )

            DocumentFormat.DOC ->
                DocReader.open(
                    context,
                    uri
                )

            DocumentFormat.DOCX ->
                DocxReader.open(
                    context,
                    uri
                )

            DocumentFormat.ODT ->
                OdtReader.open(
                    context,
                    uri
                )

            DocumentFormat.MARKDOWN ->
                MarkdownReader.open(
                    context,
                    uri
                )

            DocumentFormat.HTML,
            DocumentFormat.XHTML ->
                HtmlReader.open(
                    context,
                    uri
                )

            DocumentFormat.TXT ->
                TxtReader.open(
                    context,
                    uri
                )

            DocumentFormat.ZIP ->
                ZipReader.open(
                    context,
                    uri
                )

            DocumentFormat.CBZ ->
                throw UnsupportedOperationException(
                    "CBZ uses the comic reader."
                )

            DocumentFormat.CBR ->
                throw UnsupportedOperationException(
                    "CBR uses the comic reader."
                )

            DocumentFormat.PDF ->
                throw UnsupportedOperationException(
                    "PDF uses the PDF reader."
                )

            DocumentFormat.EPUB ->
                throw UnsupportedOperationException(
                    "EPUB uses the EPUB reader."
                )

            DocumentFormat.UNKNOWN -> {

                val info =
                    DocumentDetector.detect(
                        context,
                        uri
                    )

                // Check if it's an audio or video file
                val extension = info.extension.lowercase()
                if (extension == "mp3" || extension == "mp4") {
                    throw UnsupportedOperationException(
                        "Audio and video files (MP3, MP4) are not supported."
                    )
                }

                // Check MIME type for audio/video
                val mimeType = info.mimeType?.lowercase() ?: ""
                if (mimeType.startsWith("audio/") ||
                    mimeType.startsWith("video/")) {
                    throw UnsupportedOperationException(
                        "Audio and video files are not supported."
                    )
                }

                // Treat all other unknown formats as text
                TxtReader.open(
                    context,
                    uri
                )
            }
        }
    }
}
