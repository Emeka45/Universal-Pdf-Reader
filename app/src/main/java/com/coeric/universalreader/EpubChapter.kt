package com.coeric.universalreader

data class EpubChapter(
    val title: String,
    val content: String,
    val originalHtml: String = content
)