package com.coeric.universalreader

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object CbrReader {

    suspend fun open(
        context: Context,
        uri: Uri
    ): List<ComicPage> {

        throw UnsupportedOperationException(
            "CBR support requires a RAR extraction engine. " +
                "The CBR format has been registered, but " +
                "RAR extraction is not enabled yet."
        )
    }
}