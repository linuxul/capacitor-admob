package com.getcapacitor.community.admob.models

import com.getcapacitor.JSObject
import com.google.android.gms.ads.AdError

public data class AdMobPluginError(public val code: Int, public val message: String) : JSObject() {
    override fun put(key: String, value: Int): JSObject = throw Exception("Do not put elements directly here use the constructor")
    init {
        super.put("code", this.code)
        super.put("message", this.message)
    }
    public constructor(adError: AdError) : this(adError.code, adError.message)
}
