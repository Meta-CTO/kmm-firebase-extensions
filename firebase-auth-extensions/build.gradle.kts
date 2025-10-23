import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework
import org.jetbrains.kotlin.konan.properties.Properties
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.URI

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.spm4kmp)
    id("maven-publish")
    id("signing")
}

val versionProperties = Properties().apply {
    load(FileInputStream(File(rootProject.rootDir, "versions.properties")))
}

val currentVersion = versionProperties.getProperty("PUBLISH_VERSION") as String
val libName = "firebase-auth-extensions"

version = currentVersion
group = "com.metacto.kmm"
val dependencies = "Dependencies"

kotlin {
    androidTarget {
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
                cinterops.create(dependencies)
            }
        }
    }

    swiftPackageConfig {
        create(dependencies) {
            minIos = "15.0"
            dependency {
                remotePackageVersion(
                    url = URI("https://github.com/firebase/firebase-ios-sdk.git"),
                    products = {
                        add("FirebaseCore", exportToKotlin = true)
                        add("FirebaseAuth", exportToKotlin = true)
                    },
                    version = "12.4.0",
                )
                remotePackageVersion(
                    url = URI("https://github.com/google/GoogleSignIn-iOS.git"),
                    products = {
                        add("GoogleSignIn", exportToKotlin = true)
                    },
                    version = "9.0.0",
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
            implementation(libs.kotlinx.coroutines.core)
            implementation(project(":auth-common"))
        }

        androidMain.dependencies {
            api(libs.play.services.auth)
        }
    }
}

android {
    namespace = "com.metacto.kmm.firebase.auth.extensions"
    compileSdk = libs.versions.android.compile.sdk.get().toInt()
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    defaultConfig {
        minSdk = libs.versions.android.min.sdk.get().toInt()
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.toVersion(libs.versions.java.version.get())
        targetCompatibility = JavaVersion.toVersion(libs.versions.java.version.get())
    }
}

publishing {
    repositories {
        val localProperties = gradleLocalProperties(rootDir, providers)
        var publishUserRepo = localProperties.getProperty("PUBLISH_REPO_USER")
        var publishTokenRepo = localProperties.getProperty("PUBLISH_REPO_TOKEN")

        if (publishUserRepo.isNullOrEmpty()) {
            publishUserRepo = ""
            localProperties.setProperty("PUBLISH_REPO_USER", publishUserRepo)
        }

        if (publishTokenRepo.isNullOrEmpty()) {
            publishTokenRepo = ""
            localProperties.setProperty("PUBLISH_REPO_TOKEN", publishTokenRepo)
        }

        if (publishUserRepo.isEmpty() || publishTokenRepo.isEmpty()) {
            localProperties.store(
                FileOutputStream(File(rootDir, "local.properties")), null
            )
        }

        repositories {
            maven("https://maven.pkg.github.com/Meta-CTO/kmm-firebase-extensions") {
                name = "Github"
                credentials {
                    username = publishUserRepo
                    password = publishTokenRepo
                }
            }
        }
    }
}

