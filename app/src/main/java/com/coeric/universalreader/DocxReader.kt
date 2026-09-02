package com.coeric.universalreader

import android.content.Context
import android.net.Uri
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory

object DocxReader {

    suspend fun open(
        context: Context,
        uri: Uri
    ): ReaderDocument {

        val input =
            context.contentResolver
                .openInputStream(uri)
                ?: throw IllegalStateException(
                    "Unable to open DOCX file."
                )

        val bytes =
            input.use {
                it.readBytes()
            }

        if (bytes.isEmpty()) {
            throw IllegalArgumentException(
                "The DOCX file is empty."
            )
        }

        val documentXml =
            extractDocumentXml(bytes)

        val text =
            extractText(documentXml)

        if (text.isBlank()) {
            throw IllegalArgumentException(
                "No readable text was found in this DOCX file."
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

    private fun extractDocumentXml(
        bytes: ByteArray
    ): ByteArray {

        ByteArrayInputStream(
            bytes
        ).use { input ->

            ZipInputStream(
                input
            ).use { zip ->

                while (true) {

                    val entry =
                        zip.nextEntry
                            ?: break

                    if (
                        entry.name ==
                        "word/document.xml"
                    ) {

                        return zip.readBytes()
                    }

                    zip.closeEntry()
                }
            }
        }

        throw IllegalArgumentException(
            "Invalid DOCX file: document.xml was not found."
        )
    }

    private fun extractText(
        xmlBytes: ByteArray
    ): String {

        val factory =
            DocumentBuilderFactory
                .newInstance()

        factory.isNamespaceAware =
            true

        val builder =
            factory.newDocumentBuilder()

        val document =
            builder.parse(
                ByteArrayInputStream(
                    xmlBytes
                )
            )

        val textBuilder =
            StringBuilder()

        val nodes =
            document.getElementsByTagNameNS(
                "http://schemas.openxmlformats.org/wordprocessingml/2006/main",
                "t"
            )

        for (
            index in 0 until nodes.length
        ) {

            val node =
                nodes.item(index)

            if (
                node.textContent != null
            ) {

                textBuilder.append(
                    node.textContent
                )
            }

            val parent =
                node.parentNode

            if (
                parent?.localName ==
                "p"
            ) {

                textBuilder.append(
                    " "
                )
            }
        }

        val paragraphs =
            document.getElementsByTagNameNS(
                "http://schemas.openxmlformats.org/wordprocessingml/2006/main",
                "p"
            )

        val paragraphBuilder =
            StringBuilder()

        for (
            index in 0 until paragraphs.length
        ) {

            val paragraph =
                paragraphs.item(index)

            val textNodes =
                paragraph
                    .childNodes

            for (
                childIndex in
                0 until textNodes.length
            ) {

                val child =
                    textNodes.item(
                        childIndex
                    )

                collectText(
                    child,
                    paragraphBuilder
                )
            }

            if (
                paragraphBuilder.isNotEmpty()
            ) {

                paragraphBuilder.append(
                    "\n\n"
                )
            }
        }

        val result =
            paragraphBuilder
                .toString()
                .trim()

        return if (
            result.isNotBlank()
        ) {

            result

        } else {

            textBuilder
                .toString()
                .trim()
        }
    }

    private fun collectText(
        node: org.w3c.dom.Node,
        output: StringBuilder
    ) {

        if (
            node.nodeType ==
            org.w3c.dom.Node.TEXT_NODE
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

            output.append(
                "\t"
            )

            return
        }

        if (
            node.localName ==
            "br"
        ) {

            output.append(
                "\n"
            )

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
            ?: "DOCX Book"
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

        if (
            paragraphs.isEmpty()
        ) {

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