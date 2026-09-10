package com.coeric.universalpdfreader

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd

class AppOpenAdManager(private val application: Application) : Application.ActivityLifecycleCallbacks {
    companion object {
        private const val APP_OPEN_AD_UNIT_ID = "ca-app-pub-2020382054968819/6677211647"
        private const val MAX_AD_AGE_MS = 4L * 60L * 60L * 1000L
        private const val MIN_SHOW_INTERVAL_MS = 30_000L
        private var instance: AppOpenAdManager? = null

        fun initialize(application: Application) {
            if (instance == null) {
                instance = AppOpenAdManager(application)
                application.registerActivityLifecycleCallbacks(instance)
            }
            instance?.loadAd()
        }
    }

    private var appOpenAd: AppOpenAd? = null
    private var isLoading = false
    private var isShowing = false
    private var firstResumeHandled = false
    private var loadedAtMs = 0L
    private var lastShownAtMs = 0L
    private var currentActivity: Activity? = null

    private fun loadAd() {
        if (isLoading || isAdAvailable()) return
        isLoading = true
        AppOpenAd.load(
            application,
            APP_OPEN_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    isLoading = false
                    appOpenAd = ad
                    loadedAtMs = System.currentTimeMillis()
                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdShowedFullScreenContent() {
                            isShowing = true
                            lastShownAtMs = System.currentTimeMillis()
                        }

                        override fun onAdDismissedFullScreenContent() {
                            isShowing = false
                            appOpenAd = null
                            loadAd()
                        }

                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            isShowing = false
                            appOpenAd = null
                            loadAd()
                        }
                    }
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    isLoading = false
                    appOpenAd = null
                }
            }
        )
    }

    private fun isAdAvailable(): Boolean =
        appOpenAd != null && System.currentTimeMillis() - loadedAtMs < MAX_AD_AGE_MS

    private fun showIfAvailable(activity: Activity) {
        if (isShowing || activity.isFinishing || activity.isDestroyed) return
        if (!firstResumeHandled) {
            firstResumeHandled = true
            return
        }
        if (System.currentTimeMillis() - lastShownAtMs < MIN_SHOW_INTERVAL_MS) return
        if (!isAdAvailable()) {
            appOpenAd = null
            loadAd()
            return
        }
        appOpenAd?.show(activity)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

    override fun onActivityStarted(activity: Activity) {
        currentActivity = activity
    }

    override fun onActivityResumed(activity: Activity) {
        currentActivity = activity
        showIfAvailable(activity)
    }

    override fun onActivityPaused(activity: Activity) = Unit

    override fun onActivityStopped(activity: Activity) {
        if (currentActivity === activity) currentActivity = null
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

    override fun onActivityDestroyed(activity: Activity) {
        if (currentActivity === activity) currentActivity = null
    }
}
