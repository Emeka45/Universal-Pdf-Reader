package com.coeric.universalreader

import android.content.Context
import android.net.Uri
import org.xmlpull.v1.XmlPullParser
import android.util.Xml

object Fb2Reader {

    suspend fun open(
        context: Context,
        uri: Uri
    ): ReaderDocument {

        val input =
            context.contentResolver
                .openInputStream(uri)
                ?: throw IllegalStateException(
                    "Unable to open FB2 file."
                )

        input.use {

            val parser =
                Xml.newPullParser()

            parser.setInput(
                it,
                null
            )

            var title: String? = null
            var author: String? = null

            val chapters =
                mutableListOf<ReaderChapter>()

            var currentChapterTitle =
                "Chapter 1"

            val currentText =
                StringBuilder()

            var insideBookTitle =
                false

            var insideAuthor =
                false

            var insideSection =
                false

            var chapterNumber =
                1

            while (true) {

                when (
                    parser.eventType
                ) {

                    XmlPullParser.START_TAG -> {

                        when (
                            parser.name
                                .lowercase()
                        ) {

                            "book-title" -> {
                                insideBookTitle = true
                            }

                            "first-name",
                            "middle-name",
                            "last-name" -> {

                                if (
                                    insideAuthor
                                ) {

                                    val value =
                                        parser.nextText()
                                            .trim()

                                    if (
                                        value.isNotBlank()
                                    ) {

                                        if (
                                            author == null
                                        ) {
                                            author = value
                                        } else {
                                            author +=
                                                " $value"
                                        }
                                    }
                                }
                            }

                            "author" -> {
                                insideAuthor = true
                            }

                            "section" -> {

                                if (
                                    insideSection &&
                                    currentText
                                        .isNotBlank()
                                ) {

                                    chapters.add(
                                        ReaderChapter(
                                            title =
                                                currentChapterTitle,
                                            content =
                                                currentText
                                                    .toString()
                                                    .trim()
                                        )
                                    )

                                    currentText.clear()
                                    chapterNumber++
                                }

                                insideSection = true

                                currentChapterTitle =
                                    "Chapter $chapterNumber"
                            }

                            "title" -> {

                                if (
                                    insideSection
                                ) {

                                    val sectionTitle =
                                        readSectionTitle(
                                            parser
                                        )

                                    if (
                                        sectionTitle
                                            .isNotBlank()
                                    ) {

                                        currentChapterTitle =
                                            sectionTitle
                                    }
                                }
                            }

                            "p" -> {

                                if (
                                    insideSection
                                ) {

                                    val paragraph =
                                        parser.nextText()
                                            .trim()

                                    if (
                                        paragraph
                                            .isNotBlank()
                                    ) {

                                        if (
                                            currentText
                                                .isNotEmpty()
                                        ) {

                                            currentText.append(
                                                "\n\n"
                                            )
                                        }

                                        currentText.append(
                                            paragraph
                                        )
                                    }
                                }
                            }

                            "empty-line" -> {

                                if (
                                    insideSection
                                ) {

                                    currentText.append(
                                        "\n\n"
                                    )
                                }
                            }
                        }
                    }

                    XmlPullParser.TEXT -> {

                        if (
                            insideBookTitle
                        ) {

                            val value =
                                parser.text.trim()

                            if (
                                value.isNotBlank()
                            ) {
                                title = value
                            }
                        }
                    }

                    XmlPullParser.END_TAG -> {

                        when (
                            parser.name
                                .lowercase()
                        ) {

                            "book-title" -> {
                                insideBookTitle = false
                            }

                            "author" -> {
                                insideAuthor = false
                            }

                            "section" -> {

                                if (
                                    currentText
                                        .isNotBlank()
                                ) {

                                    chapters.add(
                                        ReaderChapter(
                                            title =
                                                currentChapterTitle,
                                            content =
                                                currentText
                                                    .toString()
                                                    .trim()
                                        )
                                    )

                                    currentText.clear()
                                    chapterNumber++
                                }

                                insideSection = false
                            }
                        }
                    }

                    XmlPullParser.END_DOCUMENT -> {
                        break
                    }
                }

                parser.next()
            }

            if (
                chapters.isEmpty()
            ) {

                throw IllegalArgumentException(
                    "No readable chapters were found in this FB2 file."
                )
            }

            return ReaderDocument(
                title =
                    title?.takeIf {
                        it.isNotBlank()
                    }
                        ?: "FB2 Book",

                author =
                    author?.takeIf {
                        it.isNotBlank()
                    },

                chapters =
                    chapters
            )
        }
    }

    private fun readSectionTitle(
        parser: XmlPullParser
    ): String {

        val builder =
            StringBuilder()

        var depth =
            parser.depth

        while (true) {

            parser.next()

            when (
                parser.eventType
            ) {

                XmlPullParser.TEXT -> {

                    builder.append(
                        parser.text
                    )
                }

                XmlPullParser.END_TAG -> {

                    if (
                        parser.depth <
                        depth
                    ) {
                        break
                    }
                }

                XmlPullParser.END_DOCUMENT -> {
                    break
                }
            }
        }

        return builder
            .toString()
            .replace(
                Regex(
                    "\\s+"
                ),
                " "
            )
            .trim()
    }
}