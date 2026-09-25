import XCTest
import AppTrackingTransparency
import Capacitor
@testable import AdMobPlugin

// These tests never reach the Google Mobile Ads or UMP SDKs: every call below stops at a check before it, or only
// reads the plugin's own state.
class AdMobTests: XCTestCase {
    func testTrackingStatusesMapToTheirNames() {
        XCTAssertEqual(AdMobPlugin.trackingStatus(.authorized)?.rawValue, "authorized")
        XCTAssertEqual(AdMobPlugin.trackingStatus(.denied)?.rawValue, "denied")
        XCTAssertEqual(AdMobPlugin.trackingStatus(.restricted)?.rawValue, "restricted")
        XCTAssertEqual(AdMobPlugin.trackingStatus(.notDetermined)?.rawValue, "notDetermined")
    }

    func testSettingsWithoutAValueAreRejected() {
        let plugin = AdMobPlugin()
        let muted = thrownError(plugin.setApplicationMuted, "setApplicationMuted")
        XCTAssertEqual(muted?.message, "muted property cannot be null")
        XCTAssertNil(muted?.code)
        let volume = thrownError(plugin.setApplicationVolume, "setApplicationVolume")
        XCTAssertEqual(volume?.message, "volume property cannot be null")
        XCTAssertNil(volume?.code)
    }

    func testNoAppOpenAdIsLoadedBeforeOneIsPrepared() async {
        let plugin = AdMobPlugin()
        let withoutId = await plugin.isAppOpenLoaded(unansweredCall("isAppOpenLoaded"))
        XCTAssertEqual(withoutId["value"] as? Bool, false)
        let withId = await plugin.isAppOpenLoaded(unansweredCall("isAppOpenLoaded", ["adId": "ca-app-pub-3940256099942544/5575463023"]))
        XCTAssertEqual(withId["value"] as? Bool, false)
    }

    /// The error `method` throws, which the bridge rejects the call with; nil when it does not throw.
    private func thrownError(_ method: (CAPPluginCall) throws -> Void, _ name: String) -> CAPPluginError? {
        do {
            try method(unansweredCall(name))
            XCTFail("\(name) must throw")
            return nil
        } catch let error as CAPPluginError {
            return error
        } catch {
            XCTFail("unexpected error \(error)")
            return nil
        }
    }

    private func unansweredCall(_ method: String, _ options: JSObject = [:]) -> CAPPluginCall {
        return CAPPluginCall(callbackId: "test", methodName: method, options: options, success: { _, _ in
            XCTFail("\(method) answers by returning or throwing")
        }, error: { _ in
            XCTFail("\(method) answers by returning or throwing")
        })
    }
}
