package com.coeric.universalreader

import android.content.Context
import android.net.Uri
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.util.zip.ZipFile
import java.io.File
import java.nio.charset.Charset

object EpubReader {

    suspend fun open(
        context: Context,
        uri: Uri
    ): EpubDocument {

        val temporaryFile =
            File.createTempFile(
                "universal_reader_",
                ".epub",
                context.cacheDir
            )

        try {

            copyUriToFile(
                context,
                uri,
                temporaryFile
            )

            ZipFile(
                temporaryFile
            ).use { zip ->

                val containerEntry =
                    zip.getEntry(
                        "META-INF/container.xml"
                    )
                        ?: throw IllegalArgumentException(
                            "Invalid EPUB: container.xml not found."
                        )

                val rootFile =
                    zip.getInputStream(
                        containerEntry
                    ).use {
                        findRootFile(it)
                    }

                val opfEntry =
                    zip.getEntry(rootFile)
                        ?: throw IllegalArgumentException(
                            "Invalid EPUB: package file not found."
                        )

                val opf =
                    zip.getInputStream(
                        opfEntry
                    ).use {
                        parseOpf(it)
                    }

                val basePath =
                    rootFile
                        .substringBeforeLast(
                            '/',
                            ""
                        )

                val chapters =
                    mutableListOf<EpubChapter>()

                for (
                    item in opf.spine
                ) {

                    val manifestItem =
                        opf.manifest[item]

                    if (
                        manifestItem == null
                    ) {
                        continue
                    }

                    val fullPath =
                        if (
                            basePath.isBlank()
                        ) {
                            manifestItem.href
                        } else {
                            "$basePath/${manifestItem.href}"
                        }

                    val normalizedPath =
                        normalizePath(
                            fullPath
                        )

                    val entry =
                        zip.getEntry(
                            normalizedPath
                        )
                            ?: continue

                    val content =
                        zip.getInputStream(
                            entry
                        ).use {
                            readHtmlContent(it)
                        }

                    if (
                        content.isBlank()
                    ) {
                        continue
                    }

                    val chapterTitle =
                        extractChapterTitle(
                            content
                        )
                            .ifBlank {
                                manifestItem.title
                                    ?: "Chapter ${chapters.size + 1}"
                            }

                    chapters.add(
                        EpubChapter(
                            title =
                                chapterTitle,

                            content =
                                content
                        )
                    )
                }

                if (chapters.isEmpty()) {

                    throw IllegalArgumentException(
                        "No readable chapters were found in this EPUB."
                    )
                }

                return EpubDocument(
                    title =
                        opf.title
                            ?: getDocumentName(
                                context,
                                uri
                            ),

                    author =
                        opf.author,

                    chapters =
                        chapters
                )
            }

        } finally {

            temporaryFile.delete()
        }
    }

    private fun copyUriToFile(
        context: Context,
        uri: Uri,
        destination: File
    ) {

        val input =
            context.contentResolver
                .openInputStream(uri)
                ?: throw IllegalArgumentException(
                    "Unable to open EPUB file."
                )

        input.use { stream ->

            destination
                .outputStream()
                .use { output ->

                    stream.copyTo(
                        output,
                        bufferSize = 64 * 1024
                    )
                }
        }
    }

    private fun findRootFile(
        input: InputStream
    ): String {

        val parser =
            android.util.Xml.newPullParser()

        parser.setInput(
            input,
            null
        )

        var event =
            parser.eventType

        while (
            event != XmlPullParser.END_DOCUMENT
        ) {

            if (
                event ==
                XmlPullParser.START_TAG &&
                parser.name.equals(
                    "rootfile",
                    ignoreCase = true
                )
            ) {

                val fullPath =
                    parser.getAttributeValue(
                        null,
                        "full-path"
                    )

                if (
                    !fullPath.isNullOrBlank()
                ) {
                    return fullPath
                }
            }

            event =
                parser.next()
        }

        throw IllegalArgumentException(
            "Invalid EPUB: package path not found."
        )
    }

    private fun parseOpf(
        input: InputStream
    ): EpubPackage {

        val parser =
            android.util.Xml.newPullParser()

        parser.setInput(
            input,
            null
        )

        var title: String? = null
        var author: String? = null

        val manifest =
            linkedMapOf<String, ManifestItem>()

        val spine =
            mutableListOf<String>()

        var event =
            parser.eventType

        while (
            event != XmlPullParser.END_DOCUMENT
        ) {

            if (
                event ==
                XmlPullParser.START_TAG
            ) {

                when (
                    parser.name.lowercase()
                ) {

                    "dc:title",
                    "title" -> {

                        if (
                            title.isNullOrBlank()
                        ) {

                            title =
                                readText(
                                    parser
                                )
                        }
                    }

                    "dc:creator",
                    "creator" -> {

                        if (
                            author.isNullOrBlank()
                        ) {

                            author =
                                readText(
                                    parser
                                )
                        }
                    }

                    "item" -> {

                        val id =
                            parser.getAttributeValue(
                                null,
                                "id"
                            )

                        val href =
                            parser.getAttributeValue(
                                null,
                                "href"
                            )

                        val itemTitle =
                            parser.getAttributeValue(
                                null,
                                "title"
                            )

                        if (
                            !id.isNullOrBlank() &&
                            !href.isNullOrBlank()
                        ) {

                            manifest[id] =
                                ManifestItem(
                                    id = id,
                                    href =
                                        decodeHref(
                                            href
                                        ),
                                    title =
                                        itemTitle
                                )
                        }
                    }

                    "itemref" -> {

                        val idref =
                            parser.getAttributeValue(
                                null,
                                "idref"
                            )

                        if (
                            !idref.isNullOrBlank()
                        ) {

                            spine.add(
                                idref
                            )
                        }
                    }
                }
            }

            event =
                parser.next()
        }

        return EpubPackage(
            title = title,
            author = author,
            manifest = manifest,
            spine = spine
        )
    }

    private fun readText(
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
                event ==
                XmlPullParser.END_TAG &&
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

    private fun readHtmlContent(
        input: InputStream
    ): String {

        val bytes =
            input.readBytes()

        val decoded =
            decodeHtml(
                bytes
            )

        return convertHtmlToText(
            decoded
        )
    }

    private fun decodeHtml(
        bytes: ByteArray
    ): String {

        val utf8 =
            try {
                String(
                    bytes,
                    Charsets.UTF_8
                )
            } catch (
                exception: Exception
            ) {
                ""
            }

        if (
            utf8.contains(
                '<'
            ) &&
            utf8.count {
                it == '\uFFFD'
            } < 10
        ) {
            return utf8
        }

        return String(
            bytes,
            Charset.forName(
                "windows-1252"
            )
        )
    }

    private fun convertHtmlToText(
        html: String
    ): String {

        return html

            .replace(
                Regex(
                    "(?is)<script[^>]*>.*?</script>"
                ),
                ""
            )

            .replace(
                Regex(
                    "(?is)<style[^>]*>.*?</style>"
                ),
                ""
            )

            .replace(
                Regex(
                    "(?is)<head[^>]*>.*?</head>"
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
                    "(?i)<li[^>]*>"
                ),
                "• "
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
                    "&#(\\d+);"
                )
            ) { match ->

                match.groupValues[1]
                    .toIntOrNull()
                    ?.let {
                        String(
                            Character.toChars(
                                it
                            )
                        )
                    }
                    ?: match.value
            }

            .let {
                cleanText(it)
            }
    }

    private fun extractChapterTitle(
        content: String
    ): String {

        val lines =
            content
                .split('\n')
                .map {
                    it.trim()
                }
                .filter {
                    it.isNotBlank()
                }

        if (lines.isEmpty()) {
            return ""
        }

        val first =
            lines.first()

        if (
            first.length <= 120
        ) {
            return first
        }

        return ""
    }

    private fun decodeHref(
        href: String
    ): String {

        return try {

            java.net.URLDecoder.decode(
                href,
                "UTF-8"
            )

        } catch (
            exception: Exception
        ) {

            href
        }
    }

    private fun normalizePath(
        path: String
    ): String {

        val parts =
            path.split('/')

        val result =
            mutableListOf<String>()

        for (
            part in parts
        ) {

            when {

                part.isBlank() ||
                part == "." -> {
                    continue
                }

                part == ".." -> {

                    if (
                        result.isNotEmpty()
                    ) {
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

        return result.joinToString(
            "/"
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

    private fun getDocumentName(
        context: Context,
        uri: Uri
    ): String {

        var name =
            "EPUB Document"

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

                if (
                    cursor.moveToFirst()
                ) {

                    val index =
                        cursor.getColumnIndex(
                            android.provider.OpenableColumns.DISPLAY_NAME
                        )

                    if (
                        index >= 0
                    ) {

                        name =
                            cursor.getString(
                                index
                            )
                    }
                }
            }

        return name.substringBeforeLast(
            '.',
            name
        )
    }

    private data class EpubPackage(
        val title: String?,
        val author: String?,
        val manifest:
            Map<String, ManifestItem>,
        val spine: List<String>
    )

    private data class ManifestItem(
        val id: String,
        val href: String,
        val title: String?
    )
}