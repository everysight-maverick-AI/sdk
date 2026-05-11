// swift-tools-version:5.5
// Created by Everysight LTD.
// Swift Package manifest for the native iOS Maverick AI sample.
import PackageDescription

let releaseName = "v0.1.0"
let checksumMav2Sdk = "e1152c59bed7b32ee2b490b601c8f6e746ab3e3843f6c912671fd51140a48913"

let package = Package(
    name: "MaverickAI",
    platforms: [
        .iOS(.v15),
        .watchOS(.v8)
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
