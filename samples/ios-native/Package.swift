// swift-tools-version:5.5
// Created by Everysight LTD.
// Swift Package manifest for the native iOS Maverick AI sample.
import PackageDescription

let releaseName = "v0.2.0"
let checksumMav2Sdk = "68fde6ff9e1942c21c42a6e1ef6c5d5bbd5b8f9b216c95bb421a1c994f65dd92"

let package = Package(
    name: "MaverickAI",
    platforms: [
        .iOS(.v15)
    ],
    products: [
        .library(name: "MaverickAI", targets: ["MaverickAI"])
    ],
    targets: [
        .binaryTarget(
            name: "MaverickAI",
            url: "https://github.com/everysight-maverick-AI/mav-ai-ios-spm/releases/download/\(releaseName)/maverick-ai-sdk.xcframework.zip",
            checksum: "\(checksumMav2Sdk)"
        )
    ]
)
