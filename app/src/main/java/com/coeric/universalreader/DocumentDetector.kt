package com.coeric.universalreader

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns

object DocumentDetector {

    fun detect(context: Context, uri: Uri): DocumentInfo {

        var name = "Unknown document"

        context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->

            if (cursor.moveToFirst()) {

                val index = cursor.getColumnIndex(
                    OpenableColumns.DISPLAY_NAME
                )

                if (index >= 0) {
                    name = cursor.getString(index)
                }
            }
        }

        val extension = name
            .substringAfterLast('.', "")
            .lowercase()

        val mimeType = context.contentResolver.getType(uri)

        return DocumentInfo(
            uri = uri.toString(),
            name = name,
            mimeType = mimeType,
            extension = extension
        )
    }
}