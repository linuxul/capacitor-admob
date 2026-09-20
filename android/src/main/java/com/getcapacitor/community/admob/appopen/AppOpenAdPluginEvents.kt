// The names mirror the event enums of the TypeScript API.
@file:Suppress("ktlint:standard:property-naming")

package com.getcapacitor.community.admob.appopen

public object AppOpenAdPluginEvents {
    public const val Loaded: String = "appOpenAdLoaded"
    public const val FailedToLoad: String = "appOpenAdFailedToLoad"
    public const val Opened: String = "appOpenAdOpened"
    public const val Closed: String = "appOpenAdClosed"
    public const val FailedToShow: String = "appOpenAdFailedToShow"
    public const val AdImpression: String = "appOpenAdImpression"
}
