package com.coeric.universalreader

data class ComicArchive(
    val pages: List<ComicPage>,
    val format: ComicArchiveFormat
)

enum class ComicArchiveFormat {
    CBZ,
    CBR
}