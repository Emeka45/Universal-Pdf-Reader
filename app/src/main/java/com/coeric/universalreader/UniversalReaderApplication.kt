package com.coeric.universalreader

import android.app.Application

class UniversalReaderApplication :
    Application() {

    override fun onCreate() {
        super.onCreate()

        val adManager =
            NoOpAdManager()

        adManager.initialize(
            this
        )

        UniversalReaderAds.initialize(
            adManager
        )
    }
}