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

            DocumentFormat.CBZ -> {
                throw UnsupportedOperationException(
                    "CBZ uses the comic reader."
                )
            }

            DocumentFormat.CBR -> {
                throw UnsupportedOperationException(
                    "CBR/RAR reading is not implemented yet."
                )
            }

            else ->
                throw UnsupportedOperationException(
                    "This format is not handled by the reader engine yet: $format"
                )
        }
    }
}