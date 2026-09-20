// swift-tools-version: 5.9
import Foundation
import PackageDescription

// Apps override this dependency with the @capacitor/ios they installed. To build this package on its own
// against a local runtime, point CAPACITOR_IOS_PATH at it.
let capacitor: Package.Dependency
if let path = ProcessInfo.processInfo.environment["CAPACITOR_IOS_PATH"] {
    capacitor = .package(name: "capacitor-swift-pm", path: path)
} else {
    capacitor = .package(url: "https://github.com/ionic-team/capacitor-swift-pm.git", from: "8.0.0")
}

let package = Package(
    name: "CapacitorCommunityAdmob",
    platforms: [.iOS(.v17)],
    products: [
        .library(
            name: "CapacitorCommunityAdmob",
            targets: ["AdMobPlugin"])
    ],
    dependencies: [
        capacitor,
        .package(url: "https://github.com/googleads/swift-package-manager-google-mobile-ads.git", exact: "13.6.0"),
        .package(url: "https://github.com/googleads/swift-package-manager-google-user-messaging-platform.git", .upToNextMinor(from: "3.1.0"))
    ],
    targets: [
        .target(
            name: "AdMobPlugin",
            dependencies: [
                .product(name: "Capacitor", package: "capacitor-swift-pm"),
                .product(name: "GoogleMobileAds", package: "swift-package-manager-google-mobile-ads"),
                .product(name: "GoogleUserMessagingPlatform", package: "swift-package-manager-google-user-messaging-platform")
            ],
            path: "ios/Sources/AdMobPlugin"),
        .testTarget(
            name: "AdMobPluginTests",
            dependencies: ["AdMobPlugin"],
            path: "ios/Tests/AdMobPluginTests")
    ]
)
