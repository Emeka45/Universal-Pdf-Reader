package com.coeric.universalreader

import android.content.Context
import android.net.Uri

/**
 * Entry point for the professional Readium EPUB engine.
 *
 * This class intentionally sits between Universal Reader
 * and Readium so that the rest of the application does not
 * become dependent on Readium's internal APIs.
 */
object ReadiumEpubEngine {

    /**
     * Checks whether Readium can identify the supplied
     * publication.
     *
     * The actual opening/presentation layer will be connected
     * after the dependency is confirmed by the build.
     */
    fun isAvailable(
        context: Context,
        uri: Uri
    ): Boolean {

        return try {

            val type =
                context.contentResolver.getType(
                    uri
                )

            val name =
                uri.toString()
                    .lowercase()

            type == "application/epub+zip" ||
                name.endsWith(".epub")

        } catch (
            exception: Exception
        ) {

            false
        }
    }
}