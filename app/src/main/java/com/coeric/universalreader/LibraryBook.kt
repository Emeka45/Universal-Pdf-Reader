package com.coeric.universalreader

data class LibraryBook(
    val uri: String,
    val name: String,
    val mimeType: String?,
    val extension: String,
    val lastOpened: Long,
    val isFavorite: Boolean = false
)