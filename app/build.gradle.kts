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
    implementation("com.google.android.gms:play-services-ads:25.4.0")
}

tasks.named("preBuild") {
    doLast {
        val source = file("src/main/java/com/coeric/universalpdfreader/MainActivity.kt")
        var text = source.readText()
        listOf(0, 10, 11, 12, 14, 16).forEach { value ->
            text = text.replace(", ${value}f)", ", ${value})")
        }
        text = text.replace("singleLine = true", "setSingleLine(true)")

        if (!text.contains("com.google.android.gms.ads.MobileAds")) {
            text = text.replace(
                "import android.widget.Toast\n",
                "import android.widget.Toast\nimport android.widget.FrameLayout\nimport com.google.android.gms.ads.AdRequest\nimport com.google.android.gms.ads.AdSize\nimport com.google.android.gms.ads.AdView\nimport com.google.android.gms.ads.MobileAds\nimport com.google.android.gms.ads.AdLoader\nimport com.google.android.gms.ads.AdListener\nimport com.google.android.gms.ads.FullScreenContentCallback\nimport com.google.android.gms.ads.LoadAdError\nimport com.google.android.gms.ads.interstitial.InterstitialAd\nimport com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback\nimport com.google.android.gms.ads.nativead.MediaView\nimport com.google.android.gms.ads.nativead.NativeAd\nimport com.google.android.gms.ads.nativead.NativeAdView\n"
            )
        }

        if (!text.contains("__universalPdfReaderAdMobInitialized")) {
            text = text.replace(
                "PDFBoxResourceLoader.init(applicationContext)\n        buildUi()",
                "PDFBoxResourceLoader.init(applicationContext)\n        MobileAds.initialize(this)\n        __universalPdfReaderLoadInterstitial()\n        buildUi()"
            )
            text = text.replace(
                "class MainActivity : Activity() {",
                "class MainActivity : Activity() {\n    private var __universalPdfReaderInterstitial: InterstitialAd? = null\n    private var __universalPdfReaderOpenCount = 0"
            )
            text = text.replace(
                "private var __universalPdfReaderOpenCount = 0",
                "private var __universalPdfReaderOpenCount = 0\n    private val __universalPdfReaderAdMobInitialized = true\n    private var __universalPdfReaderNativeAd: NativeAd? = null"
            )
        } else if (!text.contains("private var __universalPdfReaderNativeAd: NativeAd?")) {
            text = text.replace(
                "private val __universalPdfReaderAdMobInitialized = true",
                "private val __universalPdfReaderAdMobInitialized = true\n    private var __universalPdfReaderNativeAd: NativeAd? = null"
            )
        }

        if (!text.contains("__universalPdfReaderAdMobBanner")) {
            val bannerCode = """

        val __universalPdfReaderAdMobBanner = AdView(this).apply {
            adUnitId = "ca-app-pub-2020382054968819/9681618668"
            val adWidth = (resources.displayMetrics.widthPixels / resources.displayMetrics.density).toInt()
            setAdSize(AdSize.getLargeAnchoredAdaptiveBannerAdSize(this@MainActivity, adWidth))
            loadAd(AdRequest.Builder().build())
            contentDescription = "Advertisement"
        }
        panel.addView(__universalPdfReaderAdMobBanner, LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = dp(8)
        })
""".trimIndent()
            val marker = "        return panel"
            val markerIndex = text.indexOf(marker)
            if (markerIndex >= 0) text = text.substring(0, markerIndex) + bannerCode + "\n" + text.substring(markerIndex)
        }

        if (!text.contains("__universalPdfReaderNativeAdContainer")) {
            val nativeAdCode = """

        val __universalPdfReaderNativeAdContainer = FrameLayout(this).apply {
            setPadding(dp(2), dp(10), dp(2), dp(4))
            background(Color.WHITE, 18)
            contentDescription = "Sponsored content"
        }
        panel.addView(__universalPdfReaderNativeAdContainer, LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = dp(12)
        })
        __universalPdfReaderLoadNativeAd(__universalPdfReaderNativeAdContainer)
""".trimIndent()
            val marker = "        return panel"
            val markerIndex = text.indexOf(marker)
            if (markerIndex >= 0) text = text.substring(0, markerIndex) + nativeAdCode + "\n" + text.substring(markerIndex)
        }

        if (!text.contains("private fun __universalPdfReaderLoadNativeAd")) {
            val nativeMethods = """

    private fun __universalPdfReaderLoadNativeAd(container: FrameLayout) {
        val adLoader = AdLoader.Builder(this, "ca-app-pub-2020382054968819/3232790650")
            .forNativeAd { nativeAd ->
                if (isFinishing || isDestroyed) {
                    nativeAd.destroy()
                    return@forNativeAd
                }
                runOnUiThread {
                    __universalPdfReaderNativeAd?.destroy()
                    __universalPdfReaderNativeAd = nativeAd

                    val adView = NativeAdView(this)
                    val card = LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(dp(12), dp(10), dp(12), dp(10))
                    }

                    val attributionRow = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                    }
                    attributionRow.addView(TextView(this).apply {
                        text = "Ad"
                        textSize = 10f
                        typeface = Typeface.DEFAULT_BOLD
                        setTextColor(Color.rgb(78, 63, 205))
                        background(Color.rgb(232, 229, 252), 6)
                        setPadding(dp(7), dp(3), dp(7), dp(3))
                    })
                    attributionRow.addView(TextView(this).apply {
                        text = "Sponsored"
                        textSize = 10f
                        setTextColor(Color.rgb(112, 115, 129))
                        setPadding(dp(7), 0, 0, 0)
                    })
                    card.addView(attributionRow, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })

                    val topRow = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                    }
                    val icon = ImageView(this).apply {
                        scaleType = ImageView.ScaleType.CENTER_CROP
                    }
                    topRow.addView(icon, LinearLayout.LayoutParams(dp(48), dp(48)))

                    val textColumn = LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(dp(10), 0, 0, 0)
                    }
                    val headline = TextView(this).apply {
                        textSize = 15f
                        typeface = Typeface.DEFAULT_BOLD
                        setTextColor(Color.rgb(30, 31, 40))
                        maxLines = 2
                        ellipsize = android.text.TextUtils.TruncateAt.END
                    }
                    val advertiser = TextView(this).apply {
                        textSize = 10f
                        setTextColor(Color.rgb(112, 115, 129))
                        setPadding(0, dp(3), 0, 0)
                    }
                    textColumn.addView(headline)
                    textColumn.addView(advertiser)
                    topRow.addView(textColumn, LinearLayout.LayoutParams(0, -2, 1f))
                    card.addView(topRow)

                    val media = MediaView(this).apply {
                        setBackgroundColor(Color.rgb(245, 246, 250))
                    }
                    card.addView(media, LinearLayout.LayoutParams(-1, dp(150)).apply { topMargin = dp(10) })

                    val body = TextView(this).apply {
                        textSize = 12f
                        setTextColor(Color.rgb(78, 81, 94))
                        maxLines = 3
                        ellipsize = android.text.TextUtils.TruncateAt.END
                        setPadding(0, dp(9), 0, 0)
                    }
                    card.addView(body)

                    val cta = Button(this).apply {
                        textSize = 11f
                        typeface = Typeface.DEFAULT_BOLD
                        setTextColor(Color.WHITE)
                        background(Color.rgb(78, 63, 205), 10)
                        setPadding(dp(12), 0, dp(12), 0)
                        minHeight = 0
                        minimumHeight = 0
                    }
                    card.addView(cta, LinearLayout.LayoutParams(-1, dp(42)).apply { topMargin = dp(10) })

                    adView.addView(card, FrameLayout.LayoutParams(-1, -2))
                    adView.headlineView = headline
                    adView.bodyView = body
                    adView.advertiserView = advertiser
                    adView.iconView = icon
                    adView.mediaView = media
                    adView.callToActionView = cta
                    adView.setNativeAd(nativeAd)

                    container.removeAllViews()
                    container.addView(adView, FrameLayout.LayoutParams(-1, -2))
                }
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    container.visibility = View.GONE
                }
            })
            .build()
        adLoader.loadAd(AdRequest.Builder().build())
    }
""".trimIndent()
            val classEnd = text.lastIndexOf("\n}")
            if (classEnd >= 0) text = text.substring(0, classEnd) + "\n" + nativeMethods + text.substring(classEnd)
        }

        if (!text.contains("__universalPdfReaderNativeAd?.destroy()")) {
            val destroyMethod = """

    override fun onDestroy() {
        __universalPdfReaderNativeAd?.destroy()
        __universalPdfReaderNativeAd = null
        super.onDestroy()
    }
""".trimIndent()
            val classEnd = text.lastIndexOf("\n}")
            if (classEnd >= 0) text = text.substring(0, classEnd) + "\n" + destroyMethod + text.substring(classEnd)
        }

        if (!text.contains("private fun __universalPdfReaderLoadInterstitial")) {
            val interstitialCode = """

    private fun __universalPdfReaderLoadInterstitial() {
        val request = AdRequest.Builder().build()
        InterstitialAd.load(
            this,
            "ca-app-pub-2020382054968819/4122170223",
            request,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    __universalPdfReaderInterstitial = ad
                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            __universalPdfReaderInterstitial = null
                            __universalPdfReaderLoadInterstitial()
                        }
                    }
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    __universalPdfReaderInterstitial = null
                }
            }
        )
    }

    private fun __universalPdfReaderMaybeShowInterstitial() {
        __universalPdfReaderOpenCount++
        if (__universalPdfReaderOpenCount % 3 == 0) {
            __universalPdfReaderInterstitial?.let { ad ->
                ad.show(this)
                __universalPdfReaderInterstitial = null
            }
        }
    }
""".trimIndent()
            val classEnd = text.lastIndexOf("\n}")
            if (classEnd >= 0) text = text.substring(0, classEnd) + "\n" + interstitialCode + text.substring(classEnd)
            text = text.replace(
                "MobileAds.initialize(this)\n        __universalPdfReaderLoadInterstitial()\n        buildUi()",
                "MobileAds.initialize(this)\n        __universalPdfReaderLoadInterstitial()\n        buildUi()"
            )
        }

        if (!text.contains("__universalPdfReaderMaybeShowInterstitial()")) {
            val candidates = listOf(
                "        showReader()\n        showPage(0)",
                "        showReader()\n        showPage(currentPage)"
            )
            for (candidate in candidates) {
                if (text.contains(candidate)) {
                    val markerIndex = text.indexOf(candidate)
                    val insertion = candidate + "\n        __universalPdfReaderMaybeShowInterstitial()"
                    text = text.substring(0, markerIndex) + insertion + text.substring(markerIndex + candidate.length)
                    break
                }
            }
        }

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
            if (classEnd >= 0) text = text.substring(0, classEnd) + "\n" + insertion + text.substring(classEnd)
        }

        val searchStart = text.indexOf("    private fun searchPdf(query: String) {")
        if (searchStart >= 0) {
            val searchEnd = text.indexOf("\n    private fun ", searchStart + 10)
            if (searchEnd > searchStart) {
                val improvedSearch = """
    private fun searchPdf(query: String) {
        val file = pdfFile ?: run { toast("Open a PDF first"); return }
        val normalized = query.trim().replace(Regex("\\s+"), " ")
        if (normalized.isEmpty()) { toast("Enter a word or phrase to search"); searchBox.requestFocus(); return }
        Thread {
            var foundPage = -1
            var totalPages = 0
            try {
                PDDocument.load(file).use { document ->
                    totalPages = document.numberOfPages
                    val stripper = PDFTextStripper().apply { sortByPosition = true }
                    for (offset in 0 until totalPages) {
                        val pageIndex = (currentPage + offset) % totalPages
                        stripper.startPage = pageIndex + 1
                        stripper.endPage = pageIndex + 1
                        val pageText = stripper.getText(document).replace(Regex("\\s+"), " ").trim()
                        if (pageText.contains(normalized, ignoreCase = true)) { foundPage = pageIndex; break }
                    }
                }
                runOnUiThread {
                    if (foundPage >= 0) { showPage(foundPage); toast("Found \"" + normalized + "\" on page " + (foundPage + 1) + " of " + totalPages) }
                    else toast("No matches found for \"" + normalized + "\"")
                }
            } catch (e: Exception) { runOnUiThread { toast("Search failed: " + (e.message ?: "unable to read this PDF")) } }
        }.start()
    }
""".trimIndent()
                text = text.substring(0, searchStart) + improvedSearch + text.substring(searchEnd)
            }
        }

        // Improve swipe navigation while keeping pinch zoom and one-finger panning.
        if (!text.contains("__universalPdfReaderSwipeStartX")) {
            text = text.replace(
                "    private var isPinching = false\n",
                "    private var isPinching = false\n    private var __universalPdfReaderSwipeStartX = 0f\n    private var __universalPdfReaderSwipeStartY = 0f\n"
            )
            text = text.replace(
                "                lastTouchX = event.x\n                lastTouchY = event.y\n                isPanning = false",
                "                lastTouchX = event.x\n                lastTouchY = event.y\n                __universalPdfReaderSwipeStartX = event.x\n                __universalPdfReaderSwipeStartY = event.y\n                isPanning = false"
            )
            val oldUp = """            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                pinchDistance = 0f
                isPinching = false
                isPanning = false
                return true
            }"""
            val newUp = """            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (event.actionMasked == MotionEvent.ACTION_UP && zoom <= 1.01f && !isPanning && !isPinching) {
                    val dx = event.x - __universalPdfReaderSwipeStartX
                    val dy = event.y - __universalPdfReaderSwipeStartY
                    if (kotlin.math.abs(dx) >= dp(72) && kotlin.math.abs(dx) > kotlin.math.abs(dy) * 1.35f) {
                        if (dx < 0f) showPage(currentPage + 1) else showPage(currentPage - 1)
                    }
                }
                pinchDistance = 0f
                isPinching = false
                isPanning = false
                return true
            }"""
            text = text.replace(oldUp, newUp)
        }

        // Premium visual pass: stronger indigo identity, cleaner surfaces, and a richer reader canvas.
        text = text.replace("Color.rgb(246, 247, 251)", "Color.rgb(244, 246, 252)")
        text = text.replace("Color.rgb(229, 231, 237)", "Color.rgb(218, 222, 236)")
        text = text.replace("Color.rgb(241, 240, 251)", "Color.rgb(232, 229, 252)")
        text = text.replace("Color.rgb(238, 236, 252)", "Color.rgb(226, 222, 252)")
        text = text.replace("Color.rgb(67, 56, 180)", "Color.rgb(78, 63, 205)")
        text = text.replace("text = \"Read beautifully. Anywhere.\"", "text = \"Read. Search. Swipe.\"")

        // Render PDF pages in print-quality mode for stronger color/graphics fidelity.
        text = text.replace(
            "PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY",
            "PdfRenderer.Page.RENDER_MODE_FOR_PRINT"
        )
        text = text.replace("coerceIn(900, 2200)", "coerceIn(1100, 2200)")

        source.writeText(text)
    }
}
