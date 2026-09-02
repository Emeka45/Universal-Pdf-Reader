package com.coeric.universalreader

object EpubImageMarker {

    private const val PREFIX =
        "[[EPUB_IMAGE:"

    private const val SUFFIX =
        "]]"

    fun create(
        path: String
    ): String {

        return PREFIX +
            path +
            SUFFIX
    }

    fun isMarker(
        text: String
    ): Boolean {

        return text.startsWith(
            PREFIX
        ) &&
            text.endsWith(
                SUFFIX
            )
    }

    fun extractPath(
        text: String
    ): String? {

        if (
            !isMarker(text)
        ) {
            return null
        }

        return text
            .removePrefix(PREFIX)
            .removeSuffix(SUFFIX)
            .trim()
            .ifBlank {
                null
            }
    }
}