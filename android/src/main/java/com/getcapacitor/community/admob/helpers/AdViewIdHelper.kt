package com.getcapacitor.community.admob.helpers

import android.content.Context
import android.util.Log
import com.getcapacitor.community.admob.models.AdOptions
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView

// The functions are @JvmStatic so that the unit tests can replace them with Mockito.mockStatic.
public object AdViewIdHelper {
    @JvmStatic
    public fun getFinalAdId(adOptions: AdOptions, adRequest: AdRequest, logTag: String, context: Context): String {
        if (!adOptions.isTesting) {
            return adOptions.adId
        }

        if (adRequest.isTestDevice(context)) {
            Log.w(logTag, "This device is registered as Testing Device. The real Ad Id will be used")
            return adOptions.adId
        }

        return adOptions.testingId
    }

    @JvmStatic
    public fun assignIdToAdView(adView: AdView, adOptions: AdOptions, adRequest: AdRequest, logTag: String, context: Context) {
        val finalId = getFinalAdId(adOptions, adRequest, logTag, context)
        adView.adUnitId = finalId
        Log.d(logTag, "Ad ID: $finalId")
    }
}
