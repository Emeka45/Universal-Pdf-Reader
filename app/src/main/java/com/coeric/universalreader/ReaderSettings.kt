package com.coeric.universalreader

data class ReaderSettings(
    val fontSize: Float = 18f,
    val lineSpacing: Float = 1.55f,
    val theme: ReaderTheme = ReaderTheme.LIGHT,
    val textAlignment: ReaderTextAlignment =
        ReaderTextAlignment.LEFT
)

enum class ReaderTheme {
    LIGHT,
    DARK,
    SEPIA
}

enum class ReaderTextAlignment {
    LEFT,
    JUSTIFY
}