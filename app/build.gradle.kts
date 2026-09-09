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
        source.writeText(text)
    }
}
