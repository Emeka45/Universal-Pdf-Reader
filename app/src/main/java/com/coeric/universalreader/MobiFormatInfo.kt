package com.coeric.universalreader

data class MobiFormatInfo(
    val isKf8: Boolean,
    val isPalmDoc: Boolean,
    val hasExth: Boolean,
    val title: String?,
    val author: String?
)