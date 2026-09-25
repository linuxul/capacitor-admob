package com.getcapacitor.community.admob.rewarded

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
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.common.util.BiConsumer

/** Loads and shows rewarded ads. Its functions are called on the main thread, where AdMob's ad methods run. */
public class AdRewardExecutor(
    contextSupplier: Supplier<Context>,
    activitySupplier: Supplier<Activity?>,
    notifyListenersFunction: BiConsumer<String, JSObject>,
    pluginLogTag: String
) : Executor(contextSupplier, activitySupplier, notifyListenersFunction, pluginLogTag, "AdRewardExecutor") {
    public fun prepareRewardVideoAd(call: PluginCall, notifyListenersFunction: BiConsumer<String, JSObject>) {
        val adOptions = AdOptions.AdOptionsFactory.createRewardVideoOptions(call)

        try {
            val adRequest = RequestHelper.createRequest(adOptions)
            val id = AdViewIdHelper.getFinalAdId(adOptions, adRequest, logTag, contextSupplier.get())
            RewardedAd.load(
                contextSupplier.get(),
                id,
                adRequest,
                RewardedAdCallbackAndListeners.getRewardedAdLoadCallback(call, notifyListenersFunction, adOptions)
            )
        } catch (ex: Exception) {
            call.reject(ex.localizedMessage, ex = ex)
        }
    }

    public fun showRewardVideoAd(call: PluginCall, notifyListenersFunction: BiConsumer<String, JSObject>) {
        val adId = call.getString("adId") ?: lastPreparedAdId
        val ad = adId?.let { preparedAds[it] }

        if (ad == null) {
            val errorMessage = "No Reward Video Ad can be shown. It was not prepared or maybe it failed to be prepared."
            call.reject(errorMessage)
            val errorObject = AdMobPluginError(-1, errorMessage)
            notifyListenersFunction.accept(RewardAdPluginEvents.FailedToLoad, errorObject)
            return
        }

        try {
            ad.fullScreenContentCallback = FullscreenPluginCallback(RewardAdPluginEvents, notifyListenersFunction) {
                preparedAds.remove(adId)
                if (adId == lastPreparedAdId) {
                    lastPreparedAdId = preparedAds.keys.lastOrNull()
                }
            }
            ad.show(
                // A missing activity is a NullPointerException, which the catch below turns into a rejection. The
                // SDK throws one for a missing activity as well.
                activitySupplier.get()!!,
                RewardedAdCallbackAndListeners.getOnUserEarnedRewardListener(call, notifyListenersFunction)
            )
        } catch (ex: Exception) {
            call.reject(ex.localizedMessage, ex = ex)
        }
    }

    public companion object {
        public val preparedAds: MutableMap<String, RewardedAd> = LinkedHashMap()
        public var lastPreparedAdId: String? = null
    }
}
