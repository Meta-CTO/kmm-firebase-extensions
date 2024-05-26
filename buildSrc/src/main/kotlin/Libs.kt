object Libs {
    object KotlinX {
        const val SERIALIZATION = "org.jetbrains.kotlinx:kotlinx-serialization-json:${Versions.Libs.KotlinX.SERIALIZATION}"
        const val COROUTINES = "org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.Libs.KotlinX.COROUTINES}"
    }

    object Android {
        const val ACTIVITY_KTX = "androidx.activity:activity-ktx:${Versions.Libs.Android.ACTIVITY_KTX}"
        const val FIREBASE_AUTH = "com.google.android.gms:play-services-auth:${Versions.Libs.Android.FIREBASE_AUTH}"
    }

    object Firebase {
        const val AUTH = "dev.gitlive:firebase-auth:${Versions.Libs.Firebase.AUTH}"
        const val CONFIG = "dev.gitlive:firebase-config:${Versions.Libs.Firebase.CONFIG}"
    }
}