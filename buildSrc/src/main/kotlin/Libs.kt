object Libs {
    object KotlinX {
        const val SERIALIZATION = "org.jetbrains.kotlinx:kotlinx-serialization-json:${Versions.Libs.KotlinX.SERIALIZATION}"
        const val COROUTINES = "org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.Libs.KotlinX.COROUTINES}"
    }

    object Android {
        const val ACTIVITY_KTX = "androidx.activity:activity-ktx:${Versions.Libs.Android.ACTIVITY_KTX}"
        const val GOOGLE_AUTH = "com.google.android.gms:play-services-auth:${Versions.Libs.Android.GOOGLE_AUTH}"
        const val FIREBASE_AUTH = "com.google.firebase:firebase-auth-ktx:${Versions.Libs.Android.FIREBASE_AUTH}"
        const val FIREBASE_CONFIG = "com.google.firebase:firebase-config-ktx:${Versions.Libs.Android.FIREBASE_CONFIG}"
    }

    const val LOGGER = "com.metacto:logger:${Versions.Libs.LOGGER}"
    const val KMM_PREFERENCES = "com.metacto:sharedpreferences:${Versions.Libs.KMM_PREFERENCES}"
}