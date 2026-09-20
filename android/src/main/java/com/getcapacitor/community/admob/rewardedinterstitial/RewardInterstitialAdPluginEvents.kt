// The names mirror the event enums of the TypeScript API.
@file:Suppress("ktlint:standard:property-naming")

package com.getcapacitor.community.admob.rewardedinterstitial

import com.getcapacitor.community.admob.models.LoadPluginEventNames

public object RewardInterstitialAdPluginEvents : LoadPluginEventNames {
    public const val Loaded: String = "onRewardedInterstitialAdLoaded"
    public const val FailedToLoad: String = "onRewardedInterstitialAdFailedToLoad"
    public const val Rewarded: String = "onRewardedInterstitialAdReward"
    override val Showed: String = "onRewardedInterstitialAdShowed"
    override val FailedToShow: String = "onRewardedInterstitialAdFailedToShow"
    override val Dismissed: String = "onRewardedInterstitialAdDismissed"
    public const val AdImpression: String = "onRewardedInterstitialAdImpression"
}
