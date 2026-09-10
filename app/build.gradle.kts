plugins {
    id("com.android.application")
}

android {
    namespace = "com.coeric.universalpdfreader"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.coeric.universalpdfreader"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")
}

// Normalize and patch the activity before Android's Kotlin compilation task.
tasks.named("preBuild") {
    doLast {
        val source = file("src/main/java/com/coeric/universalpdfreader/MainActivity.kt")
        var text = source.readText()

        listOf(0, 10, 11, 12, 14, 16).forEach { value ->
            text = text.replace(", ${value}f)", ", ${value})")
        }
        text = text.replace("singleLine = true", "setSingleLine(true)")

        // Android back/swipe-back returns from the reader to the in-app library
        // instead of finishing the entire Activity.
        if (!text.contains("__universalPdfReaderBackNavigation")) {
            val insertion = """

    private fun __universalPdfReaderBackNavigation() {
        if (readerPanel.visibility == View.VISIBLE) {
            closePdf()
            showLibrary()
        } else {
            super.onBackPressed()
        }
    }

    override fun onBackPressed() {
        __universalPdfReaderBackNavigation()
    }
""".trimIndent()
            val classEnd = text.lastIndexOf("\n}")
            if (classEnd >= 0) {
                text = text.substring(0, classEnd) + "\n" + insertion + text.substring(classEnd)
            }
        }

        // Replace the original search implementation with a more reliable search:
        // it normalizes PDF line-break/spacing differences, searches every page,
        // wraps from the current page, jumps to the first match, and reports the result.
        val searchStart = text.indexOf("    private fun searchPdf(query: String) {")
        if (searchStart >= 0) {
            val searchEnd = text.indexOf("\n    private fun ", searchStart + 10)
            if (searchEnd > searchStart) {
                val improvedSearch = """
    private fun searchPdf(query: String) {
        val file = pdfFile ?: run { toast("Open a PDF first"); return }
        val normalized = query.trim().replace(Regex("\\s+"), " ")
        if (normalized.isEmpty()) {
            toast("Enter a word or phrase to search")
            searchBox.requestFocus()
            return
        }

        Thread {
            var foundPage = -1
            var totalPages = 0
            try {
                PDDocument.load(file).use { document ->
                    totalPages = document.numberOfPages
                    if (totalPages == 0) {
                        runOnUiThread { toast("This PDF has no pages") }
                        return@use
                    }

                    val stripper = PDFTextStripper().apply {
                        sortByPosition = true
                    }

                    // Search from the current page first, then wrap around so the
                    // search always covers the complete document.
                    for (offset in 0 until totalPages) {
                        val pageIndex = (currentPage + offset) % totalPages
                        stripper.startPage = pageIndex + 1
                        stripper.endPage = pageIndex + 1

                        val pageText = stripper.getText(document)
                            .replace(Regex("\\s+"), " ")
                            .trim()

                        if (pageText.contains(normalized, ignoreCase = true)) {
                            foundPage = pageIndex
                            break
                        }
                    }
                }

                runOnUiThread {
                    if (foundPage >= 0) {
                        showPage(foundPage)
                        toast("Found \"$normalized\" on page ${foundPage + 1} of $totalPages")
                    } else {
                        toast("No matches found for \"$normalized\"")
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    toast("Search failed: ${e.message ?: "unable to read this PDF"}")
                }
            }
        }.start()
    }
""".trimIndent()
                text = text.substring(0, searchStart) + improvedSearch + text.substring(searchEnd)
            }
        }

        source.writeText(text)
    }
}
