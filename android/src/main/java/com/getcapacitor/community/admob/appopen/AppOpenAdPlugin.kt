package com.getcapacitor.community.admob.appopen

import android.app.Activity
import android.content.Context
import com.getcapacitor.JSObject
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginException
import com.getcapacitor.community.admob.models.AdMobPluginError
import com.getcapacitor.community.admob.models.AdMobRevenueData

/**
 * Loads and shows App Open ads for AdMob, whose methods run on the main thread. What these functions throw rejects
 * the call they were given.
 */
public class AppOpenAdPlugin {

    public fun interface EventNotifier {
        public fun notify(eventName: String, data: JSObject)
    }

    private val preparedManagers = LinkedHashMap<String, AppOpenAdManager>()
    private var lastPreparedAdId: String? = null

    public fun loadAppOpen(context: Context?, call: PluginCall, notifier: EventNotifier) {
        if (context == null) {
            throw PluginException("Context is not available")
        }

        val adUnitId = call.getString("adId") ?: throw PluginException("adId is required")

        val manager = preparedManagers.getOrPut(adUnitId) { AppOpenAdManager(adUnitId) }

        manager.loadAd(
            context.applicationContext,
            onLoaded = {
                lastPreparedAdId = adUnitId
                val adInfo = JSObject().apply {
                    put("adUnitId", adUnitId)
                }
                notifier.notify(AppOpenAdPluginEvents.Loaded, adInfo)
                call.resolve(adInfo)
            },
            onFailed = { loadAdError ->
                val errorMessage = loadAdError?.message ?: "Failed to load App Open Ad"
                val errorCode = loadAdError?.code ?: -1
                notifier.notify(AppOpenAdPluginEvents.FailedToLoad, AdMobPluginError(errorCode, errorMessage))
                call.reject(errorMessage)
            },
            onPaidEvent = { adValue, networkName, impressionId ->
                val revenueData = AdMobRevenueData(adValue, adUnitId, networkName, impressionId)
                notifier.notify(AppOpenAdPluginEvents.AdImpression, revenueData)
            }
        )
    }

    public fun showAppOpen(activity: Activity?, call: PluginCall, notifier: EventNotifier) {
        if (activity == null) {
            throw PluginException("Activity is not available")
        }

        val adId = call.getString("adId") ?: lastPreparedAdId
        val manager = if (adId != null) preparedManagers[adId] else null

        if (manager == null || !manager.isAdLoaded) {
            throw PluginException("App Open Ad is not loaded")
        }

        manager.showAdIfAvailable(
            activity,
            onOpened = {
                notifier.notify(AppOpenAdPluginEvents.Opened, JSObject())
            },
            onClosed = {
                preparedManagers.remove(adId)
                if (lastPreparedAdId == adId) {
                    lastPreparedAdId = preparedManagers.entries.lastOrNull { it.value.isAdLoaded }?.key
                }
                notifier.notify(AppOpenAdPluginEvents.Closed, JSObject())
                call.resolve()
            },
            onFailedToShow = { adError ->
                preparedManagers.remove(adId)
                if (lastPreparedAdId == adId) {
                    lastPreparedAdId = preparedManagers.entries.lastOrNull { it.value.isAdLoaded }?.key
                }
                val errorMessage = adError?.message ?: "Failed to show App Open Ad"
                val errorCode = adError?.code ?: -1
                notifier.notify(AppOpenAdPluginEvents.FailedToShow, AdMobPluginError(errorCode, errorMessage))
                call.reject(errorMessage)
            }
        )
    }

    public fun isAppOpenLoaded(call: PluginCall) {
        val adId = call.getString("adId") ?: lastPreparedAdId
        val loaded = adId?.let { preparedManagers[it]?.isAdLoaded } ?: false
        val result = JSObject().apply {
            put("value", loaded)
        }
        call.resolve(result)
    }
}
