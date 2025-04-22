package com.metacto.kmm.firebase.auth.extensions

import com.google.firebase.Firebase
import com.google.firebase.FirebaseException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.auth
import com.metacto.kmm.auth.common.PhoneVerifierMetadata
import com.metacto.kmm.auth.common.PhoneVerifierProvider
import com.metacto.kmm.firebase.auth.ActionCodeSettings
import kotlinx.coroutines.suspendCancellableCoroutine

@Throws(Throwable::class)
actual suspend fun FirebaseAuthenticator.getIdToken(forceRefresh: Boolean): String {
    return Firebase.auth.currentUser?.getIdToken(forceRefresh)?.result?.token
        ?: throw Throwable("Failed to get ID token")
}

@Throws(Throwable::class)
actual suspend fun FirebaseAuthenticator.sendSignInLinkToEmail(
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
actual suspend fun FirebaseAuthenticator.signInWithEmailLink(email: String, link: String): String {
    return Firebase.auth.signInWithEmailLink(email, link).result?.user?.getIdToken(true)?.result?.token
        ?: throw Throwable("Failed to sign in with email link")
}

@Throws(Throwable::class)
actual suspend fun FirebaseAuthenticator.verifyPhoneNumber(otp: String, verificationId: String): String {
    val credential = PhoneAuthProvider.getCredential(verificationId, otp)
    return Firebase.auth.signInWithCredential(credential).result?.user?.getIdToken(true)?.result?.token
        ?: throw Throwable("Failed to verify phone number")
}

@Throws(Throwable::class)
actual suspend fun FirebaseAuthenticator.sendSignInOTPToPhone(phoneNumber: String, phoneVerifierProvider: PhoneVerifierProvider): PhoneVerifierMetadata {
    return suspendCancellableCoroutine { continuation ->

        val options = PhoneAuthOptions.newBuilder(Firebase.auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(phoneVerifierProvider.timeout, phoneVerifierProvider.unit)
            .setActivity(phoneVerifierProvider.activity)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(p0: PhoneAuthCredential) {
                    // NOT USED HERE
                }

                override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken,
                ) {
                    continuation.resumeIfActive(
                        PhoneVerifierMetadata(
                            verificationId = verificationId,
                            phoneNumber
                        )
                    )
                }

                override fun onVerificationFailed(p0: FirebaseException) {
                    continuation.exceptionIfActive(Throwable(p0))
                }
            })
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }
}

@Throws(Throwable::class)
actual suspend fun FirebaseAuthenticator.logout() {
    Firebase.auth.signOut()
}

