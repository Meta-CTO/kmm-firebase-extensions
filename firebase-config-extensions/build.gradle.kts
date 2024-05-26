import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework
import org.jetbrains.kotlin.konan.properties.Properties
import java.io.FileInputStream
import java.io.FileOutputStream

plugins {
    id(Plugins.ANDROID_LIBRARY)
    kotlin(Plugins.Kotlin.MULTIPLATFORM)
    kotlin(Plugins.Kotlin.SERIALIZATION) version Versions.Kotlin.KOTLIN
    id(Plugins.MAVEN_PUBLISH)
    id(Plugins.SIGNING)
}

val versionProperties = Properties().apply {
    load(FileInputStream(File(rootProject.rootDir, Constants.VERSIONS_PROPERTIES)))
}

val currentVersion = versionProperties.getProperty(Constants.PUBLISH_VERSION) as String
val libName = "firebase-remoteconfig-extensions"

version = currentVersion
group = Constants.GROUP_ID

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
            isStatic = true
        }
    }


    js(IR) {
        nodejs()
    }

    metadata {
        compilations.matching { it.name == "iosMain" }.all {
            compileTaskProvider.configure { enabled = false }
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(Libs.Firebase.CONFIG)
            implementation(Libs.KotlinX.SERIALIZATION)
            implementation(project(":remote-config-common"))
        }
    }

    task("testClasses")
}

android {
    namespace = "com.metacto.kmm.firebase.remoteconfig.extensions"
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

