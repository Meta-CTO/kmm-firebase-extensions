pluginManagement {
    repositories {
        gradlePluginPortal()
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        mavenLocal()
        maven(url = "https://jitpack.io")
    }
}

dependencyResolutionManagement {
    repositories {
        gradlePluginPortal()
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        mavenLocal()
        maven(url = "https://jitpack.io")
        maven {
            url = uri("https://maven.pkg.github.com/Meta-CTO/firebase-kotlin-sdk")
            credentials {
                username = "metactoengineer"
                password = "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"
            }
        }
    }
}

rootProject.name = "firebase-extensions"
include(":auth-common")
include(":firebase-auth-extensions")
include(":remote-config-common")
include(":firebase-config-extensions")
