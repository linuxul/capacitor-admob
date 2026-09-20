package com.getcapacitor.community.admob

import android.Manifest
import com.getcapacitor.JSArray
import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin
import com.getcapacitor.annotation.Permission
import com.getcapacitor.community.admob.appopen.AppOpenAdPlugin
import com.getcapacitor.community.admob.banner.BannerExecutor
import com.getcapacitor.community.admob.consent.AdConsentExecutor
import com.getcapacitor.community.admob.helpers.AuthorizationStatusEnum
import com.getcapacitor.community.admob.interstitial.AdInterstitialExecutor
import com.getcapacitor.community.admob.interstitial.InterstitialAdCallbackAndListeners
import com.getcapacitor.community.admob.rewarded.AdRewardExecutor
import com.getcapacitor.community.admob.rewardedinterstitial.AdRewardInterstitialExecutor
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.common.util.BiConsumer
import org.json.JSONException

@CapacitorPlugin(
    permissions = [Permission(alias = "network", strings = [Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.INTERNET])]
)
public class AdMob : Plugin() {
    private val notifyListenersFunction = BiConsumer<String, JSObject> { eventName, data -> notifyListeners(eventName, data) }

    private val appOpenNotifier = AppOpenAdPlugin.EventNotifier { eventName, data -> notifyListeners(eventName, data) }

    private val bannerExecutor = BannerExecutor({ context }, { activity }, notifyListenersFunction, logTag)

    private val adRewardExecutor = AdRewardExecutor({ context }, { activity }, notifyListenersFunction, logTag)

    private val adRewardInterstitialExecutor = AdRewardInterstitialExecutor({ context }, { activity }, notifyListenersFunction, logTag)

    private val adInterstitialExecutor =
        AdInterstitialExecutor({ context }, { activity }, notifyListenersFunction, logTag, InterstitialAdCallbackAndListeners)

    private val adConsentExecutor = AdConsentExecutor({ context }, { activity }, notifyListenersFunction, logTag)

    private val appOpenAdPlugin = AppOpenAdPlugin()

    @PluginMethod
    public fun loadAppOpen(call: PluginCall) {
        appOpenAdPlugin.loadAppOpen(context, activity, call, appOpenNotifier)
    }

    @PluginMethod
    public fun showAppOpen(call: PluginCall) {
        appOpenAdPlugin.showAppOpen(activity, call, appOpenNotifier)
    }

    @PluginMethod
    public fun isAppOpenLoaded(call: PluginCall) {
        appOpenAdPlugin.isAppOpenLoaded(activity, call)
    }

    // ---------------------------------------------------------
    // MAIN METHODS
    // ---------------------------------------------------------

    @PluginMethod
    public fun initialize(call: PluginCall) {
        setRequestConfiguration(call)

        // Same as banner/interstitial: bridge thread is not the UI thread — MobileAds + view setup must run on main.
        activity.runOnUiThread {
            try {
                MobileAds.initialize(context) {}
                // Resolve only once the banner parent actually exists, so a resolved
                // initialize() means what callers already read it as. See #451.
                bannerExecutor.awaitViewGroup { found ->
                    if (found) {
                        call.resolve()
                    } else {
                        call.reject("AdMob initialized, but the banner parent view never appeared")
                    }
                }
            } catch (ex: Exception) {
                call.reject(ex.localizedMessage, ex = ex)
            }
        }
    }

    @PluginMethod
    public fun requestTrackingAuthorization(call: PluginCall) {
        call.resolve()
    }

    @PluginMethod
    public fun trackingAuthorizationStatus(call: PluginCall) {
        val response = JSObject()
        response.put("status", AuthorizationStatusEnum.AUTHORIZED.status)
        call.resolve(response)
    }

    // ---------------------------------------------------------
    // USER CONSENT
    // ---------------------------------------------------------

    @PluginMethod
    public fun requestConsentInfo(call: PluginCall) {
        adConsentExecutor.requestConsentInfo(call, notifyListenersFunction)
    }

    @PluginMethod
    public fun showPrivacyOptionsForm(call: PluginCall) {
        adConsentExecutor.showPrivacyOptionsForm(call, notifyListenersFunction)
    }

    @PluginMethod
    public fun showConsentForm(call: PluginCall) {
        adConsentExecutor.showConsentForm(call, notifyListenersFunction)
    }

    @PluginMethod
    public fun resetConsentInfo(call: PluginCall) {
        adConsentExecutor.resetConsentInfo(call, notifyListenersFunction)
    }

    // ---------------------------------------------------------
    // APP SETTINGS
    // ---------------------------------------------------------

    @PluginMethod
    public fun setApplicationMuted(call: PluginCall) {
        val muted = call.getBoolean("muted")
        if (muted == null) {
            call.reject("muted property cannot be null")
            return
        }
        MobileAds.setAppMuted(muted)
        call.resolve()
    }

    @PluginMethod
    public fun setApplicationVolume(call: PluginCall) {
        val volume = call.getFloat("volume")
        if (volume == null) {
            call.reject("volume property cannot be null")
            return
        }
        MobileAds.setAppVolume(volume)
        call.resolve()
    }

    // ---------------------------------------------------------
    // BANNER ADS
    // ---------------------------------------------------------

    @PluginMethod
    public fun showBanner(call: PluginCall) {
        bannerExecutor.showBanner(call)
    }

    @PluginMethod
    public fun hideBanner(call: PluginCall) {
        bannerExecutor.hideBanner(call)
    }

    @PluginMethod
    public fun resumeBanner(call: PluginCall) {
        bannerExecutor.resumeBanner(call)
    }

    @PluginMethod
    public fun removeBanner(call: PluginCall) {
        bannerExecutor.removeBanner(call)
    }

    // ---------------------------------------------------------
    // INTERSTITIAL ADS
    // ---------------------------------------------------------

    @PluginMethod
    public fun prepareInterstitial(call: PluginCall) {
        adInterstitialExecutor.prepareInterstitial(call, notifyListenersFunction)
    }

    @PluginMethod
    public fun showInterstitial(call: PluginCall) {
        adInterstitialExecutor.showInterstitial(call, notifyListenersFunction)
    }

    // ---------------------------------------------------------
    // REWARDED ADS
    // ---------------------------------------------------------

    @PluginMethod
    public fun prepareRewardVideoAd(call: PluginCall) {
        adRewardExecutor.prepareRewardVideoAd(call, notifyListenersFunction)
    }

    @PluginMethod
    public fun showRewardVideoAd(call: PluginCall) {
        adRewardExecutor.showRewardVideoAd(call, notifyListenersFunction)
    }

    @PluginMethod
    public fun prepareRewardInterstitialAd(call: PluginCall) {
        adRewardInterstitialExecutor.prepareRewardInterstitialAd(call, notifyListenersFunction)
    }

    @PluginMethod
    public fun showRewardInterstitialAd(call: PluginCall) {
        adRewardInterstitialExecutor.showRewardInterstitialAd(call, notifyListenersFunction)
    }

    // ---------------------------------------------------------
    // REQUEST CONFIGURATION
    // ---------------------------------------------------------

    private fun setRequestConfiguration(call: PluginCall) {
        // Testing Devices
        val initializeForTesting = call.getBoolean("initializeForTesting", false) ?: false
        val testingDevices = if (initializeForTesting) {
            call.getArray("testingDevices", EMPTY_TESTING_DEVICES) ?: EMPTY_TESTING_DEVICES
        } else {
            EMPTY_TESTING_DEVICES
        }

        val tagForChildDirectedTreatment = when (call.getBoolean("tagForChildDirectedTreatment")) {
            null -> RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED
            true -> RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE
            false -> RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE
        }

        val tagForUnderAgeOfConsent = when (call.getBoolean("tagForUnderAgeOfConsent")) {
            null -> RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED
            true -> RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE
            false -> RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_FALSE
        }

        val maxAdContentRating = when (call.getString("maxAdContentRating")) {
            "General" -> RequestConfiguration.MAX_AD_CONTENT_RATING_G
            "ParentalGuidance" -> RequestConfiguration.MAX_AD_CONTENT_RATING_PG
            "Teen" -> RequestConfiguration.MAX_AD_CONTENT_RATING_T
            "MatureAudience" -> RequestConfiguration.MAX_AD_CONTENT_RATING_MA
            else -> RequestConfiguration.MAX_AD_CONTENT_RATING_UNSPECIFIED
        }

        try {
            val requestConfiguration = RequestConfiguration.Builder()
                .setTestDeviceIds(testingDevices.toList<String>())
                .setTagForChildDirectedTreatment(tagForChildDirectedTreatment)
                .setTagForUnderAgeOfConsent(tagForUnderAgeOfConsent)
                .setMaxAdContentRating(maxAdContentRating)
                .build()
            MobileAds.setRequestConfiguration(requestConfiguration)
        } catch (error: JSONException) {
            call.reject(error.toString())
        }
    }

    public companion object {
        @JvmField
        public val EMPTY_TESTING_DEVICES: JSArray = JSArray()
    }
}
