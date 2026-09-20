// The names mirror the event enums of the TypeScript API.
@file:Suppress("ktlint:standard:property-naming")

package com.getcapacitor.community.admob.interstitial

import com.getcapacitor.community.admob.models.LoadPluginEventNames

public object InterstitialAdPluginPluginEvent : LoadPluginEventNames {
    public const val Loaded: String = "interstitialAdLoaded"
    public const val FailedToLoad: String = "interstitialAdFailedToLoad"
    override val Showed: String = "interstitialAdShowed"
    override val FailedToShow: String = "interstitialAdFailedToShow"
    override val Dismissed: String = "interstitialAdDismissed"
    public const val AdImpression: String = "interstitialAdImpression"
}
