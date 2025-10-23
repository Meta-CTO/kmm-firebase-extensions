import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework
import org.jetbrains.kotlin.konan.properties.Properties
import java.io.FileInputStream
import java.io.FileOutputStream

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.multiplatform)
    id("maven-publish")
    id("signing")
}

val versionProperties = Properties().apply {
    load(FileInputStream(File(rootProject.rootDir, "versions.properties")))
}

val currentVersion = versionProperties.getProperty("PUBLISH_VERSION") as String
val libName = "remote-config-common"

version = currentVersion
group = "com.metacto.kmm"

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.fromTarget(libs.versions.java.version.get()))
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
            isStatic = true
        }
    }

    metadata {
        compilations.matching { it.name == "iosMain" }.all {
            compileTaskProvider.configure { enabled = false }
        }
    }

    sourceSets {
        commonMain.dependencies {}
        androidMain.dependencies {}
    }
}

android {
    namespace = "com.metacto.kmm.remoteconfig.common"
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

