import Foundation
import Capacitor
import GoogleMobileAds
#if canImport(AppTrackingTransparency)
import AppTrackingTransparency
#endif

@objc(AdMobPlugin)
public class AdMobPlugin: CAPPlugin, CAPBridgedPlugin {
    public let identifier = "AdMob"
    public let jsName = "AdMob"
    public let pluginMethods: [CAPPluginMethod] = [
        .promise("initialize", AdMobPlugin.initialize),
        .async("trackingAuthorizationStatus", AdMobPlugin.trackingAuthorizationStatus),
        .async("requestConsentInfo", AdMobPlugin.requestConsentInfo),
        .async("showPrivacyOptionsForm", AdMobPlugin.showPrivacyOptionsForm),
        .async("requestTrackingAuthorization", AdMobPlugin.requestTrackingAuthorization),
        .async("showConsentForm", AdMobPlugin.showConsentForm),
        .promise("resetConsentInfo", AdMobPlugin.resetConsentInfo),
        .promise("setApplicationMuted", AdMobPlugin.setApplicationMuted),
        .promise("setApplicationVolume", AdMobPlugin.setApplicationVolume),
        .promise("showBanner", AdMobPlugin.showBanner),
        .promise("resumeBanner", AdMobPlugin.resumeBanner),
        .promise("hideBanner", AdMobPlugin.hideBanner),
        .promise("removeBanner", AdMobPlugin.removeBanner),
        .promise("prepareInterstitial", AdMobPlugin.prepareInterstitial),
        .promise("showInterstitial", AdMobPlugin.showInterstitial),
        .promise("prepareRewardVideoAd", AdMobPlugin.prepareRewardVideoAd),
        .promise("showRewardVideoAd", AdMobPlugin.showRewardVideoAd),
        .promise("prepareRewardInterstitialAd", AdMobPlugin.prepareRewardInterstitialAd),
        .promise("showRewardInterstitialAd", AdMobPlugin.showRewardInterstitialAd),
        .promise("loadAppOpen", AdMobPlugin.loadAppOpen),
        .promise("showAppOpen", AdMobPlugin.showAppOpen),
        .async("isAppOpenLoaded", AdMobPlugin.isAppOpenLoaded)
    ]

    // Initialization, the settings, and loading, showing, hiding and removing ads stay synchronous: the bridge queue
    // runs them in the order of the calls and they hand their UIKit and SDK work to the main queue in that order,
    // which async methods would not keep. Their results come from SDK callbacks and delegates (a rewarded ad resolves
    // only when the reward is earned), which they keep. The methods that only read a status, ask for tracking
    // authorization, or update and present the consent forms are async methods on the main actor.

    private let appOpenAdPlugin = AppOpenAdPlugin()
    func loadAppOpen(_ call: CAPPluginCall) {
        appOpenAdPlugin.loadAppOpen(
            call,
            notify: { [weak self] eventName, data in
                self?.notifyListeners(eventName, data: data)
            }
        )
    }

    func showAppOpen(_ call: CAPPluginCall) {
        appOpenAdPlugin.showAppOpen(
            call,
            getRootViewController: self.getRootVC,
            notify: { [weak self] eventName, data in
                self?.notifyListeners(eventName, data: data)
            }
        )
    }

    /// Runs on the main actor, where loadAppOpen and showAppOpen change the prepared ads.
    @MainActor
    func isAppOpenLoaded(_ call: CAPPluginCall) async -> JSObject {
        ["value": appOpenAdPlugin.isAppOpenLoaded(adId: call.getString("adId"))]
    }

    var testingDevices: [String] = []

    private let bannerExecutor = BannerExecutor()
    private let adInterstitialExecutor = AdInterstitialExecutor()
    private let adRewardExecutor = AdRewardExecutor()
    private let adRewardInterstitialExecutor = AdRewardInterstitialExecutor()
    private let consentExecutor = ConsentExecutor()

    /**
     * Enable SKAdNetwork to track conversions
     * https://developers.google.com/admob/ios/ios14
     */
    func initialize(_ call: CAPPluginCall) {
        self.bannerExecutor.plugin = self
        self.adInterstitialExecutor.plugin = self
        self.adRewardExecutor.plugin = self
        self.adRewardInterstitialExecutor.plugin = self
        self.adInterstitialExecutor.plugin = self
        self.consentExecutor.plugin = self
        self.setRequestConfiguration(call)

        MobileAds.shared.start(completionHandler: nil)
        call.resolve([:])
    }

    /**
     * DEPRECATED: It's now ship with Admob UMP Consent
     */
    @MainActor
    func requestTrackingAuthorization(_ call: CAPPluginCall) async -> JSObject {
        #if canImport(AppTrackingTransparency)
        _ = await ATTrackingManager.requestTrackingAuthorization()
        #endif
        return [:]
    }

    func setApplicationMuted(_ call: CAPPluginCall) throws {
        guard let shouldMute = call.getBool("muted") else {
            throw CAPPluginError("muted property cannot be null")
        }
        MobileAds.shared.isApplicationMuted = shouldMute
        call.resolve([:])
    }

    func setApplicationVolume(_ call: CAPPluginCall) throws {
        guard var volume = call.getFloat("volume") else {
            throw CAPPluginError("volume property cannot be null")
        }
        // Clamp volumes.
        if volume < 0.0 {volume = 0.0} else if volume > 1.0 {volume = 1.0}

        MobileAds.shared.applicationVolume = volume

        call.resolve([:])
    }

    /**
     *  AdMob: Banner
     *  https://developers.google.com/ad-manager/mobile-ads-sdk/ios/banner?hl=ja
     */
    func showBanner(_ call: CAPPluginCall) {
        let adUnitID = getAdId(call, "ca-app-pub-3940256099942544/6300978111")
        let request = self.GADRequestWithOption(call.getBool("npa") ?? false)

        DispatchQueue.main.async {
            self.bannerExecutor.showBanner(call, request, adUnitID)
        }
    }

    func hideBanner(_ call: CAPPluginCall) {
        DispatchQueue.main.async {
            self.bannerExecutor.hideBanner(call)
        }
    }

    func resumeBanner(_ call: CAPPluginCall) {
        DispatchQueue.main.async {
            self.bannerExecutor.resumeBanner(call)
        }
    }

    func removeBanner(_ call: CAPPluginCall) {
        DispatchQueue.main.async {
            self.bannerExecutor.removeBanner(call)
        }
    }

    /**
     *  AdMob: Intertitial
     *  https://developers.google.com/admob/ios/interstitial?hl=ja
     */
    func prepareInterstitial(_ call: CAPPluginCall) {
        let adUnitID = getAdId(call, "ca-app-pub-3940256099942544/1033173712")
        let request = self.GADRequestWithOption(call.getBool("npa") ?? false)

        DispatchQueue.main.async {
            self.adInterstitialExecutor.prepareInterstitial(call, request, adUnitID)
        }
    }

    func showInterstitial(_ call: CAPPluginCall) {
        DispatchQueue.main.async {
            self.adInterstitialExecutor.showInterstitial(call)
        }
    }

    /**
     *  AdMob: Rewarded Ads
     *  https://developers.google.com/ad-manager/mobile-ads-sdk/ios/rewarded-ads
     */
    func prepareRewardVideoAd(_ call: CAPPluginCall) {
        let adUnitID = getAdId(call, "ca-app-pub-3940256099942544/1712485313")
        let request = self.GADRequestWithOption(call.getBool("npa") ?? false)

        DispatchQueue.main.async {
            self.adRewardExecutor.prepareRewardVideoAd(call, request, adUnitID)
        }
    }

    func showRewardVideoAd(_ call: CAPPluginCall) {
        DispatchQueue.main.async {
            self.adRewardExecutor.showRewardVideoAd(call)
        }
    }

    /**
     *  AdMob: Rewarded Interstitial Ads
     *  https://developers.google.com/ad-manager/mobile-ads-sdk/ios/rewarded-interstitial
     */
    func prepareRewardInterstitialAd(_ call: CAPPluginCall) {
        let adUnitID = getAdId(call, "ca-app-pub-3940256099942544/6978759866")
        let request = self.GADRequestWithOption(call.getBool("npa") ?? false)

        DispatchQueue.main.async {
            self.adRewardInterstitialExecutor.prepareRewardInterstitialAd(call, request, adUnitID)
        }
    }

    func showRewardInterstitialAd(_ call: CAPPluginCall) {
        DispatchQueue.main.async {
            self.adRewardInterstitialExecutor.showRewardInterstitialAd(call)
        }
    }

    @MainActor
    func trackingAuthorizationStatus(_ call: CAPPluginCall) async throws -> JSObject {
        guard let status = AdMobPlugin.trackingStatus(ATTrackingManager.trackingAuthorizationStatus) else {
            throw CAPPluginError("trackingAuthorizationStatus can't get status")
        }
        return ["status": status.rawValue]
    }

    /// The status JavaScript receives for an App Tracking Transparency status, or nil for a status this plugin does not know.
    static func trackingStatus(_ status: ATTrackingManager.AuthorizationStatus) -> AuthorizationStatusEnum? {
        switch status {
        case .authorized:
            return .Authorized
        case .denied:
            return .Denied
        case .restricted:
            return .Restricted
        case .notDetermined:
            return .NotDetermined
        @unknown default:
            return nil
        }
    }

    /**
     * Admob: User Message Platform
     * https://support.google.com/admob/answer/10113005?hl=en
     */
    @MainActor
    func requestConsentInfo(_ call: CAPPluginCall) async throws -> JSObject {
        let debugGeography = call.getInt("debugGeography", 0)

        let testDeviceJSArray = call.getArray("testDeviceIdentifiers") ?? []
        var testDeviceIdentifiers: [String] = []
        if testDeviceJSArray.count > 0 {
            for deviceId in testDeviceJSArray {
                if let name = deviceId as? String {
                    testDeviceIdentifiers.append(name)
                }
            }
        }

        let tagForUnderAgeOfConsent = call.getBool("tagForUnderAgeOfConsent", false)

        return try await consentExecutor.requestConsentInfo(debugGeography, testDeviceIdentifiers, tagForUnderAgeOfConsent)
    }

    @MainActor
    func showConsentForm(_ call: CAPPluginCall) async throws -> JSObject {
        try await consentExecutor.showConsentForm()
    }

    @MainActor
    func showPrivacyOptionsForm(_ call: CAPPluginCall) async throws {
        try await consentExecutor.showPrivacyOptionsForm()
    }

    func resetConsentInfo(_ call: CAPPluginCall) {
        DispatchQueue.main.async {
            self.consentExecutor.resetConsentInfo(call)
        }
    }

    private func getAdId(_ call: CAPPluginCall, _ testingID: String) -> String {
        let adUnitID = call.getString("adId") ?? testingID
        let isTest = call.getBool("isTesting") ?? false
        if isTest {
            return testingID
        }
        return adUnitID
    }

    private func GADRequestWithOption(_ npa: Bool) -> Request {
        let request = Request()

        if npa {
            let extras = Extras()
            extras.additionalParameters = ["npa": "1"]
            request.register(extras)
        }

        return request
    }

    /**
     * https://developers.google.com/admob/ios/targeting?hl=ja
     */
    private func setRequestConfiguration(_ call: CAPPluginCall) {

        if call.getBool("initializeForTesting") ?? false {
            MobileAds.shared.requestConfiguration.testDeviceIdentifiers = call.getArray("testingDevices", String.self) ?? []
        }

        if call.getBool("tagForChildDirectedTreatment") == true {
            MobileAds.shared.requestConfiguration.tagForChildDirectedTreatment = true
        }

        if call.getBool("tagForUnderAgeOfConsent") == true {
            MobileAds.shared.requestConfiguration.tagForUnderAgeOfConsent = true
        }

        if call.getString("maxAdContentRating") != nil {
            switch call.getString("maxAdContentRating") {
            case "General":
                MobileAds.shared.requestConfiguration.maxAdContentRating =
                    GADMaxAdContentRating.general
            case "ParentalGuidance":
                MobileAds.shared.requestConfiguration.maxAdContentRating =
                    GADMaxAdContentRating.parentalGuidance
            case "Teen":
                MobileAds.shared.requestConfiguration.maxAdContentRating =
                    GADMaxAdContentRating.teen
            case "MatureAudience":
                MobileAds.shared.requestConfiguration.maxAdContentRating =
                    GADMaxAdContentRating.matureAudience
            default:
                print("maxAdContentRating can't find value")
            }
        }

    }

    func getRootVC() -> UIViewController? {
        var window: UIWindow? = UIApplication.shared.delegate?.window ?? nil

        if window == nil {
            let scene: UIWindowScene? = UIApplication.shared.connectedScenes.first as? UIWindowScene
            window = scene?.windows.filter({$0.isKeyWindow}).first
            if window == nil {
                window = scene?.windows.first
            }
        }
        return window?.rootViewController
    }
}
