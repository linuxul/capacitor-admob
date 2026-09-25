import Foundation
import Capacitor
import GoogleMobileAds
import UserMessagingPlatform

class ConsentExecutor: NSObject {
    weak var plugin: AdMobPlugin?

    /// Updates the consent information and returns it. The UMP SDK is used from the main thread only.
    @MainActor
    func requestConsentInfo(_ debugGeography: Int, _ testDeviceIdentifiers: [String], _ tagForUnderAgeOfConsent: Bool) async throws -> JSObject {
        let parameters = RequestParameters()
        let debugSettings = DebugSettings()

        debugSettings.geography = DebugGeography(rawValue: debugGeography) ?? DebugGeography.disabled
        debugSettings.testDeviceIdentifiers = testDeviceIdentifiers

        parameters.debugSettings = debugSettings
        parameters.isTaggedForUnderAgeOfConsent = tagForUnderAgeOfConsent

        // Request an update to the consent information.
        do {
            try await ConsentInformation.shared.requestConsentInfoUpdate(with: parameters)
        } catch {
            throw CAPPluginError("Request consent info failed", underlyingError: error)
        }
        return [
            "status": self.getConsentStatusString(ConsentInformation.shared.consentStatus),
            "isConsentFormAvailable": ConsentInformation.shared.formStatus == FormStatus.available,
            "canRequestAds": ConsentInformation.shared.canRequestAds,
            "privacyOptionsRequirementStatus": self.getPrivacyOptionsRequirementStatus(ConsentInformation.shared.privacyOptionsRequirementStatus)
        ]
    }

    @MainActor
    func showPrivacyOptionsForm() async throws {
        guard let rootViewController = plugin?.getRootVC() else {
            throw CAPPluginError("No ViewController")
        }

        do {
            try await ConsentForm.presentPrivacyOptionsForm(from: rootViewController)
        } catch {
            throw CAPPluginError("Failed to show privacy options form: \(error.localizedDescription)", underlyingError: error)
        }
    }

    /// Loads and presents the consent form if it is required, and returns the consent information afterwards.
    @MainActor
    func showConsentForm() async throws -> JSObject {
        guard let rootViewController = plugin?.getRootVC() else {
            throw CAPPluginError("No ViewController")
        }
        guard ConsentInformation.shared.formStatus == FormStatus.available else {
            throw CAPPluginError("Consent Form not available")
        }

        do {
            try await ConsentForm.loadAndPresentIfRequired(from: rootViewController)
        } catch {
            throw CAPPluginError("Request consent info failed", underlyingError: error)
        }
        return [
            "status": self.getConsentStatusString(ConsentInformation.shared.consentStatus),
            "canRequestAds": ConsentInformation.shared.canRequestAds,
            "privacyOptionsRequirementStatus": self.getPrivacyOptionsRequirementStatus(ConsentInformation.shared.privacyOptionsRequirementStatus)
        ]
    }

    func resetConsentInfo(_ call: CAPPluginCall) {
        ConsentInformation.shared.reset()
        call.resolve()
    }

    func getConsentStatusString(_ consentStatus: ConsentStatus) -> String {
        switch consentStatus {
        case ConsentStatus.required:
            return "REQUIRED"
        case ConsentStatus.notRequired:
            return "NOT_REQUIRED"
        case ConsentStatus.obtained:
            return "OBTAINED"
        default:
            return "UNKNOWN"
        }
    }

    func getPrivacyOptionsRequirementStatus(_ requirementStatus: PrivacyOptionsRequirementStatus) -> String {
        switch requirementStatus {
        case PrivacyOptionsRequirementStatus.required:
            return "REQUIRED"
        case PrivacyOptionsRequirementStatus.notRequired:
            return "NOT_REQUIRED"
        default:
            return "UNKNOWN"
        }
    }
}
