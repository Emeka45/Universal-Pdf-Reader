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

// The redesigned activity was written with Java-style Android view calls.
// Normalize two Kotlin interop issues before Kotlin compilation: Android's
// deprecated singleLine property and the UI helper's integer corner-radius API.
val fixPdfReaderSource by tasks.registering {
    doLast {
        val source = file("src/main/java/com/coeric/universalpdfreader/MainActivity.kt")
        var text = source.readText()
        listOf(0, 10, 11, 12, 14, 16).forEach { value ->
            text = text.replace(", ${value}f)", ", ${value})")
        }
        text = text.replace("singleLine = true", "setSingleLine(true)")
        source.writeText(text)
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    dependsOn(fixPdfReaderSource)
}
