package com.coeric.universalreader

data class ReadingPosition(
    val documentUri: String,
    val chapterIndex: Int = 0,
    val scrollIndex: Int = 0,
    val scrollOffset: Int = 0
)