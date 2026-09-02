package com.coeric.universalreader

import android.content.Context
import android.net.Uri
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Node

object OdtReader {

    suspend fun open(
        context: Context,
        uri: Uri
    ): ReaderDocument {

        val input =
            context.contentResolver
                .openInputStream(uri)
                ?: throw IllegalStateException(
                    "Unable to open ODT file."
                )

        val bytes =
            input.use {
                it.readBytes()
            }

        if (bytes.isEmpty()) {
            throw IllegalArgumentException(
                "The ODT file is empty."
            )
        }

        val contentXml =
            extractContentXml(bytes)

        val text =
            extractText(contentXml)

        if (text.isBlank()) {
            throw IllegalArgumentException(
                "No readable text was found in this ODT file."
            )
        }

        val title =
            detectTitle(text)

        return ReaderDocument(
            title = title,
            author = null,
            chapters = createChapters(text)
        )
    }

    private fun extractContentXml(
        bytes: ByteArray
    ): ByteArray {

        ByteArrayInputStream(bytes).use { input ->

            ZipInputStream(input).use { zip ->

                while (true) {

                    val entry =
                        zip.nextEntry
                            ?: break

                    if (
                        entry.name ==
                        "content.xml"
                    ) {

                        return zip.readBytes()
                    }

                    zip.closeEntry()
                }
            }
        }

        throw IllegalArgumentException(
            "Invalid ODT file: content.xml was not found."
        )
    }

    private fun extractText(
        xmlBytes: ByteArray
    ): String {

        val factory =
            DocumentBuilderFactory
                .newInstance()

        factory.isNamespaceAware = true

        val builder =
            factory.newDocumentBuilder()

        val document =
            builder.parse(
                ByteArrayInputStream(
                    xmlBytes
                )
            )

        val output =
            StringBuilder()

        val paragraphs =
            document.getElementsByTagNameNS(
                "*",
                "p"
            )

        for (
            index in 0 until paragraphs.length
        ) {

            val paragraph =
                paragraphs.item(index)

            val paragraphText =
                StringBuilder()

            collectText(
                paragraph,
                paragraphText
            )

            val text =
                paragraphText
                    .toString()
                    .replace(
                        Regex("\\s+"),
                        " "
                    )
                    .trim()

            if (text.isNotBlank()) {

                if (output.isNotEmpty()) {
                    output.append("\n\n")
                }

                output.append(text)
            }
        }

        return output
            .toString()
            .trim()
    }

    private fun collectText(
        node: Node,
        output: StringBuilder
    ) {

        if (
            node.nodeType ==
            Node.TEXT_NODE
        ) {

            output.append(
                node.nodeValue
            )

            return
        }

        if (
            node.localName ==
            "tab"
        ) {

            output.append("\t")

            return
        }

        if (
            node.localName ==
            "line-break"
        ) {

            output.append("\n")

            return
        }

        val children =
            node.childNodes

        for (
            index in 0 until children.length
        ) {

            collectText(
                children.item(index),
                output
            )
        }
    }

    private fun detectTitle(
        text: String
    ): String {

        return text
            .lineSequence()
            .map {
                it.trim()
            }
            .firstOrNull {
                it.length in 2..150
            }
            ?: "ODT Book"
    }

    private fun createChapters(
        text: String
    ): List<ReaderChapter> {

        val paragraphs =
            text
                .replace(
                    "\r\n",
                    "\n"
                )
                .replace(
                    "\r",
                    "\n"
                )
                .split(
                    Regex(
                        "\\n\\s*\\n"
                    )
                )
                .map {
                    it.trim()
                }
                .filter {
                    it.isNotBlank()
                }

        if (paragraphs.isEmpty()) {

            return listOf(
                ReaderChapter(
                    title = "Chapter 1",
                    content = text
                )
            )
        }

        val chapters =
            mutableListOf<ReaderChapter>()

        val builder =
            StringBuilder()

        var chapterNumber =
            1

        for (
            paragraph in paragraphs
        ) {

            if (
                builder.isNotEmpty() &&
                builder.length +
                paragraph.length >
                12000
            ) {

                chapters.add(
                    ReaderChapter(
                        title =
                            "Chapter $chapterNumber",
                        content =
                            builder
                                .toString()
                                .trim()
                    )
                )

                chapterNumber++

                builder.clear()
            }

            if (
                builder.isNotEmpty()
            ) {

                builder.append(
                    "\n\n"
                )
            }

            builder.append(
                paragraph
            )
        }

        if (
            builder.isNotEmpty()
        ) {

            chapters.add(
                ReaderChapter(
                    title =
                        "Chapter $chapterNumber",
                    content =
                        builder
                            .toString()
                            .trim()
                )
            )
        }

        return chapters
    }
}