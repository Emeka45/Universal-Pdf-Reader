package com.coeric.universalreader

import android.app.Activity
import android.content.Context
import android.view.View

interface AdManager {

    fun initialize(
        context: Context
    )

    fun createBanner(
        context: Context
    ): View?

    fun showInterstitial(
        activity: Activity,
        onFinished: () -> Unit = {}
    )

    fun showRewarded(
        activity: Activity,
        onRewarded: () -> Unit,
        onFinished: () -> Unit = {}
    )
}