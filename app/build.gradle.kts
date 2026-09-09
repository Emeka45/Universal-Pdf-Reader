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

// Normalize the redesigned activity before Android's Kotlin compilation task.
// This runs from preBuild, which is guaranteed to execute before compilation.
tasks.named("preBuild") {
    doLast {
        val source = file("src/main/java/com/coeric/universalpdfreader/MainActivity.kt")
        var text = source.readText()
        listOf(0, 10, 11, 12, 14, 16).forEach { value ->
            text = text.replace(", ${value}f)", ", ${value})")
        }
        text = text.replace("singleLine = true", "setSingleLine(true)")

        // Android back/swipe-back should leave the PDF reader and return to the
        // in-app library instead of finishing the entire reader Activity.
        // The API 33+ callback handles gesture navigation; onBackPressed keeps
        // the same behavior on older Android versions.
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

            val onCreateMarker = "        showLibrary()\n"
            val onCreateReplacement = """
        showLibrary()
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT
            ) {
                __universalPdfReaderBackNavigation()
            }
        }
""".trimIndent() + "\n"
            text = text.replace(onCreateMarker, onCreateReplacement, 1)
        }
        source.writeText(text)
    }
}
