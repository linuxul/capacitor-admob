package com.getcapacitor.community.admob.banner

import com.getcapacitor.JSObject
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdView
import java.util.*

public data class BannerAdSizeInfo(public val width: Int, public val height: Int) : JSObject() {
    override fun put(key: String, value: Int): JSObject = throw Exception("Do not put elements directly here use the constructor")
    init {
        super.put("width", width)
        super.put("height", height)
    }
    public constructor(
        mAdView: AdView
    ) : this(Objects.requireNonNull(mAdView.adSize)!!.width, Objects.requireNonNull(mAdView.adSize)!!.height)
}
