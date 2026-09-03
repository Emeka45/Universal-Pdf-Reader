package com.coeric.universalreader

object UniversalReaderAds {

    lateinit var manager: AdManager
        private set

    fun initialize(
        manager: AdManager
    ) {
        this.manager = manager
    }
}