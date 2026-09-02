package com.coeric.universalreader

data class LibraryFolder(
    val id: String,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)