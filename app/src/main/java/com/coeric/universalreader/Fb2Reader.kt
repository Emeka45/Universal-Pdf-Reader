package com.coeric.universalreader

import android.content.Context
import android.net.Uri
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream

object Fb2Reader {

    suspend fun open(
        context: Context,
        uri: Uri
    ): ReaderDocument {

        val input =
            context.contentResolver
                .openInputStream(uri)
                ?: throw IllegalArgumentException(
                    "Unable to open FB2 file."
                )

        input.use {
            return parse(
                context,
                uri,
                it
            )
        }
    }

    private fun parse(
        context: Context,
        uri: Uri,
        input: InputStream
    ): ReaderDocument {

        val parser =
            Xml.newPullParser()

        parser.setInput(
            input,
            null
        )

        var title: String? = null
        var author: String? = null

        val chapters =
            mutableListOf<ReaderChapter>()

        var event =
            parser.eventType

        while (
            event != XmlPullParser.END_DOCUMENT
        ) {

            if (
                event == XmlPullParser.START_TAG
            ) {

                when (
                    parser.name.lowercase()
                ) {

                    "book-title" -> {

                        val value =
                            readElementText(
                                parser
                            )

                        if (
                            title.isNullOrBlank() &&
                            value.isNotBlank()
                        ) {
                            title = value
                        }
                    }

                    "first-name" -> {

                        val value =
                            readElementText(
                                parser
                            )

                        if (
                            author.isNullOrBlank() &&
                            value.isNotBlank()
                        ) {
                            author = value
                        }
                    }

                    "section" -> {

                        val chapter =
                            readSection(
                                parser,
                                chapters.size + 1
                            )

                        if (
                            chapter != null
                        ) {
                            chapters.add(
                                chapter
                            )
                        }
                    }
                }
            }

            event =
                parser.next()
        }

        if (chapters.isEmpty()) {

            throw IllegalArgumentException(
                "No readable sections were found in this FB2 file."
            )
        }

        return ReaderDocument(
            title =
                title
                    ?: getDocumentName(
                        context,
                        uri
                    ),

            author =
                author,

            chapters =
                chapters
        )
    }

    private fun readSection(
        parser: XmlPullParser,
        chapterNumber: Int
    ): ReaderChapter? {

        var title = ""

        val paragraphs =
            mutableListOf<String>()

        var depth =
            parser.depth

        var event =
            parser.next()

        while (
            event != XmlPullParser.END_DOCUMENT
        ) {

            if (
                event == XmlPullParser.END_TAG &&
                parser.depth < depth
            ) {
                break
            }

            if (
                event == XmlPullParser.START_TAG
            ) {

                when (
                    parser.name.lowercase()
                ) {

                    "title" -> {

                        val value =
                            readTitle(
                                parser
                            )

                        if (
                            title.isBlank() &&
                            value.isNotBlank()
                        ) {
                            title = value
                        }
                    }

                    "p" -> {

                        val value =
                            readElementText(
                                parser
                            )

                        if (
                            value.isNotBlank()
                        ) {
                            paragraphs.add(
                                value
                            )
                        }
                    }

                    "empty-line" -> {

                        if (
                            paragraphs.isNotEmpty() &&
                            paragraphs.last().isNotBlank()
                        ) {
                            paragraphs.add("")
                        }
                    }

                    "subtitle",
                    "epigraph",
                    "text-author",
                    "poem" -> {

                        val value =
                            readContainerText(
                                parser
                            )

                        if (
                            value.isNotBlank()
                        ) {
                            paragraphs.add(
                                value
                            )
                        }
                    }
                }
            }

            event =
                parser.next()
        }

        val content =
            paragraphs
                .joinToString("\n\n")
                .replace(
                    Regex("\n{3,}"),
                    "\n\n"
                )
                .trim()

        if (content.isBlank()) {
            return null
        }

        return ReaderChapter(
            title =
                title.ifBlank {
                    "Chapter $chapterNumber"
                },

            content =
                content
        )
    }

    private fun readTitle(
        parser: XmlPullParser
    ): String {

        return readContainerText(
            parser
        )
    }

    private fun readContainerText(
        parser: XmlPullParser
    ): String {

        val builder =
            StringBuilder()

        val startDepth =
            parser.depth

        var event =
            parser.next()

        while (
            event != XmlPullParser.END_DOCUMENT
        ) {

            if (
                event == XmlPullParser.END_TAG &&
                parser.depth < startDepth
            ) {
                break
            }

            if (
                event == XmlPullParser.TEXT ||
                event == XmlPullParser.CDSECT
            ) {

                builder.append(
                    parser.text
                )
            }

            if (
                event == XmlPullParser.START_TAG
            ) {

                val name =
                    parser.name.lowercase()

                if (
                    name == "p" ||
                    name == "v" ||
                    name == "stanza" ||
                    name == "subtitle" ||
                    name == "text-author"
                ) {

                    builder.append(
                        "\n"
                    )
                }
            }

            event =
                parser.next()
        }

        return cleanText(
            builder.toString()
        )
    }

    private fun readElementText(
        parser: XmlPullParser
    ): String {

        val startDepth =
            parser.depth

        val builder =
            StringBuilder()

        var event =
            parser.next()

        while (
            event != XmlPullParser.END_DOCUMENT
        ) {

            if (
                event == XmlPullParser.END_TAG &&
                parser.depth < startDepth
            ) {
                break
            }

            if (
                event == XmlPullParser.TEXT ||
                event == XmlPullParser.CDSECT
            ) {

                builder.append(
                    parser.text
                )
            }

            event =
                parser.next()
        }

        return cleanText(
            builder.toString()
        )
    }

    private fun cleanText(
        value: String
    ): String {

        return value
            .replace(
                '\u00A0',
                ' '
            )
            .replace(
                "\r\n",
                "\n"
            )
            .replace(
                '\r',
                '\n'
            )
            .replace(
                Regex("[ \t]+"),
                " "
            )
            .replace(
                Regex("\n{3,}"),
                "\n\n"
            )
            .trim()
    }

    private fun getDocumentName(
        context: Context,
        uri: Uri
    ): String {

        var name =
            "FB2 Document"

        context.contentResolver
            .query(
                uri,
                arrayOf(
                    android.provider.OpenableColumns.DISPLAY_NAME
                ),
                null,
                null,
                null
            )
            ?.use { cursor ->

                if (cursor.moveToFirst()) {

                    val index =
                        cursor.getColumnIndex(
                            android.provider.OpenableColumns.DISPLAY_NAME
                        )

                    if (index >= 0) {
                        name =
                            cursor.getString(index)
                    }
                }
            }

        return name.substringBeforeLast(
            '.',
            name
        )
    }
}