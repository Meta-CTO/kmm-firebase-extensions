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
        }

        object Firebase {
            const val AUTH = "1.12.0-metacto-5"
            const val CONFIG = "1.10.4"
        }
    }

    object Plugins {
        const val SPM_4_KMP = "0.6.0"
    }
}