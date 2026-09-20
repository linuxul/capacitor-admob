package com.getcapacitor.community.admob.models

import androidx.annotation.VisibleForTesting
import com.getcapacitor.PluginCall
import com.getcapacitor.community.admob.banner.BannerAdSizeEnum
import com.getcapacitor.community.admob.rewarded.models.SsvInfo

/**
 * Holds the options for an Ad Request
 * TODO: automatically create type definitions https://github.com/vojtechhabarta/typescript-generator ?
 */
public class AdOptions private constructor(
    /**
     * The ad unit ID that you want to request
     *
     * @see <a href="https://support.google.com/admob/answer/7356431?hl=en">Find ad Unit ID of an app</a>
     */
    public val adId: String,
    /**
     * If set to true, an test app will be requested using the official sample ads unit ids
     *
     * @see <a href="https://developers.google.com/admob/android/test-ads#sample_ad_units">Sample ad units</a>
     */
    public val isTesting: Boolean,
    /**
     * The position of the ad, it can be TOP_CENTER,
     * CENTER or BOTTOM
     *
     * TODO: Make an enum
     */
    public val position: String,
    /**
     * Margin Banner. Default is 0 px;
     * If position is BOTTOM_CENTER, margin is be margin-bottom.
     * If position is TOP_CENTER, margin is be margin-top.
     */
    public val margin: Int,
    /**
     * The default behavior of the Google Mobile Ads SDK is to serve personalized ads.
     * Set this to true to request Non-Personalized Ads
     *
     * Default is false
     *
     * @see <a href="https://developers.google.com/admob/android/eu-consent">EU-Consent</a>
     */
    public val npa: Boolean,
    /**
     * Banner Ad Size, defaults to ADAPTIVE_BANNER.
     * IT can be: ADAPTIVE_BANNER, BANNER, MEDIUM_RECTANGLE,
     * FULL_BANNER, LEADERBOARD, SKYSCRAPER, or CUSTOM
     */
    public val adSize: BannerAdSizeEnum,
    /**
     * Used for Server side verification of Reward Ads
     */
    public val ssvInfo: SsvInfo,
    /**
     * The id used for this type of test ads.
     */
    public val testingId: String
) {
    /*
     * TODO: Since the Id in the Typescript AdOptions interface is not optional
     *  the default value here should never be used. In case it is used it means this is an error.
     *  Would not be better to print an error (call.reject()) and do not create any Ad?
     *  Why? Because an distracted dev could think that everything is working when it is not.
     */
    private constructor(call: PluginCall, testingId: String) : this(
        adId = call.getString("adId", testingId) ?: testingId,
        isTesting = call.getBoolean("isTesting", false) ?: false,
        position = call.getString("position", "BOTTOM_CENTER") ?: "BOTTOM_CENTER",
        margin = call.getInt("margin", 0) ?: 0,
        npa = call.getBoolean("npa", false) ?: false,
        adSize = adSizeStringToAdSizeEnum(call.getString("adSize", BannerAdSizeEnum.ADAPTIVE_BANNER.name)),
        ssvInfo = SsvInfo(call),
        testingId = testingId
    )

    // The functions are @JvmStatic so that the unit tests can replace them with Mockito.mockStatic.
    public object AdOptionsFactory {
        @JvmStatic
        public fun createBannerOptions(call: PluginCall): AdOptions = AdOptions(call, BANNER_TESTER_ID)

        @JvmStatic
        public fun createInterstitialOptions(call: PluginCall): AdOptions = AdOptions(call, INTERSTITIAL_TESTER_ID)

        @JvmStatic
        public fun createRewardVideoOptions(call: PluginCall): AdOptions = AdOptions(call, REWARD_VIDEO_TESTER_ID)

        @JvmStatic
        public fun createRewardInterstitialOptions(call: PluginCall): AdOptions = AdOptions(call, REWARD_INTERSTITIAL_TESTER_ID)

        @JvmStatic
        public fun createAppOpenOptions(call: PluginCall): AdOptions = AdOptions(call, APP_OPEN_TESTER_ID)

        @JvmStatic
        public fun createGenericOptions(call: PluginCall, testingID: String): AdOptions = AdOptions(call, testingID)
    }

    /**
     * Just for testing
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    public class TesterAdOptionsBuilder {
        private var id = "TesterAdOptionsBuilder__defaultID"
        private var testingID = "TesterAdOptionsBuilder__testingID"
        private var isTesting = true
        private var position = "TesterAdOptionsBuilder__position"
        private var margin = 1
        private var npa = false
        private var adSize = BannerAdSizeEnum.ADAPTIVE_BANNER
        private var ssvInfo = SsvInfo()

        public fun setSsvInfo(info: SsvInfo): TesterAdOptionsBuilder = apply { ssvInfo = info }

        public fun setIsTesting(value: Boolean): TesterAdOptionsBuilder = apply { isTesting = value }

        public fun setNpa(value: Boolean): TesterAdOptionsBuilder = apply { npa = value }

        public fun setPosition(value: String): TesterAdOptionsBuilder = apply { position = value }

        public fun setTestingID(value: String): TesterAdOptionsBuilder = apply { testingID = value }

        public fun setID(value: String): TesterAdOptionsBuilder = apply { id = value }

        public fun setMargin(value: Int): TesterAdOptionsBuilder = apply { margin = value }

        public fun setAdSize(value: BannerAdSizeEnum): TesterAdOptionsBuilder = apply { adSize = value }

        public fun build(): AdOptions = AdOptions(id, isTesting, position, margin, npa, adSize, ssvInfo, testingID)
    }

    public companion object {
        public const val BANNER_TESTER_ID: String = "ca-app-pub-3940256099942544/6300978111"
        public const val INTERSTITIAL_TESTER_ID: String = "ca-app-pub-3940256099942544/1033173712"
        public const val REWARD_VIDEO_TESTER_ID: String = "ca-app-pub-3940256099942544/5224354917"
        public const val REWARD_INTERSTITIAL_TESTER_ID: String = "ca-app-pub-3940256099942544/5354046379"
        public const val APP_OPEN_TESTER_ID: String = "ca-app-pub-3940256099942544/9257395921"

        private fun adSizeStringToAdSizeEnum(sizeString: String?): BannerAdSizeEnum = try {
            BannerAdSizeEnum.valueOf(sizeString ?: BannerAdSizeEnum.ADAPTIVE_BANNER.name)
        } catch (_: IllegalArgumentException) {
            BannerAdSizeEnum.ADAPTIVE_BANNER
        }
    }
}
