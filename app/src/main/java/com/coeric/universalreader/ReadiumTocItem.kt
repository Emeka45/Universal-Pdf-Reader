package com.coeric.universalreader

import org.readium.r2.shared.publication.Link

data class ReadiumTocItem(
    val title: String,
    val href: String,
    val children: List<ReadiumTocItem> = emptyList()
)

fun Link.toReadiumTocItem(): ReadiumTocItem {

    return ReadiumTocItem(
        title = title ?: "Untitled",
        href = href.toString(),
        children =
            children.map {
                it.toReadiumTocItem()
            }
    )
}