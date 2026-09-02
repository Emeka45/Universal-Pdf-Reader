package com.coeric.universalreader

data class DocumentInfo(
    val uri: String,
    val name: String,
    val mimeType: String?,
    val extension: String
)