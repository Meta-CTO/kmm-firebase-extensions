@file:OptIn(ExperimentalForeignApi::class)

package com.metacto.kmm.firebase.auth.extensions

import FirebaseAuth.FIRActionCodeSettings
import FirebaseAuth.FIRAuth
import com.metacto.kmm.auth.common.PhoneVerifierMetadata
import com.metacto.kmm.auth.common.PhoneVerifierProvider
import com.metacto.kmm.firebase.auth.ActionCodeSettings
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSURL
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Throws(Throwable::class)
actual suspend fun FirebaseAuthenticator.getIdToken(forceRefresh: Boolean): String {
    return suspendCancellableCoroutine { continuation ->
        val user = FIRAuth.auth().currentUser()

        if (user == null) {
            continuation.resumeWithException(Throwable("Unable to get id token from a null user"))
            return@suspendCancellableCoroutine
        }

        user.getIDTokenForcingRefresh(forceRefresh) { token, error ->
            if (error != null) {
                continuation.resumeWithException(Throwable(error.localizedDescription))
                return@getIDTokenForcingRefresh
            }

            if (token == null) {
                continuation.resumeWithException(Throwable("Unable to get id token due to unknown error"))
                return@getIDTokenForcingRefresh
            }

            continuation.resume(token)
        }
    }
}

@Throws(Throwable::class)
actual suspend fun FirebaseAuthenticator.sendSignInLinkToEmail(
    email: String,
    actionCodeSettings: ActionCodeSettings
) {
    return suspendCancellableCoroutine { continuation ->
        val actionCodeSettingsIOS = actionCodeSettings.toIOS()

        FIRAuth.auth().sendSignInLinkToEmail(email, actionCodeSettingsIOS) { error ->
            if (error != null) {
                continuation.resumeWithException(Throwable(error.localizedDescription))
                return@sendSignInLinkToEmail
            }

            continuation.resume(Unit)
        }
    }
}

@Throws(Throwable::class)
actual suspend fun FirebaseAuthenticator.signInWithEmailAndPassword(
    email: String,
    password: String
): String {
    return suspendCancellableCoroutine { continuation ->
        FIRAuth.auth().signInWithEmail(email = email, password = password) { result, error ->
            if (error != null) {
                continuation.resumeWithException(Throwable(error.localizedDescription))
                return@signInWithEmail
            }

            val user = result?.user()
            if (user == null) {
                continuation.resumeWithException(Throwable("Unable to get id token from a null user"))
                return@signInWithEmail
            }

            user.getIDTokenForcingRefresh(true) { token, tokenError ->
                if (tokenError != null) {
                    continuation.resumeWithException(Throwable(tokenError.localizedDescription))
                    return@getIDTokenForcingRefresh
                }

                if (token == null) {
                    continuation.resumeWithException(Throwable("Unable to get id token due to unknown error"))
                    return@getIDTokenForcingRefresh
                }

                continuation.resume(token)
            }
        }
    }
}

@Throws(Throwable::class)
actual suspend fun FirebaseAuthenticator.sendPasswordResetEmail(
    email: String,
    actionCodeSettings: ActionCodeSettings?
): Boolean {
    return suspendCancellableCoroutine { continuation ->
        val settings = actionCodeSettings?.toIOS()

        FIRAuth.auth().sendPasswordResetWithEmail(email = email, actionCodeSettings = settings) { error ->
            continuation.resume(error == null)
        }
    }
}

@Throws(Throwable::class)
actual suspend fun FirebaseAuthenticator.sendEmailVerification(): Boolean {
    return suspendCancellableCoroutine { continuation ->
        FIRAuth.auth().currentUser()?.sendEmailVerificationWithCompletion { emailError ->
            continuation.resume(emailError == null)
        }
    }
}

@Throws(Throwable::class)
actual suspend fun FirebaseAuthenticator.signUpWithEmailAndPassword(
    email: String,
    password: String,
    actionCodeSettings: ActionCodeSettings?
): String {
    return suspendCancellableCoroutine { continuation ->
        FIRAuth.auth().createUserWithEmail(email = email, password = password) { result, error ->
            if (error != null) {
                val errorDescription = error.localizedDescription

                val exception = when {
                    error.code == 17007L || errorDescription.contains("email address is already in use", ignoreCase = true) -> {
                        EmailAlreadyExistsThrowable()
                    }
                    error.code == 17026L || errorDescription.contains("weak password", ignoreCase = true)
                            || errorDescription.contains("password should be at least", ignoreCase = true) -> {
                        WeakPasswordThrowable(errorDescription)
                    }
                    error.code == 17008L || errorDescription.contains("badly formatted", ignoreCase = true)
                            || errorDescription.contains("invalid email", ignoreCase = true) -> {
                        InvalidEmailThrowable()
                    }
                    else -> {
                        Throwable(errorDescription)
                    }
                }

                continuation.resumeWithException(exception)
                return@createUserWithEmail
            }

            val user = result?.user()
            if (user == null) {
                continuation.resumeWithException(Throwable("Unable to get id token from a null user"))
                return@createUserWithEmail
            }

            val settings = actionCodeSettings?.toIOS()
            if (settings != null) {
                user.sendEmailVerificationWithActionCodeSettings(settings) { emailError ->
                    if (emailError != null) {
                        continuation.resumeWithException(Throwable(emailError.localizedDescription))
                        return@sendEmailVerificationWithActionCodeSettings
                    }

                    user.getIDTokenForcingRefresh(true) { token, tokenError ->
                        if (tokenError != null) {
                            continuation.resumeWithException(Throwable(tokenError.localizedDescription))
                            return@getIDTokenForcingRefresh
                        }

                        if (token == null) {
                            continuation.resumeWithException(Throwable("Unable to get id token due to unknown error"))
                            return@getIDTokenForcingRefresh
                        }

                        continuation.resume(token)
                    }
                }
            } else {
                user.sendEmailVerificationWithCompletion { emailError ->
                    if (emailError != null) {
                        continuation.resumeWithException(Throwable(emailError.localizedDescription))
                        return@sendEmailVerificationWithCompletion
                    }

                    user.getIDTokenForcingRefresh(true) { token, tokenError ->
                        if (tokenError != null) {
                            continuation.resumeWithException(Throwable(tokenError.localizedDescription))
                            return@getIDTokenForcingRefresh
                        }

                        if (token == null) {
                            continuation.resumeWithException(Throwable("Unable to get id token due to unknown error"))
                            return@getIDTokenForcingRefresh
                        }

                        continuation.resume(token)
                    }
                }
            }
        }
    }
}

@Throws(Throwable::class)
actual suspend fun FirebaseAuthenticator.isCurrentUserEmailVerified(): Boolean {
    val user = FIRAuth.auth().currentUser() ?: return false

    return suspendCancellableCoroutine { continuation ->
        user.reloadWithCompletion { error ->
            if (error != null) {
                continuation.resumeWithException(Throwable(error.localizedDescription))
                return@reloadWithCompletion
            }

            val isVerified = FIRAuth.auth().currentUser()?.emailVerified() ?: false
            continuation.resume(isVerified)
        }
    }
}

@Throws(Throwable::class)
actual suspend fun FirebaseAuthenticator.signInWithEmailLink(email: String, link: String): String {
    return suspendCancellableCoroutine { continuation ->
        FIRAuth.auth().signInWithEmail(email = email, link = link) { result, error ->
            if (error != null) {
                continuation.resumeWithException(Throwable(error.localizedDescription))
                return@signInWithEmail
            }

            val user = result?.user()
            if (user == null) {
                continuation.resumeWithException(Throwable("Unable to get id token from a null user"))
                return@signInWithEmail
            }

            user.getIDTokenForcingRefresh(true) { token, tokenError ->
                if (tokenError != null) {
                    continuation.resumeWithException(Throwable(tokenError.localizedDescription))
                    return@getIDTokenForcingRefresh
                }

                if (token == null) {
                    continuation.resumeWithException(Throwable("Unable to get id token due to unknown error"))
                    return@getIDTokenForcingRefresh
                }

                continuation.resume(token)
            }
        }
    }
}

@Throws(Throwable::class)
actual suspend fun FirebaseAuthenticator.verifyPhoneNumber(
    otp: String,
    verificationId: String
): String {
    return suspendCancellableCoroutine { continuation ->
        val credential = FirebaseAuth.FIRPhoneAuthProvider.provider()
            .credentialWithVerificationID(verificationId, otp)

        FIRAuth.auth().signInWithCredential(credential) { result, error ->
            if (error != null) {
                continuation.resumeWithException(Throwable(error.localizedDescription))
                return@signInWithCredential
            }

            val user = result?.user()
            if (user == null) {
                continuation.resumeWithException(Throwable("Unable to get id token from a null user"))
                return@signInWithCredential
            }

            user.getIDTokenForcingRefresh(true) { token, tokenError ->
                if (tokenError != null) {
                    continuation.resumeWithException(Throwable(tokenError.localizedDescription))
                    return@getIDTokenForcingRefresh
                }

                if (token == null) {
                    continuation.resumeWithException(Throwable("Unable to get id token due to unknown error"))
                    return@getIDTokenForcingRefresh
                }

                continuation.resume(token)
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
        FirebaseAuth.FIRPhoneAuthProvider.provider()
            .verifyPhoneNumber(phoneNumber, null) { verificationID, error ->
                if (error != null) {
                    continuation.resumeWithException(Throwable(error.localizedDescription))
                    return@verifyPhoneNumber
                }

                if (verificationID == null) {
                    continuation.resumeWithException(Throwable("Unable to send sign in OTP due to unknown error"))
                    return@verifyPhoneNumber
                }

                continuation.resume(PhoneVerifierMetadata(verificationID, phoneNumber))
            }
    }
}

@Throws(Throwable::class)
actual suspend fun FirebaseAuthenticator.logout() {
    FIRAuth.auth().signOut(null)
}

private fun ActionCodeSettings.toIOS(): FIRActionCodeSettings {
    return FIRActionCodeSettings().apply {
        setURL(NSURL(string = url))
        setHandleCodeInApp(canHandleCodeInApp)
        setIOSBundleID(iOSBundleId)
        setAndroidPackageName(androidPackageName)
        setAndroidInstallIfNotAvailable(installIfNotAvailable)
    }
}