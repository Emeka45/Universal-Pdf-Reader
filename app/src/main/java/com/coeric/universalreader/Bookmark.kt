package com.coeric.universalreader

data class Bookmark(
    val id: String,
    val documentUri: String,
    val chapterIndex: Int,
    val title: String,
    val note: String = "",
    val createdAt: Long =
        System.currentTimeMillis()
)