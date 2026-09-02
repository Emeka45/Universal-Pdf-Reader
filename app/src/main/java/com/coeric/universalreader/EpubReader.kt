package com.coeric.universalreader

import android.content.Context
import android.net.Uri
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.InputStream
import java.net.URLDecoder
import java.nio.charset.Charset
import java.util.zip.ZipFile

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
                    rootFile.substringBeforeLast(
                        '/',
                        ""
                    )

                val chapters =
                    mutableListOf<EpubChapter>()

                for (
                    spineId in opf.spine
                ) {

                    val item =
                        opf.manifest[spineId]
                            ?: continue

                    val fullPath =
                        combinePaths(
                            basePath,
                            item.href
                        )

                    val entry =
                        zip.getEntry(
                            fullPath
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

                    val title =
                        extractChapterTitle(
                            content
                        ).ifBlank {
                            item.title
                                ?: "Chapter ${
                                    chapters.size + 1
                                }"
                        }

                    chapters.add(
                        EpubChapter(
                            title = title,
                            content = content
                        )
                    )
                }

                if (
                    chapters.isEmpty()
                ) {

                    throw IllegalArgumentException(
                        "No readable chapters were found in this EPUB."
                    )
                }

                val images =
                    extractImages(
                        zip = zip,
                        basePath = basePath,
                        manifest = opf.manifest
                    )

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
                        chapters,

                    images =
                        images
                )
            }

        } finally {

            temporaryFile.delete()
        }
    }

    private fun extractImages(
        zip: ZipFile,
        basePath: String,
        manifest: Map<String, ManifestItem>
    ): List<EpubImage> {

        val images =
            mutableListOf<EpubImage>()

        for (
            item in manifest.values
        ) {

            val path =
                combinePaths(
                    basePath,
                    item.href
                )

            val extension =
                path
                    .substringAfterLast(
                        '.',
                        ""
                    )
                    .lowercase()

            val mimeType =
                item.mediaType
                    ?: imageMimeType(
                        extension
                    )

            val isImage =
                mimeType
                    ?.lowercase()
                    ?.startsWith(
                        "image/"
                    ) == true ||
                    extension in setOf(
                        "jpg",
                        "jpeg",
                        "png",
                        "gif",
                        "webp",
                        "bmp",
                        "svg",
                        "avif"
                    )

            if (
                !isImage
            ) {
                continue
            }

            val entry =
                zip.getEntry(path)
                    ?: continue

            try {

                val data =
                    zip.getInputStream(
                        entry
                    ).use {
                        it.readBytes()
                    }

                if (
                    data.isNotEmpty()
                ) {

                    images.add(
                        EpubImage(
                            path = path,
                            mimeType = mimeType,
                            data = data
                        )
                    )
                }

            } catch (
                exception: Exception
            ) {

                // Ignore a damaged image and
                // continue loading the EPUB.
            }
        }

        return images
    }

    private fun imageMimeType(
        extension: String
    ): String? {

        return when (extension) {

            "jpg",
            "jpeg" ->
                "image/jpeg"

            "png" ->
                "image/png"

            "gif" ->
                "image/gif"

            "webp" ->
                "image/webp"

            "bmp" ->
                "image/bmp"

            "svg" ->
                "image/svg+xml"

            "avif" ->
                "image/avif"

            else ->
                null
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
            Xml.newPullParser()

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
            Xml.newPullParser()

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
                    parser.name
                        .substringAfterLast(':')
                        .lowercase()
                ) {

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

                        val mediaType =
                            parser.getAttributeValue(
                                null,
                                "media-type"
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
                                        itemTitle,
                                    mediaType =
                                        mediaType
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

        val depth =
            parser.depth

        var event =
            parser.next()

        while (
            event != XmlPullParser.END_DOCUMENT
        ) {

            if (
                event ==
                    XmlPullParser.END_TAG &&
                parser.depth < depth
            ) {
                break
            }

            if (
                event ==
                    XmlPullParser.TEXT ||
                event ==
                    XmlPullParser.CDSECT
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

        val html =
            decodeHtml(
                bytes
            )

        return convertHtmlToText(
            html
        )
    }

    private fun decodeHtml(
        bytes: ByteArray
    ): String {

        val utf8 =
            String(
                bytes,
                Charsets.UTF_8
            )

        val replacementCount =
            utf8.count {
                it == '\uFFFD'
            }

        if (
            replacementCount < 10
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
                            Character.toChars(it)
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

        if (
            lines.isEmpty()
        ) {
            return ""
        }

        val first =
            lines.first()

        return if (
            first.length <= 120
        ) {
            first
        } else {
            ""
        }
    }

    private fun decodeHref(
        href: String
    ): String {

        return try {

            URLDecoder.decode(
                href,
                "UTF-8"
            )

        } catch (
            exception: Exception
        ) {

            href
        }
    }

    private fun combinePaths(
        base: String,
        child: String
    ): String {

        return normalizePath(
            if (
                base.isBlank()
            ) {
                child
            } else {
                "$base/$child"
            }
        )
    }

    private fun normalizePath(
        path: String
    ): String {

        val result =
            mutableListOf<String>()

        for (
            part in path.split('/')
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

        return result.joinToString("/")
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
                Regex("[ \\t]+"),
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
        val title: String?,
        val mediaType: String?
    )
}