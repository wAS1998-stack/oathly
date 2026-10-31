package com.oathly.app.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.oathly.app.BuildConfig
import com.oathly.app.data.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Ads, done politely:
 *  - Google's consent form (UMP) runs first wherever the law requires it.
 *  - One banner on menu screens; never on a question.
 *  - A full-screen ad only after a finished practice test, at most every 4 minutes and
 *    never after the user's first test.
 *  - Nothing at all for Pro users.
 */
class AdsManager(private val context: Context, private val settings: SettingsStore, private val scope: CoroutineScope) {

    private val started = AtomicBoolean(false)
    private val _consentReady = MutableStateFlow(false)
    private val _pro = MutableStateFlow(settings.proCached)
    private val _bannersAllowed = MutableStateFlow(false)
    /** True when a banner may be shown right now. */
    val bannersAllowed: StateFlow<Boolean> = _bannersAllowed.asStateFlow()

    private var interstitial: InterstitialAd? = null
    private var loading = false
    private lateinit var consent: ConsentInformation

    val privacyOptionsRequired: Boolean
        get() = ::consent.isInitialized &&
            consent.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    fun setPro(pro: Boolean) {
        _pro.value = pro
        if (pro) interstitial = null
        refresh()
    }

    private fun refresh() {
        _bannersAllowed.value = _consentReady.value && !_pro.value && started.get()
    }

    /** Call from the activity's onCreate. Shows the consent form when required. */
    fun gatherConsent(activity: Activity) {
        consent = UserMessagingPlatform.getConsentInformation(activity)
        val params = ConsentRequestParameters.Builder().build()
        consent.requestConsentInfoUpdate(
            activity, params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { _ ->
                    if (consent.canRequestAds()) startAds()
                }
            },
            { _ -> if (consent.canRequestAds()) startAds() },
        )
        if (consent.canRequestAds()) startAds()
    }

    fun showPrivacyOptions(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { _ ->
            if (consent.canRequestAds()) startAds()
        }
    }

    private fun startAds() {
        _consentReady.value = true
        if (started.getAndSet(true)) { refresh(); return }
        scope.launch(Dispatchers.IO) {
            MobileAds.initialize(context) {}
            scope.launch(Dispatchers.Main) {
                refresh()
                preloadInterstitial()
            }
        }
    }

    fun preloadInterstitial() {
        if (_pro.value || !started.get() || interstitial != null || loading) return
        loading = true
        InterstitialAd.load(
            context, BuildConfig.ADMOB_INTERSTITIAL_ID, AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) { interstitial = ad; loading = false }
                override fun onAdFailedToLoad(error: LoadAdError) { interstitial = null; loading = false }
            },
        )
    }

    /** Called when the user leaves a results screen. [then] always runs, ad or not. */
    fun afterTest(activity: Activity, then: () -> Unit) {
        val count = settings.testsSinceInterstitial + 1
        settings.testsSinceInterstitial = count
        val now = System.currentTimeMillis()
        val ad = interstitial
        val eligible = !_pro.value && ad != null && count >= 2 && now - settings.lastInterstitialAt > 4 * 60_000
        if (!eligible) { then(); preloadInterstitial(); return }
        ad!!.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() { interstitial = null; then(); preloadInterstitial() }
            override fun onAdFailedToShowFullScreenContent(error: AdError) { interstitial = null; then(); preloadInterstitial() }
        }
        settings.lastInterstitialAt = now
        settings.testsSinceInterstitial = 0
        ad.show(activity)
    }
}
