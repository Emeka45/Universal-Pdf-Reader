package com.coeric.universalreader

data class EpubDocument(
    val title: String,
    val author: String?,
    val chapters: List<EpubChapter>,
    val images: List<EpubImage> = emptyList()
)