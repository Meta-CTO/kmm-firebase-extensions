buildscript {
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
    dependencies {
        classpath(Build.KOTLIN_GRADLE_PLUGIN)
        classpath(Build.BUILD_TOOLS)
    }
}
