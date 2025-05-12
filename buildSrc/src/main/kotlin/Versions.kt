import org.gradle.api.JavaVersion

object Versions {
    object JAVA {
        val VERSION = JavaVersion.VERSION_17
    }

    object Kotlin {
        const val KOTLIN = "2.1.10"
    }

    object Android {
        const val MIN_SDK = 26
        const val COMPILE_SDK = 34
    }

    object Gradle {
        const val BUILD_TOOLS = "8.8.2"
    }

    object Libs {
        object KotlinX {
            const val SERIALIZATION = "1.6.3"
            const val COROUTINES = "1.8.1"
        }

        object Android {
            const val ACTIVITY_KTX = "1.9.0"
            const val GOOGLE_AUTH = "21.3.0"
            const val FIREBASE_AUTH = "23.2.0"
            const val FIREBASE_CONFIG = "22.1.1"
        }

        const val LOGGER = "1.0.15"
        const val KMM_PREFERENCES = "1.0.3"
    }

    object Plugins {
        const val SPM_4_KMP = "0.6.0"
    }
}