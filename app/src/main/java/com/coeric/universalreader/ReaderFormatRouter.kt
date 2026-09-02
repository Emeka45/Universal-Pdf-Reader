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

            else ->
                throw UnsupportedOperationException(
                    "This format is not handled by the reader engine yet: $format"
                )
        }
    }
}