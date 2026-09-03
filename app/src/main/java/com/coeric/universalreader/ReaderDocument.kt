package com.coeric.universalreader

data class ReaderDocument(
    val title: String,
    val author: String? = null,
    val chapters: List<ReaderChapter>
)

data class ReaderChapter(
    val title: String,
    val content: String
)