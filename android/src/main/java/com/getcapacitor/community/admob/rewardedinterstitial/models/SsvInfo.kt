package com.getcapacitor.community.admob.rewardedinterstitial.models

import com.getcapacitor.PluginCall

public class SsvInfo(public val customData: String? = null, public val userId: String? = null) {

    public constructor(pluginCall: PluginCall?) : this(
        pluginCall?.getObject("ssv")?.getString("customData"),
        pluginCall?.getObject("ssv")?.getString("userId")
    )

    public constructor() : this(null, null)

    public val hasInfo: Boolean
        get() = customData != null || userId != null
}
