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
        val actionCodeSettingsIOS = actionCodeSettings.toIOS()

        FIRAuth.auth().sendSignInLinkToEmail(email, actionCodeSettingsIOS) { error ->
            handleError(error, continuation) {
                continuation.resumeIfActive(Unit)
            }
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
            handleError(error, continuation) {
                // Get ID token
                getIDTokenFromUser(result?.user(), continuation)
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
            if (error != null) {
                // Password reset email not sent successfully
                continuation.resumeIfActive(false)
            } else {
                // Password reset email sent successfully
                continuation.resumeIfActive(true)
            }
        }
    }
}

@Throws(Throwable::class)
actual suspend fun FirebaseAuthenticator.sendEmailVerification(): Boolean {
    return suspendCancellableCoroutine { continuation ->
        FIRAuth.auth().currentUser()?.sendEmailVerificationWithCompletion { emailError ->
            if (emailError != null) {
                // Email verification not sent successfully
                continuation.resumeIfActive(false)
            } else {
                // Email verification sent successfully
                continuation.resumeIfActive(true)
            }
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
                // Check error by error code number or description
                val errorDescription = error.localizedDescription
                when {
                    error.code == 17007L || errorDescription.contains(
                        "email address is already in use",
                        ignoreCase = true
                    ) -> {
                        continuation.exceptionIfActive(EmailAlreadyExistsThrowable())
                    }

                    error.code == 17026L || errorDescription.contains(
                        "weak password",
                        ignoreCase = true
                    ) || errorDescription.contains(
                        "password should be at least",
                        ignoreCase = true
                    ) -> {
                        continuation.exceptionIfActive(WeakPasswordThrowable(errorDescription))
                    }

                    error.code == 17008L || errorDescription.contains(
                        "badly formatted",
                        ignoreCase = true
                    ) || errorDescription.contains("invalid email", ignoreCase = true) -> {
                        continuation.exceptionIfActive(InvalidEmailThrowable())
                    }

                    else -> {
                        continuation.exceptionIfActive(Throwable(errorDescription))
                    }
                }
            } else {
                val settings = actionCodeSettings?.toIOS()
                if (settings != null) {
                    // Send verification email with action code settings
                    result?.user()
                        ?.sendEmailVerificationWithActionCodeSettings(settings) { emailError ->
                            handleError(emailError, continuation) {
                                // Get ID token after sending verification email
                                getIDTokenFromUser(result.user(), continuation)
                            }
                        }
                } else {
                    // Send verification email without action code settings
                    result?.user()?.sendEmailVerificationWithCompletion() { emailError ->
                        handleError(emailError, continuation) {
                            // Get ID token after sending verification email
                            getIDTokenFromUser(result.user(), continuation)
                        }
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
            handleError(error, continuation) {
                val isVerified = FIRAuth.auth().currentUser()?.emailVerified() ?: false
                continuation.resumeIfActive(isVerified)
            }
        }
    }
}

@Throws(Throwable::class)
actual suspend fun FirebaseAuthenticator.signInWithEmailLink(email: String, link: String): String {
    return suspendCancellableCoroutine { continuation ->
        FIRAuth.auth().signInWithEmail(
            email = email,
            link = link
        ) { result: FIRAuthDataResult?, error: NSError? ->
            handleError(error, continuation) {
                getIDTokenFromUser(result?.user(), continuation)
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
        FIRAuth.auth()
            .signInWithCredential(credential) { result: FIRAuthDataResult?, error: NSError? ->
                handleError(error, continuation) {
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
        FirebaseAuth.FIRPhoneAuthProvider.provider()
            .verifyPhoneNumber(phoneNumber, null) { verificationID, error ->
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
        handleTokenResult(token, error, continuation)
    }
}

private fun handleTokenResult(
    token: String?,
    error: NSError?,
    continuation: CancellableContinuation<String>
) {
    when {
        error != null -> continuation.exceptionIfActive(Throwable(error.localizedDescription))
        token != null -> continuation.resumeIfActive(token)
        else -> continuation.exceptionIfActive(Throwable("Unable to get id token due to unknown error"))
    }
}

private fun handleError(
    error: NSError?,
    continuation: CancellableContinuation<*>,
    onSuccess: () -> Unit
) {
    if (error != null) {
        continuation.exceptionIfActive(Throwable(error.localizedDescription))
    } else {
        onSuccess()
    }
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