package com.coeric.universalpdfreader

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.appopen.AppOpenAd
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner

class UniversalPdfReaderApplication : Application(), Application.ActivityLifecycleCallbacks, DefaultLifecycleObserver {
    companion object {
        private const val APP_OPEN_AD_UNIT_ID = "ca-app-pub-2020382054968819/6677211647"
        private const val MAX_AD_AGE_MS = 4L * 60L * 60L * 1000L
        private const val MIN_SHOW_INTERVAL_MS = 30_000L
    }

    private var currentActivity: Activity? = null
    private var appOpenAd: AppOpenAd? = null
    private var isLoadingAd = false
    private var isShowingAd = false
    private var firstForegroundHandled = false
    private var loadedAtMs = 0L
    private var lastShownAtMs = 0L

    override fun onCreate() {
        super.onCreate()
        MobileAds.initialize(this)
        registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        loadAppOpenAd()
    }

    private fun loadAppOpenAd() {
        if (isLoadingAd || isAdAvailable()) return
        isLoadingAd = true
        AppOpenAd.load(
            this,
            APP_OPEN_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    isLoadingAd = false
                    appOpenAd = ad
                    loadedAtMs = System.currentTimeMillis()
                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            isShowingAd = false
                            appOpenAd = null
                            loadAppOpenAd()
                        }

                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            isShowingAd = false
                            appOpenAd = null
                            loadAppOpenAd()
                        }

                        override fun onAdShowedFullScreenContent() {
                            isShowingAd = true
                            lastShownAtMs = System.currentTimeMillis()
                        }
                    }
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    isLoadingAd = false
                    appOpenAd = null
                }
            }
        )
    }

    private fun isAdAvailable(): Boolean {
        return appOpenAd != null &&
            System.currentTimeMillis() - loadedAtMs < MAX_AD_AGE_MS
    }

    private fun showAppOpenAdIfAvailable() {
        val activity = currentActivity ?: return
        if (isShowingAd) return
        if (!firstForegroundHandled) {
            firstForegroundHandled = true
            return
        }
        if (System.currentTimeMillis() - lastShownAtMs < MIN_SHOW_INTERVAL_MS) return

        if (!isAdAvailable()) {
            appOpenAd = null
            loadAppOpenAd()
            return
        }

        appOpenAd?.show(activity)
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        showAppOpenAdIfAvailable()
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

    override fun onActivityStarted(activity: Activity) {
        currentActivity = activity
    }

    override fun onActivityResumed(activity: Activity) {
        currentActivity = activity
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
