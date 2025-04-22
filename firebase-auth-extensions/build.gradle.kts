import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework
import org.jetbrains.kotlin.konan.properties.Properties
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.URI

plugins {
    id(Plugins.ANDROID_LIBRARY)
    kotlin(Plugins.Kotlin.MULTIPLATFORM)
    id(Plugins.SPM_4_KMP) version Versions.Plugins.SPM_4_KMP
    id(Plugins.MAVEN_PUBLISH)
    id(Plugins.SIGNING)
}

val versionProperties = Properties().apply {
    load(FileInputStream(File(rootProject.rootDir, Constants.VERSIONS_PROPERTIES)))
}

val currentVersion = versionProperties.getProperty(Constants.PUBLISH_VERSION) as String
val libName = "firebase-auth-extensions"

version = currentVersion
group = Constants.GROUP_ID
val firebaseCinteropName = "FirebaseSDK"

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = Versions.JAVA.VERSION.toString()
            }
        }
        publishLibraryVariants("debug", "release")
    }

    val xcf = XCFramework()
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework(libName) {
            baseName = libName
            xcf.add(this)
        }
        it.compilations {
            val main by getting {
                cinterops.create(firebaseCinteropName)
            }
        }
    }

    swiftPackageConfig {
        create(firebaseCinteropName) {
            dependency {
                remotePackageVersion(
                    url = URI("https://github.com/firebase/firebase-ios-sdk.git"),
                    products = {
                        add("FirebaseCore", exportToKotlin = true)
                        add("FirebaseAuth", exportToKotlin = true)
                    },
                    version = "11.11.0",
                )
                remotePackageVersion(
                    url = URI("https://github.com/google/GoogleSignIn-iOS.git"),
                    products = {
                        add("GoogleSignIn", exportToKotlin = true)
                    },
                    version = "8.0.0",
                )
            }
        }
    }

    metadata {
        compilations.matching { it.name == "iosMain" }.all {
            compileTaskProvider.configure { enabled = false }
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(Libs.KotlinX.COROUTINES)
            implementation(project(":auth-common"))
        }

        androidMain.dependencies {
            api(Libs.Android.GOOGLE_AUTH)
            api(Libs.Android.FIREBASE_AUTH)
        }
    }

    task("testClasses")
}

android {
    namespace = "com.metacto.kmm.firebase.auth.extensions"
    compileSdk = Versions.Android.COMPILE_SDK
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    defaultConfig {
        minSdk = Versions.Android.MIN_SDK
        consumerProguardFiles("consumer-rules.pro")
    }
    compileOptions {
        sourceCompatibility = Versions.JAVA.VERSION
        targetCompatibility = Versions.JAVA.VERSION
    }
}

publishing {
    repositories {
        val localProperties = gradleLocalProperties(rootDir, providers)
        var publishUserRepo = localProperties.getProperty(Constants.PUBLISH_REPO_USER)
        var publishTokenRepo = localProperties.getProperty(Constants.PUBLISH_REPO_TOKEN)

        if (publishUserRepo.isNullOrEmpty()) {
            publishUserRepo = ""
            localProperties.setProperty(Constants.PUBLISH_REPO_USER, publishUserRepo)
        }

        if (publishTokenRepo.isNullOrEmpty()) {
            publishTokenRepo = ""
            localProperties.setProperty(Constants.PUBLISH_REPO_TOKEN, publishTokenRepo)
        }

        if (publishUserRepo.isEmpty() || publishTokenRepo.isEmpty()) {
            localProperties.store(
                FileOutputStream(File(rootDir, Constants.LOCAL_PROPERTIES)), null
            )
        }

        repositories {
            maven(Constants.MAVEN_URL) {
                name = Constants.PUBLISH_MAVEN_REPO_NAME
                credentials {
                    username = publishUserRepo
                    password = publishTokenRepo
                }
            }
        }
    }
}

