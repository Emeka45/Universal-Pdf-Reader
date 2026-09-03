package com.coeric.universalreader

data class EpubChapter(
    val title: String,
    val content: String,
    val originalHtml: String = content
)

data class EpubDocument(
    val title: String,
    val author: String?,
    val chapters: List<EpubChapter>,
    val images: List<EpubImage> = emptyList()
)
