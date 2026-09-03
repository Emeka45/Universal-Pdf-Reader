package com.coeric.universalreader

import android.app.Activity
import android.content.Context
import android.view.View

class NoOpAdManager : AdManager {

    override fun initialize(
        context: Context
    ) {
    }

    override fun createBanner(
        context: Context
    ): View? {
        return null
    }

    override fun showInterstitial(
        activity: Activity,
        onFinished: () -> Unit
    ) {
        onFinished()
    }

    override fun showRewarded(
        activity: Activity,
        onRewarded: () -> Unit,
        onFinished: () -> Unit
    ) {
        onFinished()
    }
}