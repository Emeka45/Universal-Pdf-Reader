package com.coeric.universalreader

data class Highlight(
    val id: String,
    val documentUri: String,
    val chapterIndex: Int,
    val selectedText: String,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)