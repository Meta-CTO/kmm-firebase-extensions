# KMM Firebase Remote Config Module Setup Guide

This guide explains how to set up a Kotlin Multiplatform Mobile (KMM) module that integrates native iOS Swift/Objective-C code using the SPM_4_KMP plugin.

## Architecture Overview

This module demonstrates a complete setup for integrating Firebase Remote Config into a KMM project with:
- Common Kotlin code shared between platforms
- Android-specific implementation using Firebase Android SDK
- iOS-specific implementation using Swift Package Manager (SPM) and native Swift code
- Automatic bridging between Kotlin and Swift/Objective-C using SPM_4_KMP plugin

## Module Structure

```
firebase-config-extensions/
├── build.gradle.kts              # Module configuration with SPM_4_KMP setup
├── src/
│   ├── commonMain/              # Shared Kotlin interfaces
│   │   └── kotlin/.../
│   │       └── FirebaseRemoteConfigsProvider.kt  # expect class declaration
│   ├── androidMain/             # Android-specific implementation
│   │   └── kotlin/.../
│   │       └── FirebaseRemoteConfigsProvider.kt  # actual Android implementation
│   ├── iosMain/                 # iOS-specific Kotlin implementation
│   │   └── kotlin/.../
│   │       └── FirebaseRemoteConfigsProvider.kt  # actual iOS implementation
│   └── swift/                   # Native Swift code
│       └── Dependencies/
│           └── FirebaseRemoteConfigsProvider.swift  # Swift implementation
```

## Key Components

### 1. SPM_4_KMP Plugin Configuration

The plugin is applied in `build.gradle.kts`:

```kotlin
plugins {
    id("io.github.frankois944.spmForKmp") version "0.6.0"
}
```

### 2. Swift Package Configuration

The `swiftPackageConfig` block defines external SPM dependencies:

```kotlin
swiftPackageConfig {
    create("Dependencies") {  // Creates a cinterop named "Dependencies"
        dependency {
            remotePackageVersion(
                url = URI("https://github.com/firebase/firebase-ios-sdk.git"),
                products = {
                    add("FirebaseCore", exportToKotlin = false)
                    add("FirebaseRemoteConfig", exportToKotlin = false)
                },
                version = "11.11.0",
            )
        }
    }
}
```

**Key Points:**
- `create("Dependencies")` creates a cinterop configuration named "Dependencies"
- This name must match the cinterop name in the iOS target configuration
- `exportToKotlin = false` means the Firebase SDK won't be directly exposed to Kotlin
- Instead, we wrap it in our Swift code

### 3. iOS Target Configuration

```kotlin
listOf(
    iosX64(),
    iosArm64(),
    iosSimulatorArm64()
).forEach {
    it.binaries.framework(libName) {
        baseName = libName
        xcf.add(this)
        isStatic = true
    }
    it.compilations {
        val main by getting {
            cinterops.create("Dependencies")  // Must match swiftPackageConfig name
        }
    }
}
```

### 4. Swift Implementation

The Swift code in `src/swift/Dependencies/FirebaseRemoteConfigsProvider.swift`:
- Must be marked with `@objc` for Objective-C interoperability
- Class must inherit from `NSObject`
- All public methods must be marked with `@objc public`
- Uses completion handlers for async operations (compatible with Kotlin coroutines)

```swift
@objc public class FirebaseRemoteConfigsProvider: NSObject {
    @objc public func fetchConfigsFromRemote(completion: @escaping ([String: Any]?, NSError?) -> Void) {
        // Implementation
    }
}
```

### 5. Kotlin iOS Implementation

In `iosMain/FirebaseRemoteConfigsProvider.kt`:
- Uses `kotlinx.cinterop.ExperimentalForeignApi` for native interop
- Accesses Swift class through the generated `Dependencies` object
- Converts between Kotlin coroutines and completion handlers

```kotlin
@OptIn(ExperimentalForeignApi::class)
actual class FirebaseRemoteConfigsProvider {
    private val firebaseConfigs = Dependencies.FirebaseRemoteConfigsProvider()
    
    private suspend fun fetchConfigsFromRemote() {
        return suspendCancellableCoroutine { continuation ->
            firebaseConfigs.fetchConfigsFromRemoteWithCompletion { values, error ->
                // Handle completion
            }
        }
    }
}
```

## How SPM_4_KMP Works

### Overview
The SPM_4_KMP plugin (Swift Package Manager for Kotlin Multiplatform) is an alternative to the CocoaPods plugin for integrating Swift code into KMP projects. It creates a bridge between Swift and Kotlin without requiring external dependencies.

### Key Concepts

1. **Build Time Generation**: The plugin generates:
   - A `.def` file for cinterop configuration
   - Kotlin bindings for the Swift/Objective-C code
   - A `Dependencies` object containing all exposed classes

2. **Swift Code Location**: Swift files must be placed in `src/swift/[CinteropName]/`
   - In this case: `src/swift/Dependencies/`
   - The directory name MUST match the cinterop name

3. **Bridge Mechanism**: 
   - Pure Swift packages can't be directly exported to Kotlin
   - The plugin creates a Swift<->Kotlin bridge using Objective-C compatibility
   - The bridge directory is a symbolic link (not a copy) for better developer experience

4. **Automatic Bridging**: The plugin handles:
   - Swift to Objective-C bridging
   - Objective-C to Kotlin/Native bindings
   - Type conversions for basic types

5. **Name Mapping**: Swift method names are transformed:
   - `fetchConfigsFromRemote(completion:)` becomes `fetchConfigsFromRemoteWithCompletion`
   - Parameters are appended to method names

### Local Swift Code vs External Packages

**For Local Swift Code** (like in this example):
- Place Swift files in `src/swift/[CinteropName]/`
- No package declaration needed in `swiftPackageConfig`
- Direct access through generated bindings

**For External SPM Packages**:
- Declare dependencies in `swiftPackageConfig`
- Use `remotePackageVersion` for external packages
- Set `exportToKotlin = false` and wrap in Swift bridge

## Setting Up a New Module

To create a similar module for kmm-download-manager:

### 0. Prerequisites

In your project's `gradle.properties`, add:

```properties
kotlin.mpp.enableCInteropCommonization=true
```

This is required for the SPM_4_KMP plugin to work properly.

### 1. Module Configuration

Create `build.gradle.kts`:

```kotlin
plugins {
    id("com.android.library")
    kotlin("multiplatform")
    id("io.github.frankois944.spmForKmp") version "0.6.0"
}

kotlin {
    androidTarget()
    
    val xcf = XCFramework()
    listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach {
        it.binaries.framework("kmm-download-manager") {
            baseName = "kmm-download-manager"
            xcf.add(this)
            isStatic = true
        }
        it.compilations {
            val main by getting {
                cinterops.create("DownloadManager")
            }
        }
    }
    
    // For local Swift code (not using external SPM packages)
    swiftPackageConfig {
        create("DownloadManager") {
            // No external dependencies needed for local code
        }
    }
}
```

### 2. Create Swift Implementation

Place in `src/swift/DownloadManager/DownloadManager.swift`:

```swift
import Foundation

@objc public class DownloadManager: NSObject {
    @objc public func startDownload(url: String, completion: @escaping (NSError?) -> Void) {
        // Implementation
    }
}
```

### 3. Create Kotlin Expect Class

In `commonMain`:

```kotlin
expect class DownloadManager {
    suspend fun startDownload(url: String)
}
```

### 4. Create iOS Actual Implementation

In `iosMain`:

```kotlin
@OptIn(ExperimentalForeignApi::class)
actual class DownloadManager {
    private val nativeManager = DownloadManager.DownloadManager()
    
    actual suspend fun startDownload(url: String) {
        return suspendCancellableCoroutine { continuation ->
            nativeManager.startDownloadWithCompletion(url) { error ->
                // Handle completion
            }
        }
    }
}
```

## Important Notes

1. **Swift File Location**: Must match the cinterop name (`src/swift/[CinteropName]/`)
2. **Objective-C Compatibility**: All exposed Swift code must be Objective-C compatible
3. **Static Framework**: iOS frameworks should be static (`isStatic = true`)
4. **Completion Handlers**: Use for async operations (automatically bridged to coroutines)
5. **Type Limitations**: Only Objective-C compatible types can cross the bridge

## Troubleshooting

1. **"Unresolved reference: Dependencies"**: 
   - Ensure cinterop name matches in both `swiftPackageConfig` and `cinterops.create()`
   - Clean and rebuild the project

2. **Swift code not found**:
   - Verify Swift files are in correct directory
   - Check that classes/methods are marked with `@objc`

3. **Type conversion issues**:
   - Use only Objective-C compatible types
   - Complex types may need custom conversion

## Benefits of This Approach

1. **Native Performance**: Direct access to platform SDKs
2. **Type Safety**: Kotlin type checking with native code
3. **Coroutine Support**: Automatic bridging of async operations
4. **No CocoaPods Required**: Uses modern SPM for dependencies
5. **Single Source of Truth**: One Swift implementation for all iOS targets

## SPM_4_KMP vs CocoaPods

### Advantages of SPM_4_KMP:
- **No Third-Party Dependencies**: Uses embedded Swift Package Manager
- **Less Intrusive**: Doesn't require Ruby or CocoaPods installation
- **Modern Approach**: Aligns with Apple's recommended package management
- **Symbolic Links**: Bridge directory is a symbolic link for better developer experience
- **Direct Compilation Error Feedback**: Can modify bridge code directly from build output

### When to Use SPM_4_KMP:
- When you need to integrate Swift-only iOS libraries
- When you want to write custom native iOS code for your KMP project
- When you prefer SPM over CocoaPods for dependency management
- When you need fine-grained control over the Swift-Kotlin bridge

## Additional Resources

- [SPM_4_KMP GitHub Repository](https://github.com/frankois944/spm4Kmp)
- [SPM_4_KMP Documentation](https://frankois944.github.io/spm4Kmp/)
- [Gradle Plugin Portal](https://plugins.gradle.org/plugin/io.github.frankois944.spmForKmp)