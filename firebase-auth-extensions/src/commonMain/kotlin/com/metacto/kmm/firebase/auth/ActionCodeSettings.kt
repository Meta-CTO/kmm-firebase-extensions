package com.metacto.kmm.firebase.auth

data class ActionCodeSettings(
    val url: String,
    val androidPackageName: String,
    val deepLinkDomain: String? = null,
    val canHandleCodeInApp: Boolean = false,
    val installIfNotAvailable: Boolean = false,
    val iOSBundleId: String
)