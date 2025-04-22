@file:OptIn(ExperimentalForeignApi::class)

package com.metacto.kmm.firebase.auth.extensions

import FirebaseAuth.FIRActionCodeSettings
import com.metacto.kmm.auth.common.PhoneVerifierMetadata
import com.metacto.kmm.auth.common.PhoneVerifierProvider
import com.metacto.kmm.firebase.auth.ActionCodeSettings
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSURL

@Throws(Throwable::class)
actual suspend fun FirebaseAuthenticator.getIdToken(forceRefresh: Boolean): String {
    return suspendCancellableCoroutine { cont ->
        FirebaseAuth.FIRAuth.auth().currentUser()
            ?.getIDTokenResultForcingRefresh(forceRefresh) { token, nsError ->
                if (nsError != null) {
                    cont.exceptionIfActive(Throwable(nsError.localizedDescription))
                } else if (token != null) {
                    cont.resumeIfActive(token.token())
                }
            }
    }
}

@Throws(Throwable::class)
actual suspend fun FirebaseAuthenticator.sendSignInLinkToEmail(
    email: String,
    actionCodeSettings: ActionCodeSettings
) {
    return suspendCancellableCoroutine { cont ->
        // Convert ActionCodeSettings to the iOS equivalent
        val actionCodeSettingsIOS = FIRActionCodeSettings()
        actionCodeSettingsIOS.setURL(NSURL(actionCodeSettings.url))
        actionCodeSettingsIOS.setHandleCodeInApp(actionCodeSettings.canHandleCodeInApp)
        actionCodeSettingsIOS.setIOSBundleID(actionCodeSettings.iOSBundleId)
        actionCodeSettingsIOS.setAndroidPackageName(actionCodeSettings.androidPackageName)
        actionCodeSettingsIOS.setAndroidInstallIfNotAvailable(actionCodeSettings.installIfNotAvailable)

        FirebaseAuth.FIRAuth.auth().sendSignInLinkToEmail(email, actionCodeSettingsIOS) { nsError ->
            if (nsError != null) {
                cont.exceptionIfActive(Throwable(nsError.localizedDescription))
            } else {
                cont.resumeIfActive(Unit)
            }
        }
    }
}

@Throws(Throwable::class)
actual suspend fun FirebaseAuthenticator.signInWithEmailLink(email: String, link: String): String {
    return suspendCancellableCoroutine { cont ->
        FirebaseAuth.FIRAuth.auth().signInWithEmail(email, link) { data, nsError ->
            if (nsError != null) {
                cont.exceptionIfActive(Throwable(nsError.localizedDescription))
            } else {
                data?.user()?.getIDTokenForcingRefresh(true) { token, nsError ->
                    if (nsError != null) {
                        cont.exceptionIfActive(Throwable(nsError.localizedDescription))
                    } else if (token != null) {
                        cont.resumeIfActive(token)
                    }
                }
            }
        }
    }
}

@Throws(Throwable::class)
actual suspend fun FirebaseAuthenticator.verifyPhoneNumber(otp: String, verificationId: String): String {
    return suspendCancellableCoroutine { cont ->
        val credential = FirebaseAuth.FIRPhoneAuthProvider.provider().credentialWithVerificationID(verificationId, otp)
        FirebaseAuth.FIRAuth.auth().signInWithCredential(credential) { data, nsError ->
            if (nsError != null) {
                cont.exceptionIfActive(Throwable(nsError.localizedDescription))
            } else {
                data?.user()?.getIDTokenForcingRefresh(true) { token, nsError ->
                    if (nsError != null) {
                        cont.exceptionIfActive(Throwable(nsError.localizedDescription))
                    } else if (token != null) {
                        cont.resumeIfActive(token)
                    }
                }
            }
        }
    }
}

@Throws(Throwable::class)
actual suspend fun FirebaseAuthenticator.sendSignInOTPToPhone(
    phoneNumber: String,
    phoneVerifierProvider: PhoneVerifierProvider
): PhoneVerifierMetadata {
    return suspendCancellableCoroutine { cont ->
        FirebaseAuth.FIRPhoneAuthProvider.provider().verifyPhoneNumber(phoneNumber, null) { verificationID, nsError ->
            if (nsError != null) {
                cont.exceptionIfActive(Throwable(nsError.localizedDescription))
            } else if (verificationID != null) {
                cont.resumeIfActive(PhoneVerifierMetadata(verificationID, phoneNumber))
            }
        }
    }
}

@Throws(Throwable::class)
actual suspend fun FirebaseAuthenticator.logout() {
    FirebaseAuth.FIRAuth.auth().signOut(null)
}