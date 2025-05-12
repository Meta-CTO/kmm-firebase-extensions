@file:OptIn(ExperimentalForeignApi::class)

package com.metacto.kmm.firebase.auth.extensions

import FirebaseAuth.FIRActionCodeSettings
import FirebaseAuth.FIRAuth
import FirebaseAuth.FIRAuthDataResult
import FirebaseAuth.FIRUser
import com.metacto.kmm.auth.common.PhoneVerifierMetadata
import com.metacto.kmm.auth.common.PhoneVerifierProvider
import com.metacto.kmm.firebase.auth.ActionCodeSettings
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSError
import platform.Foundation.NSURL

@Throws(Throwable::class)
actual suspend fun FirebaseAuthenticator.getIdToken(forceRefresh: Boolean): String {
    return suspendCancellableCoroutine { continuation ->
        getIDTokenFromUser(FIRAuth.auth().currentUser(), continuation)
    }
}

@Throws(Throwable::class)
actual suspend fun FirebaseAuthenticator.sendSignInLinkToEmail(
    email: String,
    actionCodeSettings: ActionCodeSettings
) {
    return suspendCancellableCoroutine { continuation ->
        val actionCodeSettingsIOS = FIRActionCodeSettings()
        actionCodeSettingsIOS.setURL(NSURL(string = actionCodeSettings.url))
        actionCodeSettingsIOS.setHandleCodeInApp(actionCodeSettings.canHandleCodeInApp)
        actionCodeSettingsIOS.setIOSBundleID(actionCodeSettings.iOSBundleId)
        actionCodeSettingsIOS.setAndroidPackageName(actionCodeSettings.androidPackageName)
        actionCodeSettingsIOS.setAndroidInstallIfNotAvailable(actionCodeSettings.installIfNotAvailable)

        FIRAuth.auth().sendSignInLinkToEmail(email, actionCodeSettingsIOS) { error ->
            if (error != null) {
                continuation.exceptionIfActive(Throwable(error.localizedDescription))
            } else {
                continuation.resumeIfActive(Unit)
            }
        }
    }
}

@Throws(Throwable::class)
actual suspend fun FirebaseAuthenticator.signInWithEmailLink(email: String, link: String): String {
    return suspendCancellableCoroutine { continuation ->
        FIRAuth.auth().signInWithEmail(email = email, link = link) { result: FIRAuthDataResult?, error: NSError? ->
            if (error != null) {
                continuation.exceptionIfActive(Throwable(error.localizedDescription))
            } else {
                getIDTokenFromUser(result?.user(), continuation)
            }
        }
    }
}

@Throws(Throwable::class)
actual suspend fun FirebaseAuthenticator.verifyPhoneNumber(otp: String, verificationId: String): String {
    return suspendCancellableCoroutine { continuation ->
        val credential = FirebaseAuth.FIRPhoneAuthProvider.provider().credentialWithVerificationID(verificationId, otp)
        FIRAuth.auth().signInWithCredential(credential) { result: FIRAuthDataResult?, error: NSError? ->
            if (error != null) {
                continuation.exceptionIfActive(Throwable(error.localizedDescription))
            } else {
                getIDTokenFromUser(result?.user(), continuation)
            }
        }
    }
}

@Throws(Throwable::class)
actual suspend fun FirebaseAuthenticator.sendSignInOTPToPhone(
    phoneNumber: String,
    phoneVerifierProvider: PhoneVerifierProvider?
): PhoneVerifierMetadata {
    return suspendCancellableCoroutine { continuation ->
        FirebaseAuth.FIRPhoneAuthProvider.provider().verifyPhoneNumber(phoneNumber, null) { verificationID, error ->
            if (error != null) {
                continuation.exceptionIfActive(Throwable(error.localizedDescription))
            } else if (verificationID != null) {
                continuation.resumeIfActive(PhoneVerifierMetadata(verificationID, phoneNumber))
            } else {
                continuation.exceptionIfActive(Throwable("Unable to send sign in OTP due to unknown error"))
            }
        }
    }
}

@Throws(Throwable::class)
actual suspend fun FirebaseAuthenticator.logout() {
    FIRAuth.auth().signOut(null)
}

private fun getIDTokenFromUser(
    user: FIRUser?,
    continuation: CancellableContinuation<String>
) {
    if (user == null) {
        continuation.exceptionIfActive(Throwable("Unable to get id token from a null user"))
        return
    }

    user.getIDTokenForcingRefresh(true) { token, error ->
        if (error != null) {
            continuation.exceptionIfActive(Throwable(error.localizedDescription))
        } else if (token != null) {
            continuation.resumeIfActive(token)
        } else {
            continuation.exceptionIfActive(Throwable("Unable to get id token due to unknown error"))
        }
    }
}