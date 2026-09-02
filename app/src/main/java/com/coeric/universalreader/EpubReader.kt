package com.coeric.universalreader

import android.content.Context
import android.net.Uri
import org.w3c.dom.Element
import java.io.File
import java.io.InputStream
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.zip.ZipFile
import javax.xml.parsers.DocumentBuilderFactory

object EpubReader {

    fun open(
        context: Context,
        uri: Uri
    ): EpubDocument {

        val temporaryFile = File.createTempFile(
            "universal_reader_",
            ".epub",
            context.cacheDir
        )

        try {

            context.contentResolver
                .openInputStream(uri)
                ?.use { input ->

                    temporaryFile
                        .outputStream()
                        .use { output ->
                            input.copyTo(output)
                        }
                }
                ?: throw IllegalStateException(
                    "Unable to open EPUB file."
                )

            ZipFile(temporaryFile).use { zip ->

                val containerEntry =
                    zip.getEntry(
                        "META-INF/container.xml"
                    )
                        ?: throw IllegalStateException(
                            "Invalid EPUB: container.xml not found."
                        )

                val containerDocument =
                    parseXml(
                        zip.getInputStream(
                            containerEntry
                        )
                    )

                val rootfile =
                    containerDocument
                        .getElementsByTagNameNS(
                            "*",
                            "rootfile"
                        )
                        .item(0) as? Element
                        ?: throw IllegalStateException(
                            "Invalid EPUB: package file not found."
                        )

                val packagePath =
                    normalizePath(
                        rootfile.getAttribute(
                            "full-path"
                        )
                    )

                val packageEntry =
                    findZipEntry(
                        zip,
                        packagePath
                    )
                        ?: throw IllegalStateException(
                            "EPUB package file not found: $packagePath"
                        )

                val packageDocument =
                    parseXml(
                        zip.getInputStream(
                            packageEntry
                        )
                    )

                val title =
                    findMetadata(
                        packageDocument,
                        "title"
                    )
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?: "Untitled"

                val author =
                    findMetadata(
                        packageDocument,
                        "creator"
                    )
                        ?.takeIf {
                            it.isNotBlank()
                        }

                val manifest =
                    packageDocument
                        .getElementsByTagNameNS(
                            "*",
                            "item"
                        )

                val spine =
                    packageDocument
                        .getElementsByTagNameNS(
                            "*",
                            "itemref"
                        )

                val manifestMap =
                    mutableMapOf<String, String>()

                val titleMap =
                    mutableMapOf<String, String>()

                for (i in 0 until manifest.length) {

                    val item =
                        manifest.item(i) as? Element
                            ?: continue

                    val id =
                        item.getAttribute("id")

                    val href =
                        item.getAttribute("href")

                    if (
                        id.isNotBlank() &&
                        href.isNotBlank()
                    ) {

                        manifestMap[id] = href

                        val itemTitle =
                            item.getAttribute("title")

                        if (
                            itemTitle.isNotBlank()
                        ) {
                            titleMap[id] =
                                itemTitle
                        }
                    }
                }

                val packageDirectory =
                    packagePath
                        .substringBeforeLast(
                            "/",
                            ""
                        )

                val chapters =
                    mutableListOf<EpubChapter>()

                for (i in 0 until spine.length) {

                    val itemRef =
                        spine.item(i) as? Element
                            ?: continue

                    val idref =
                        itemRef.getAttribute(
                            "idref"
                        )

                    val href =
                        manifestMap[idref]
                            ?: continue

                    val fullPath =
                        resolvePath(
                            packageDirectory,
                            href
                        )

                    val chapterEntry =
                        findZipEntry(
                            zip,
                            fullPath
                        )
                            ?: continue

                    val html =
                        zip.getInputStream(
                            chapterEntry
                        )
                            .bufferedReader()
                            .use {
                                it.readText()
                            }

                    val text =
                        htmlToText(html)

                    if (
                        text.isBlank()
                    ) {
                        continue
                    }

                    val chapterTitle =
                        titleMap[idref]
                            ?.takeIf {
                                it.isNotBlank()
                            }
                            ?: extractHeading(html)
                            ?: "Chapter ${chapters.size + 1}"

                    chapters.add(
                        EpubChapter(
                            title =
                                chapterTitle,
                            content =
                                text
                        )
                    )
                }

                if (
                    chapters.isEmpty()
                ) {

                    throw IllegalStateException(
                        "No readable chapters were found in this EPUB."
                    )
                }

                return EpubDocument(
                    title = title,
                    author = author,
                    chapters = chapters
                )
            }

        } finally {

            temporaryFile.delete()
        }
    }

    private fun parseXml(
        inputStream: InputStream
    ): org.w3c.dom.Document {

        return DocumentBuilderFactory
            .newInstance()
            .apply {
                isNamespaceAware = true
            }
            .newDocumentBuilder()
            .parse(inputStream)
    }

    private fun findMetadata(
        document: org.w3c.dom.Document,
        localName: String
    ): String? {

        val nodes =
            document.getElementsByTagNameNS(
                "*",
                localName
            )

        for (i in 0 until nodes.length) {

            val value =
                nodes.item(i)
                    ?.textContent
                    ?.trim()

            if (!value.isNullOrBlank()) {
                return value
            }
        }

        return null
    }

    private fun resolvePath(
        baseDirectory: String,
        relativePath: String
    ): String {

        val decoded =
            URLDecoder.decode(
                relativePath,
                StandardCharsets.UTF_8.name()
            )

        val combined =
            if (baseDirectory.isBlank()) {
                decoded
            } else {
                "$baseDirectory/$decoded"
            }

        return normalizePath(combined)
    }

    private fun normalizePath(
        path: String
    ): String {

        val parts =
            path
                .replace('\\', '/')
                .split("/")

        val result =
            mutableListOf<String>()

        for (part in parts) {

            when (part) {

                "",
                "." -> {
                    // Ignore.
                }

                ".." -> {

                    if (result.isNotEmpty()) {
                        result.removeAt(
                            result.lastIndex
                        )
                    }
                }

                else -> {
                    result.add(part)
                }
            }
        }

        return result.joinToString("/")
    }

    private fun findZipEntry(
        zip: ZipFile,
        path: String
    ): java.util.zip.ZipEntry? {

        zip.getEntry(path)?.let {
            return it
        }

        val normalized =
            normalizePath(path)

        zip.entries()
            .asSequence()
            .forEach { entry ->

                if (
                    normalizePath(
                        entry.name
                    ) == normalized
                ) {
                    return entry
                }
            }

        return null
    }

    private fun extractHeading(
        html: String
    ): String? {

        val match =
            Regex(
                "(?is)<h[1-6][^>]*>(.*?)</h[1-6]>"
            )
                .find(html)

        return match
            ?.groupValues
            ?.getOrNull(1)
            ?.let {
                htmlToText(it)
            }
            ?.takeIf {
                it.isNotBlank()
            }
    }

    private fun htmlToText(
        html: String
    ): String {

        return html
            .replace(
                Regex(
                    "(?is)<script.*?>.*?</script>"
                ),
                ""
            )
            .replace(
                Regex(
                    "(?is)<style.*?>.*?</style>"
                ),
                ""
            )
            .replace(
                Regex(
                    "(?i)<br\\s*/?>"
                ),
                "\n"
            )
            .replace(
                Regex(
                    "(?i)</p\\s*>"
                ),
                "\n\n"
            )
            .replace(
                Regex(
                    "(?i)</div\\s*>"
                ),
                "\n\n"
            )
            .replace(
                Regex(
                    "(?i)</li\\s*>"
                ),
                "\n"
            )
            .replace(
                Regex(
                    "(?i)</h[1-6]\\s*>"
                ),
                "\n\n"
            )
            .replace(
                Regex(
                    "<[^>]+>"
                ),
                ""
            )
            .replace(
                "&nbsp;",
                " "
            )
            .replace(
                "&amp;",
                "&"
            )
            .replace(
                "&lt;",
                "<"
            )
            .replace(
                "&gt;",
                ">"
            )
            .replace(
                "&quot;",
                "\""
            )
            .replace(
                "&#39;",
                "'"
            )
            .replace(
                Regex(
                    "[ \\t]+"
                ),
                " "
            )
            .replace(
                Regex(
                    "\n{3,}"
                ),
                "\n\n"
            )
            .trim()
    }
}