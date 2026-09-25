package com.getcapacitor.community.admob.rewardedinterstitial

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
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.common.util.BiConsumer

/** Loads and shows rewarded interstitial ads. Its functions are called on the main thread, where AdMob's ad methods run. */
public class AdRewardInterstitialExecutor(
    contextSupplier: Supplier<Context>,
    activitySupplier: Supplier<Activity?>,
    notifyListenersFunction: BiConsumer<String, JSObject>,
    pluginLogTag: String
) : Executor(contextSupplier, activitySupplier, notifyListenersFunction, pluginLogTag, "AdRewardExecutor") {
    public fun prepareRewardInterstitialAd(call: PluginCall, notifyListenersFunction: BiConsumer<String, JSObject>) {
        val adOptions = AdOptions.AdOptionsFactory.createRewardInterstitialOptions(call)

        try {
            val adRequest = RequestHelper.createRequest(adOptions)
            val id = AdViewIdHelper.getFinalAdId(adOptions, adRequest, logTag, contextSupplier.get())
            RewardedInterstitialAd.load(
                contextSupplier.get(),
                id,
                adRequest,
                RewardedInterstitialAdCallbackAndListeners.getRewardedAdLoadCallback(call, notifyListenersFunction, adOptions)
            )
        } catch (ex: Exception) {
            call.reject(ex.localizedMessage, ex = ex)
        }
    }

    public fun showRewardInterstitialAd(call: PluginCall, notifyListenersFunction: BiConsumer<String, JSObject>) {
        val adId = call.getString("adId") ?: lastPreparedAdId
        val ad = adId?.let { preparedAds[it] }

        if (ad == null) {
            val errorMessage = "No Reward Interstitial Video Ad can be shown. It was not prepared or maybe it failed to be prepared."
            call.reject(errorMessage)
            val errorObject = AdMobPluginError(-1, errorMessage)
            notifyListenersFunction.accept(RewardInterstitialAdPluginEvents.FailedToLoad, errorObject)
            return
        }

        try {
            ad.fullScreenContentCallback = FullscreenPluginCallback(RewardInterstitialAdPluginEvents, notifyListenersFunction) {
                preparedAds.remove(adId)
                if (adId == lastPreparedAdId) {
                    lastPreparedAdId = preparedAds.keys.lastOrNull()
                }
            }
            ad.show(
                // A missing activity is a NullPointerException, which the catch below turns into a rejection. The
                // SDK throws one for a missing activity as well.
                activitySupplier.get()!!,
                RewardedInterstitialAdCallbackAndListeners.getOnUserEarnedRewardListener(call, notifyListenersFunction)
            )
        } catch (ex: Exception) {
            call.reject(ex.localizedMessage, ex = ex)
        }
    }

    public companion object {
        public val preparedAds: MutableMap<String, RewardedInterstitialAd> = LinkedHashMap()
        public var lastPreparedAdId: String? = null
    }
}
