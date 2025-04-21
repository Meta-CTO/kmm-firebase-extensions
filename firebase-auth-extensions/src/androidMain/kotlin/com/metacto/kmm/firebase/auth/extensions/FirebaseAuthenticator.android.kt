package com.metacto.kmm.firebase.auth.extensions

import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.metacto.kmm.auth.common.PhoneVerifierMetadata
import com.metacto.kmm.firebase.auth.ActionCodeSettings

@Throws(Throwable::class)
actual fun FirebaseAuthenticator.getIdToken(forceRefresh: Boolean): String {
    return Firebase.auth.currentUser?.getIdToken(forceRefresh)?.result?.token
        ?: throw Throwable("Failed to get ID token")
}

@Throws(Throwable::class)
actual fun FirebaseAuthenticator.sendSignInLinkToEmail(
    email: String,
    actionCodeSettings: ActionCodeSettings
) {
    val authActionCodeSettings = com.google.firebase.auth.ActionCodeSettings.newBuilder()
        .setUrl(actionCodeSettings.url)
        .setHandleCodeInApp(actionCodeSettings.canHandleCodeInApp)
        .setIOSBundleId(actionCodeSettings.iOSBundleId)
        .setAndroidPackageName(
            actionCodeSettings.androidPackageName,
            actionCodeSettings.installIfNotAvailable,
            null
        )
        .build()
    Firebase.auth.sendSignInLinkToEmail(email, authActionCodeSettings).result
}

@Throws(Throwable::class)
actual fun FirebaseAuthenticator.signInWithEmailLink(email: String, link: String): String {
    return Firebase.auth.signInWithEmailLink(email, link).result?.user?.getIdToken(true)?.result?.token
        ?: throw Throwable("Failed to sign in with email link")
}

@Throws(Throwable::class)
actual fun FirebaseAuthenticator.verifyPhoneNumber(otp: String, verificationId: String): String {
    val credential = com.google.firebase.auth.PhoneAuthProvider.getCredential(verificationId, otp)
    return Firebase.auth.signInWithCredential(credential).result?.user?.getIdToken(true)?.result?.token
        ?: throw Throwable("Failed to verify phone number")
}

@Throws(Throwable::class)
actual fun FirebaseAuthenticator.sendSignInOTPToPhone(phoneNumber: String): PhoneVerifierMetadata {
    val phoneAuth
    val verificationId = Firebase.auth.verifyPhoneNumber(
        com.google.firebase.auth.PhoneAuthOptions.newBuilder(Firebase.auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, java.util.concurrent.TimeUnit.SECONDS)
            .setActivity(options.activity)
            .setCallbacks(options.phoneAuthProviderCallbacks)
            .build()
    ).result?.verificationId
        ?: throw Throwable("Failed to send OTP to phone number")
    return PhoneVerifierMetadata(verificationId = verificationId)
}