// swift-tools-version:6.2
import PackageDescription

// BEGIN KMMBRIDGE VARIABLES BLOCK (do not edit)
let remoteKotlinUrl = "https://github.com/WashPost/kmp-classic/releases/download/vPlaceholder/KMPShared.xcframework.zip"
let remoteKotlinChecksum = "0000000000000000000000000000000000000000000000000000000000000000"
let packageName = "KMPShared"
// END KMMBRIDGE BLOCK

let package = Package(
    name: packageName,
    platforms: [
        .iOS(.v17)
    ],
    products: [
        .library(
            name: packageName,
            targets: [packageName]
        ),
    ],
    targets: [
        .binaryTarget(
            name: packageName,
            url: remoteKotlinUrl,
            checksum: remoteKotlinChecksum
        )
        ,
    ]
)