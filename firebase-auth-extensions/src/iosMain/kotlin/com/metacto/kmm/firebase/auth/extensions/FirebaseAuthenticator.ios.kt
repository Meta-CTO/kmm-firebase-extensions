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
    return FIRAuth.currentUserOrThrow().idToken(forceRefresh)
}

@Throws(Throwable::class)
actual suspend fun FirebaseAuthenticator.sendSignInLinkToEmail(
    email: String,
    actionCodeSettings: ActionCodeSettings
) {
    val actionCodeSettingsIOS = actionCodeSettings.toIOS()
    awaitCallback(onSuccess = Unit) { callback ->
        FIRAuth.auth().sendSignInLinkToEmail(email, actionCodeSettingsIOS, callback)
    }
}

@Throws(Throwable::class)
actual suspend fun FirebaseAuthenticator.signInWithEmailAndPassword(
    email: String,
    password: String
): String {
    val user = suspendCancellableCoroutine { continuation ->
        FIRAuth.auth().signInWithEmail(email = email, password = password) { result, error ->
            if (error != null) {
                val errorDescription = error.localizedDescription

                val exception = when {
                    error.code == 17009L || errorDescription.contains("wrong password", ignoreCase = true)
                            || errorDescription.contains("password is invalid", ignoreCase = true) -> {
                        WrongPasswordThrowable()
                    }
                    error.code == 17011L || errorDescription.contains("no user record", ignoreCase = true)
                            || errorDescription.contains("user not found", ignoreCase = true) -> {
                        UserNotFoundThrowable()
                    }
                    error.code == 17008L || errorDescription.contains("badly formatted", ignoreCase = true)
                            || errorDescription.contains("invalid email", ignoreCase = true) -> {
                        InvalidEmailThrowable()
                    }
                    else -> {
                        InvalidCredentialsThrowable(errorDescription)
                    }
                }

                continuation.resumeWithException(exception)
                return@signInWithEmail
            }

            val user = result?.user()
            if (user == null) {
                continuation.resumeWithException(Throwable("Unable to get id token from a null user"))
                return@signInWithEmail
            }

            continuation.resume(user)
        }
    }

    return user.idToken(true)
}

@Throws(Throwable::class)
actual suspend fun FirebaseAuthenticator.sendPasswordResetEmail(
    email: String,
    actionCodeSettings: ActionCodeSettings?
): Boolean {
    val settings = actionCodeSettings?.toIOS()
    return try {
        awaitCallback(onSuccess = true) { callback ->
            FIRAuth.auth().sendPasswordResetWithEmail(email = email, actionCodeSettings = settings, callback)
        }
    } catch (e: Throwable) {
        false
    }
}

@Throws(Throwable::class)
actual suspend fun FirebaseAuthenticator.sendEmailVerification(): Boolean {
    return try {
        awaitCallback(onSuccess = true) { callback ->
            FIRAuth.auth().currentUser()?.sendEmailVerificationWithCompletion(callback)
        }
    } catch (e: Throwable) {
        false
    }
}

@Throws(Throwable::class)
actual suspend fun FirebaseAuthenticator.signUpWithEmailAndPassword(
    email: String,
    password: String,
    actionCodeSettings: ActionCodeSettings?
): String {
    val user = suspendCancellableCoroutine { continuation ->
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

            continuation.resume(user)
        }
    }

    val settings = actionCodeSettings?.toIOS()
    if (settings != null) {
        awaitCallback(onSuccess = Unit) { callback ->
            user.sendEmailVerificationWithActionCodeSettings(settings, callback)
        }
    } else {
        awaitCallback(onSuccess = Unit) { callback ->
            user.sendEmailVerificationWithCompletion(callback)
        }
    }

    return user.idToken(true)
}

@Throws(Throwable::class)
actual suspend fun FirebaseAuthenticator.isCurrentUserEmailVerified(): Boolean {
    val user = FIRAuth.auth().currentUser() ?: return false

    awaitCallback(onSuccess = Unit) { callback ->
        user.reloadWithCompletion(callback)
    }

    return FIRAuth.auth().currentUser()?.emailVerified() ?: false
}

@Throws(Throwable::class)
actual suspend fun FirebaseAuthenticator.signInWithEmailLink(email: String, link: String): String {
    val user = suspendCancellableCoroutine { continuation ->
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

            continuation.resume(user)
        }
    }

    return user.idToken(true)
}

@Throws(Throwable::class)
actual suspend fun FirebaseAuthenticator.verifyPhoneNumber(
    otp: String,
    verificationId: String
): String {
    val credential = FirebaseAuth.FIRPhoneAuthProvider.provider()
        .credentialWithVerificationID(verificationId, otp)

    val user = suspendCancellableCoroutine { continuation ->
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

            continuation.resume(user)
        }
    }

    return user.idToken(true)
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