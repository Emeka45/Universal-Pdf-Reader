package com.coeric.universalreader

import org.readium.r2.shared.publication.Locator

data class ReadiumEpubSearchResult(
    val locator: Locator,
    val title: String
)