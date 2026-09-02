package com.coeric.universalreader

data class EpubImage(
    val path: String,
    val mimeType: String?,
    val data: ByteArray
)