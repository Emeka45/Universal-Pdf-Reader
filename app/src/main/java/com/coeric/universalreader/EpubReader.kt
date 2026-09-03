package com.coeric.universalreader

import android.content.Context
import android.net.Uri
import java.io.File
import java.nio.charset.Charset
import java.util.zip.ZipFile
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

object EpubReader {

    private data class ManifestItem(
        val id: String,
        val href: String,
        val mediaType: String?,
        val title: String?
    )

    private data class ParsedPackage(
        val title: String,
        val author: String?,
        val chapters: List<EpubChapter>,
        val images: List<EpubImage>
    )

    suspend fun open(
        context: Context,
        uri: Uri
    ): EpubDocument {

        val temporaryFile =
            File.createTempFile(
                "universal_epub_",
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

                            input.copyTo(
                                output,
                                bufferSize = 64 * 1024
                            )
                        }
                }
                ?: throw IllegalArgumentException(
                    "Unable to open EPUB."
                )

            return ZipFile(temporaryFile).use { zip ->

                val containerEntry =
                    zip.getEntry(
                        "META-INF/container.xml"
                    )
                        ?: throw IllegalArgumentException(
                            "Invalid EPUB: container.xml not found."
                        )

                val containerXml =
                    zip.getInputStream(
                        containerEntry
                    ).use {
                        readText(it.readBytes())
                    }

                val opfPath =
                    findRootfilePath(
                        containerXml
                    )

                val opfEntry =
                    zip.getEntry(opfPath)
                        ?: throw IllegalArgumentException(
                            "Invalid EPUB: OPF file not found."
                        )

                val opfXml =
                    zip.getInputStream(
                        opfEntry
                    ).use {
                        readText(it.readBytes())
                    }

                val basePath =
                    opfPath
                        .substringBeforeLast(
                            '/',
                            ""
                        )

                val parsed =
                    parsePackage(
                        zip = zip,
                        opfXml = opfXml,
                        basePath = basePath
                    )

                EpubDocument(
                    title = parsed.title,
                    author = parsed.author,
                    chapters = parsed.chapters,
                    images = parsed.images
                )
            }

        } finally {
            temporaryFile.delete()
        }
    }

    private fun findRootfilePath(
        xml: String
    ): String {

        val parser =
            createParser(xml)

        var rootfilePath: String? = null

        while (
            parser.eventType !=
                XmlPullParser.END_DOCUMENT
        ) {

            if (
                parser.eventType ==
                    XmlPullParser.START_TAG &&
                parser.name.equals(
                    "rootfile",
                    ignoreCase = true
                )
            ) {

                rootfilePath =
                    parser.getAttributeValue(
                        null,
                        "full-path"
                    )

                if (
                    !rootfilePath.isNullOrBlank()
                ) {
                    break
                }
            }

            parser.next()
        }

        return rootfilePath
            ?: throw IllegalArgumentException(
                "Invalid EPUB: rootfile not found."
            )
    }

    private fun parsePackage(
        zip: ZipFile,
        opfXml: String,
        basePath: String
    ): ParsedPackage {

        val parser =
            createParser(opfXml)

        var title =
            "Untitled"

        var author: String? =
            null

        val manifest =
            linkedMapOf<String, ManifestItem>()

        val spine =
            mutableListOf<String>()

        var currentMetadataTag: String? =
            null

        while (
            parser.eventType !=
                XmlPullParser.END_DOCUMENT
        ) {

            when (parser.eventType) {

                XmlPullParser.START_TAG -> {

                    when {

                        parser.name.equals(
                            "title",
                            ignoreCase = true
                        ) -> {

                            currentMetadataTag =
                                "title"

                            val text =
                                parser.nextText()
                                    .trim()

                            if (text.isNotBlank()) {
                                title = text
                            }
                        }

                        parser.name.equals(
                            "creator",
                            ignoreCase = true
                        ) -> {

                            currentMetadataTag =
                                "creator"

                            val text =
                                parser.nextText()
                                    .trim()

                            if (text.isNotBlank()) {
                                author = text
                            }
                        }

                        parser.name.equals(
                            "item",
                            ignoreCase = true
                        ) -> {

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

                            val mediaType =
                                parser.getAttributeValue(
                                    null,
                                    "media-type"
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
                                            decodeUrl(
                                                href
                                            ),
                                        mediaType =
                                            mediaType,
                                        title =
                                            itemTitle
                                    )
                            }
                        }

                        parser.name.equals(
                            "itemref",
                            ignoreCase = true
                        ) -> {

                            val idref =
                                parser.getAttributeValue(
                                    null,
                                    "idref"
                                )

                            if (
                                !idref.isNullOrBlank()
                            ) {
                                spine.add(idref)
                            }
                        }
                    }
                }

                XmlPullParser.END_TAG -> {

                    if (
                        parser.name.equals(
                            currentMetadataTag,
                            ignoreCase = true
                        )
                    ) {
                        currentMetadataTag = null
                    }
                }
            }

            parser.next()
        }

        val chapters =
            mutableListOf<EpubChapter>()

        for (id in spine) {

            val item =
                manifest[id]
                    ?: continue

            val chapterPath =
                combinePaths(
                    basePath,
                    item.href
                )

            val entry =
                zip.getEntry(chapterPath)
                    ?: continue

            val html =
                zip.getInputStream(entry).use {
                    readText(
                        it.readBytes()
                    )
                }

            val cleanedText =
                htmlToText(html)

            val chapterTitle =
                item.title
                    ?.takeIf {
                        it.isNotBlank()
                    }
                    ?: extractChapterTitle(
                        html
                    )
                    ?: "Chapter ${chapters.size + 1}"

            chapters.add(
                EpubChapter(
                    title = chapterTitle,
                    content = cleanedText,
                    originalHtml = html
                )
            )
        }

        val images =
            extractImages(
                zip = zip,
                manifest = manifest,
                basePath = basePath
            )

        return ParsedPackage(
            title = title,
            author = author,
            chapters = chapters,
            images = images
        )
    }

    private fun extractImages(
        zip: ZipFile,
        manifest: Map<String, ManifestItem>,
        basePath: String
    ): List<EpubImage> {

        val images =
            mutableListOf<EpubImage>()

        for (item in manifest.values) {

            val mediaType =
                item.mediaType
                    ?.lowercase()
                    ?: continue

            if (
                !mediaType.startsWith(
                    "image/"
                )
            ) {
                continue
            }

            val path =
                combinePaths(
                    basePath,
                    item.href
                )

            val entry =
                zip.getEntry(path)
                    ?: continue

            val data =
                zip.getInputStream(entry)
                    .use {
                        it.readBytes()
                    }

            if (data.isEmpty()) {
                continue
            }

            images.add(
                EpubImage(
                    path = path,
                    mimeType = mediaType,
                    data = data
                )
            )
        }

        return images
    }

    private fun htmlToText(
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
                    "(?i)</?(p|div|section|article|"
                        + "blockquote|li|h1|h2|h3|h4|h5|h6|"
                        + "tr|br)[^>]*>"
                ),
                "\n"
            )
            .replace(
                Regex(
                    "(?is)<[^>]+>"
                ),
                ""
            )
            .let {
                decodeHtmlEntities(it)
            }
            .replace(
                Regex(
                    "[ \\t]+"
                ),
                " "
            )
            .replace(
                Regex(
                    "\\n[ \\t]+"
                ),
                "\n"
            )
            .replace(
                Regex(
                    "\\n{3,}"
                ),
                "\n\n"
            )
            .trim()
    }

    private fun extractChapterTitle(
        html: String
    ): String? {

        val heading =
            Regex(
                "(?is)<h[1-6][^>]*>(.*?)</h[1-6]>"
            )
                .find(html)
                ?.groupValues
                ?.getOrNull(1)

        return heading
            ?.let {
                htmlToText(it)
            }
            ?.trim()
            ?.takeIf {
                it.isNotBlank()
            }
    }

    private fun decodeHtmlEntities(
        text: String
    ): String {

        var result = text

        val entities =
            mapOf(
                "&nbsp;" to " ",
                "&amp;" to "&",
                "&lt;" to "<",
                "&gt;" to ">",
                "&quot;" to "\"",
                "&apos;" to "'",
                "&copy;" to "©",
                "&reg;" to "®",
                "&hellip;" to "…",
                "&mdash;" to "—",
                "&ndash;" to "–",
                "&ldquo;" to "“",
                "&rdquo;" to "”",
                "&lsquo;" to "‘",
                "&rsquo;" to "’"
            )

        for (
            (entity, value) in entities
        ) {
            result =
                result.replace(
                    entity,
                    value
                )
        }

        result =
            Regex(
                "&#(x?[0-9A-Fa-f]+);"
            )
                .replace(result) { match ->

                    try {

                        val raw =
                            match
                                .groupValues[1]

                        val code =
                            if (
                                raw.startsWith(
                                    "x",
                                    ignoreCase = true
                                )
                            ) {
                                raw
                                    .substring(1)
                                    .toInt(16)
                            } else {
                                raw.toInt()
                            }

                        String(
                            Character
                                .toChars(code)
                        )

                    } catch (
                        exception: Exception
                    ) {
                        match.value
                    }
                }

        return result
    }

    private fun combinePaths(
        base: String,
        child: String
    ): String {

        val combined =
            if (base.isBlank()) {
                child
            } else {
                "$base/$child"
            }

        val parts =
            combined
                .replace(
                    '\\',
                    '/'
                )
                .split("/")

        val normalized =
            mutableListOf<String>()

        for (part in parts) {

            when {

                part.isBlank() ||
                    part == "." -> Unit

                part == ".." -> {

                    if (normalized.isNotEmpty()) {
                        normalized.removeAt(
                            normalized.lastIndex
                        )
                    }
                }

                else ->
                    normalized.add(part)
            }
        }

        return normalized.joinToString("/")
    }

    private fun decodeUrl(
        value: String
    ): String {

        return try {
            java.net.URLDecoder.decode(
                value,
                "UTF-8"
            )
        } catch (
            exception: Exception
        ) {
            value
        }
    }

    private fun createParser(
        xml: String
    ): XmlPullParser {

        val factory =
            XmlPullParserFactory
                .newInstance()

        factory.isNamespaceAware = true

        return factory
            .newPullParser()
            .also {
                it.setInput(
                    xml.reader()
                )
            }
    }

    private fun readText(
        bytes: ByteArray
    ): String {

        if (
            bytes.size >= 3 &&
            bytes[0] == 0xEF.toByte() &&
            bytes[1] == 0xBB.toByte() &&
            bytes[2] == 0xBF.toByte()
        ) {
            return String(
                bytes,
                3,
                bytes.size - 3,
                Charsets.UTF_8
            )
        }

        if (
            bytes.size >= 2 &&
            bytes[0] == 0xFF.toByte() &&
            bytes[1] == 0xFE.toByte()
        ) {
            return String(
                bytes,
                2,
                bytes.size - 2,
                Charsets.UTF_16LE
            )
        }

        if (
            bytes.size >= 2 &&
            bytes[0] == 0xFE.toByte() &&
            bytes[1] == 0xFF.toByte()
        ) {
            return String(
                bytes,
                2,
                bytes.size - 2,
                Charsets.UTF_16BE
            )
        }

        return try {

            val utf8 =
                String(
                    bytes,
                    Charsets.UTF_8
                )

            if (
                utf8.contains(
                    '\uFFFD'
                )
            ) {
                String(
                    bytes,
                    Charset.forName(
                        "windows-1252"
                    )
                )
            } else {
                utf8
            }

        } catch (
            exception: Exception
        ) {
            String(
                bytes,
                Charset.forName(
                    "windows-1252"
                )
            )
        }
    }
}