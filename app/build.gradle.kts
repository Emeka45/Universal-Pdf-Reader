plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.coeric.universalreader"

    compileSdk = 36

    defaultConfig {
        applicationId = "com.coeric.universalreader"

        minSdk = 26
        targetSdk = 36

        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility =
            JavaVersion.VERSION_17

        targetCompatibility =
            JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {

    implementation(
        "androidx.core:core-ktx:1.17.0"
    )

    implementation(
        "androidx.activity:activity-compose:1.10.1"
    )

    /*
     * Compose BOM
     *
     * 2025.08.00 is compatible with
     * compileSdk 36 / AGP 8.13.0.
     */
    implementation(
        platform(
            "androidx.compose:compose-bom:2025.08.00"
        )
    )

    implementation(
        "androidx.compose.ui:ui"
    )

    implementation(
        "androidx.compose.ui:ui-tooling-preview"
    )

    implementation(
        "androidx.compose.foundation:foundation"
    )

    implementation(
        "androidx.compose.material3:material3"
    )

    implementation(
        "androidx.compose.material:material-icons-extended"
    )

    /*
     * CBR / RAR support
     */
    implementation(
        "com.github.junrar:junrar:8.1.0"
    )

    /*
     * PDF text extraction and search
     */
    implementation(
        "com.tom-roush:pdfbox-android:2.0.27.0"
    )

    /*
     * Compose tooling for debug builds
     */
    debugImplementation(
        "androidx.compose.ui:ui-tooling"
    )
}