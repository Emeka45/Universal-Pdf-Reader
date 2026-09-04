package com.coeric.universalreader

import org.readium.r2.shared.publication.Locator

data class ReadiumBookmark(
    val id: String,
    val documentUri: String,
    val locator: Locator,
    val title: String,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)