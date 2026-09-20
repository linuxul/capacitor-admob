package com.getcapacitor.community.admob.banner

public enum class BannerAdPluginEvents(public val webEventName: String) {
    SizeChanged("bannerAdSizeChanged"),
    Closed("bannerAdClosed"),
    FailedToLoad("bannerAdFailedToLoad"),
    Opened("bannerAdOpened"),
    Loaded("bannerAdLoaded"),
    Clicked("bannerAdClicked"),
    AdImpression("bannerAdImpression"),
    AdPaid("bannerAdPaid")
}
