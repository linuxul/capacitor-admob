package com.getcapacitor.community.admob.interstitial

import android.app.Activity
import android.content.Context
import androidx.core.util.Supplier
import com.getcapacitor.JSObject
import com.getcapacitor.PluginCall
import com.getcapacitor.community.admob.helpers.AdViewIdHelper
import com.getcapacitor.community.admob.helpers.FullscreenPluginCallback
import com.getcapacitor.community.admob.helpers.RequestHelper
import com.getcapacitor.community.admob.models.AdMobPluginError
import com.getcapacitor.community.admob.models.AdOptions
import com.getcapacitor.community.admob.models.Executor
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.common.util.BiConsumer

public class AdInterstitialExecutor(
    contextSupplier: Supplier<Context>,
    activitySupplier: Supplier<Activity?>,
    notifyListenersFunction: BiConsumer<String, JSObject>,
    pluginLogTag: String,
    private val adCallbackAndListeners: InterstitialAdCallbackAndListeners
) : Executor(contextSupplier, activitySupplier, notifyListenersFunction, pluginLogTag, "AdRewardExecutor") {
    public fun prepareInterstitial(call: PluginCall, notifyListenersFunction: BiConsumer<String, JSObject>) {
        val adOptions = AdOptions.AdOptionsFactory.createInterstitialOptions(call)

        try {
            // A missing activity was a NullPointerException here, which the catch below turns into a rejection.
            activitySupplier.get()!!.runOnUiThread {
                val adRequest = RequestHelper.createRequest(adOptions)
                val id = AdViewIdHelper.getFinalAdId(adOptions, adRequest, logTag, contextSupplier.get())
                InterstitialAd.load(
                    // The SDK throws a NullPointerException for a missing context as well.
                    activitySupplier.get()!!,
                    id,
                    adRequest,
                    adCallbackAndListeners.getInterstitialAdLoadCallback(call, notifyListenersFunction)
                )
            }
        } catch (ex: Exception) {
            call.reject(ex.localizedMessage, ex = ex)
        }
    }

    public fun showInterstitial(call: PluginCall, notifyListenersFunction: BiConsumer<String, JSObject>) {
        val adId = call.getString("adId") ?: lastPreparedAdId
        val ad = adId?.let { preparedAds[it] }

        if (ad == null) {
            val errorMessage = "No Interstitial can be shown. It was not prepared or maybe it failed to be prepared."
            call.reject(errorMessage)
            val errorObject = AdMobPluginError(-1, errorMessage)
            notifyListenersFunction.accept(InterstitialAdPluginPluginEvent.FailedToLoad, errorObject)
            return
        }

        // A missing activity was an uncaught NullPointerException here.
        activitySupplier.get()!!.runOnUiThread {
            try {
                ad.fullScreenContentCallback = FullscreenPluginCallback(InterstitialAdPluginPluginEvent, notifyListenersFunction) {
                    preparedAds.remove(adId)
                    if (adId == lastPreparedAdId) {
                        lastPreparedAdId = preparedAds.keys.lastOrNull()
                    }
                }
                // Caught below, like the NullPointerException the SDK throws for a missing activity.
                ad.show(activitySupplier.get()!!)
                call.resolve()
            } catch (ex: Exception) {
                call.reject(ex.localizedMessage, ex = ex)
            }
        }
    }

    public companion object {
        public val preparedAds: MutableMap<String, InterstitialAd> = LinkedHashMap()
        public var lastPreparedAdId: String? = null
    }
}
