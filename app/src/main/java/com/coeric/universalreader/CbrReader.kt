package com.coeric.universalreader

import android.content.Context
import android.net.Uri

object CbrReader {

    suspend fun open(
        context: Context,
        uri: Uri
    ): ComicArchive {

        throw UnsupportedOperationException(
            "CBR/RAR reading is not implemented yet. " +
                "A RAR extraction library is required."
        )
    }
}