package com.coeric.universalreader

sealed class EpubContentBlock {

    data class Text(
        val text: String
    ) : EpubContentBlock()

    data class Image(
        val path: String,
        val altText: String? = null
    ) : EpubContentBlock()
}