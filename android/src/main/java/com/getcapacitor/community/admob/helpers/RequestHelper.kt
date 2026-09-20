package com.getcapacitor.community.admob.helpers

import android.os.Bundle
import com.getcapacitor.community.admob.models.AdOptions
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdRequest

// The functions are @JvmStatic so that the unit tests can replace them with Mockito.mockStatic.
public object RequestHelper {
    /**
     * Use this function to create all requests, here we can centralize request extras
     */
    @JvmStatic
    public fun createRequest(adOptions: AdOptions): AdRequest {
        val adRequestBuilder = AdRequest.Builder()

        // TODO: Allow more key/value extras?
        if (adOptions.npa) {
            val extras = Bundle()
            extras.putString("npa", "1")
            adRequestBuilder.addNetworkExtrasBundle(AdMobAdapter::class.java, extras)
        }

        return adRequestBuilder.build()
    }

    /**
     * Gets a string error reason from an error code.
     */
    @JvmStatic
    public fun getRequestErrorReason(errorCode: Int): String = when (errorCode) {
        AdRequest.ERROR_CODE_INTERNAL_ERROR -> "Internal error"
        AdRequest.ERROR_CODE_INVALID_REQUEST -> "Invalid request"
        AdRequest.ERROR_CODE_NETWORK_ERROR -> "Network Error"
        AdRequest.ERROR_CODE_NO_FILL -> "No fill"
        AdRequest.ERROR_CODE_APP_ID_MISSING -> "App Id Missing"
        else -> "Unknown error"
    }
}
