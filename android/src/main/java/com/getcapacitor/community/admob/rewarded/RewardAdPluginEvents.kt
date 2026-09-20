// The names mirror the event enums of the TypeScript API.
@file:Suppress("ktlint:standard:property-naming")

package com.getcapacitor.community.admob.rewarded

import com.getcapacitor.community.admob.models.LoadPluginEventNames

public object RewardAdPluginEvents : LoadPluginEventNames {
    public const val Loaded: String = "onRewardedVideoAdLoaded"
    public const val FailedToLoad: String = "onRewardedVideoAdFailedToLoad"
    public const val Rewarded: String = "onRewardedVideoAdReward"
    override val Showed: String = "onRewardedVideoAdShowed"
    override val FailedToShow: String = "onRewardedVideoAdFailedToShow"
    override val Dismissed: String = "onRewardedVideoAdDismissed"
    public const val AdImpression: String = "onRewardedVideoAdImpression"
}
