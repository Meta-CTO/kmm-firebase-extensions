// swift-tools-version: 5.9
import PackageDescription

let package = Package(
  name: "exportedFirebaseAuthKMMSDK",
  platforms: [.iOS("12.0"), .macOS("10.13"), .tvOS("12.0"), .watchOS("4.0")],
  products: [
    .library(
      name: "exportedFirebaseAuthKMMSDK",
      type: .static,
      targets: ["exportedFirebaseAuthKMMSDK"])
  ],
  dependencies: [
    .package(url: "https://github.com/firebase/firebase-ios-sdk.git", exact: "11.11.0"),
    .package(url: "https://github.com/google/GoogleSignIn-iOS.git", exact: "8.0.0"),
  ],
  targets: [
    .target(
      name: "exportedFirebaseAuthKMMSDK",
      dependencies: [
        .product(name: "FirebaseCore", package: "firebase-ios-sdk"),
        .product(name: "FirebaseAuth", package: "firebase-ios-sdk"),
        .product(name: "GoogleSignIn", package: "GoogleSignIn-iOS"),
      ],
      path: "Sources"

    )

  ]
)
