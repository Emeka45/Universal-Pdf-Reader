package com.coeric.universalreader

import android.content.Context
import android.net.Uri
import java.nio.charset.StandardCharsets

object MarkdownReader {

    suspend fun open(
        context: Context,
        uri: Uri
    ): ReaderDocument {

        val input =
            context.contentResolver
                .openInputStream(uri)
                ?: throw IllegalStateException(
                    "Unable to open Markdown file."
                )

        val bytes =
            input.use {
                it.readBytes()
            }

        if (bytes.isEmpty()) {
            throw IllegalArgumentException(
                "The Markdown file is empty."
            )
        }

        val markdown =
            String(
                bytes,
                StandardCharsets.UTF_8
            )

        val text =
            convertMarkdownToPlainText(
                markdown
            )

        if (text.isBlank()) {
            throw IllegalArgumentException(
                "No readable text was found in this Markdown file."
            )
        }

        val title =
            detectTitle(markdown)

        return ReaderDocument(
            title =
                title ?: "Markdown Book",
            author = null,
            chapters =
                createChapters(text)
        )
    }

    private fun convertMarkdownToPlainText(
        markdown: String
    ): String {

        var text = markdown

        // Remove YAML front matter.
        text =
            text.replace(
                Regex(
                    "\\A---\\s*\\n[\\s\\S]*?\\n---\\s*\\n"
                ),
                ""
            )

        // Remove fenced code blocks.
        text =
            text.replace(
                Regex(
                    "```[\\s\\S]*?```"
                ),
                ""
            )

        // Remove inline code markers.
        text =
            text.replace(
                Regex("`([^`]*)`")
            ) {
                it.groupValues[1]
            }

        // Convert images to their alt text.
        text =
            text.replace(
                Regex(
                    "!\\[([^]]*)\\]\\([^)]*\\)"
                )
            ) {
                it.groupValues[1]
            }

        // Convert links to visible link text.
        text =
            text.replace(
                Regex(
                    "\\[([^]]+)\\]\\([^)]*\\)"
                )
            ) {
                it.groupValues[1]
            }

        // Remove heading markers.
        text =
            text.replace(
                Regex(
                    "(?m)^\\s{0,3}#{1,6}\\s+"
                ),
                ""
            )

        // Remove blockquote markers.
        text =
            text.replace(
                Regex(
                    "(?m)^\\s*>\\s?"
                ),
                ""
            )

        // Remove unordered-list markers.
        text =
            text.replace(
                Regex(
                    "(?m)^\\s*[-*+]\\s+"
                ),
                ""
            )

        // Remove ordered-list markers.
        text =
            text.replace(
                Regex(
                    "(?m)^\\s*\\d+[.)]\\s+"
                ),
                ""
            )

        // Remove horizontal rules.
        text =
            text.replace(
                Regex(
                    "(?m)^\\s*([-*_])(?:\\s*\\1){2,}\\s*$"
                ),
                ""
            )

        // Remove emphasis markers.
        text =
            text.replace(
                Regex(
                    "(\\*\\*|__)(.*?)\\1"
                )
            ) {
                it.groupValues[2]
            }

        text =
            text.replace(
                Regex(
                    "(\\*|_)(.*?)\\1"
                )
            ) {
                it.groupValues[2]
            }

        // Remove strikethrough markers.
        text =
            text.replace(
                Regex(
                    "~~(.*?)~~"
                )
            ) {
                it.groupValues[1]
            }

        // Clean remaining escaped Markdown characters.
        text =
            text.replace(
                Regex(
                    "\\\\([\\\\`*_[\\]{}()#+.!>-])"
                )
            ) {
                it.groupValues[1]
            }

        // Normalize whitespace.
        text =
            text.replace(
                "\r\n",
                "\n"
            )

        text =
            text.replace(
                "\r",
                "\n"
            )

        text =
            text.replace(
                Regex("[ \\t]+"),
                " "
            )

        text =
            text.replace(
                Regex("\\n{3,}"),
                "\n\n"
            )

        return text.trim()
    }

    private fun detectTitle(
        markdown: String
    ): String? {

        val lines =
            markdown
                .replace(
                    "\r\n",
                    "\n"
                )
                .replace(
                    "\r",
                    "\n"
                )
                .lineSequence()
                .map {
                    it.trim()
                }
                .filter {
                    it.isNotBlank()
                }
                .toList()

        // Prefer the first Markdown H1.
        val heading =
            lines.firstOrNull {
                it.startsWith("# ") &&
                    it.length > 2
            }

        if (heading != null) {
            return heading
                .removePrefix("# ")
                .trim()
        }

        // Otherwise use the first meaningful line.
        return lines.firstOrNull {
            it.length in 2..150 &&
                !it.startsWith("---")
        }
    }

    private fun createChapters(
        text: String
    ): List<ReaderChapter> {

        val paragraphs =
            text
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