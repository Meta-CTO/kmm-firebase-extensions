package com.metacto.kmm.firebase.auth.extensions

import com.google.firebase.Firebase
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.auth
import com.metacto.kmm.auth.common.PhoneVerifierMetadata
import com.metacto.kmm.auth.common.PhoneVerifierProvider
import com.metacto.kmm.firebase.auth.ActionCodeSettings
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

@Throws(Throwable::class)
actual suspend fun FirebaseAuthenticator.getIdToken(forceRefresh: Boolean): String {
    return currentUserOrThrow().idToken(forceRefresh)
}

@Throws(Throwable::class)
actual suspend fun FirebaseAuthenticator.signInWithEmailAndPassword(
    email: String,
    password: String
): String {
    val user = try {
        val result = Firebase.auth.signInWithEmailAndPassword(email, password).await()
        result.user ?: throw Throwable("User cannot be null")
    } catch (exception: Throwable) {
        val error = when (exception) {
            is FirebaseAuthInvalidCredentialsException -> {
                // This can be either invalid email format or wrong password
                if (exception.message?.contains("password is invalid", ignoreCase = true) == true) {
                    WrongPasswordThrowable()
                } else {
                    InvalidCredentialsThrowable()
                }
            }
            is FirebaseAuthInvalidUserException -> {
                UserNotFoundThrowable()
            }
            else -> exception
        }
        throw error
    }
    return user.idToken(true)
}

@Throws(Throwable::class)
actual suspend fun FirebaseAuthenticator.sendPasswordResetEmail(
    email: String,
    actionCodeSettings: ActionCodeSettings?
): Boolean {
    val authActionCodeSettings = actionCodeSettings?.toAndroid()
    return try {
        Firebase.auth.sendPasswordResetEmail(email, authActionCodeSettings).await()
        true
    } catch (e: Throwable) {
        false
    }
}

@Throws(Throwable::class)
actual suspend fun FirebaseAuthenticator.sendEmailVerification(): Boolean {
    val user = Firebase.auth.currentUser ?: throw Throwable("No current user found")
    return try {
        user.sendEmailVerification().await()
        true
    } catch (e: Throwable) {
        false
    }
}

@Throws(Throwable::class)
actual suspend fun FirebaseAuthenticator.signUpWithEmailAndPassword(
    email: String,
    password: String,
    actionCodeSettings: ActionCodeSettings?,
    shouldSendEmailVerification: Boolean
): String {

    val user = try {
        val result = Firebase.auth.createUserWithEmailAndPassword(email, password).await()
        result.user ?: throw Throwable("User cannot be null")
    } catch (exception: Throwable) {
        val error = when (exception) {
            is FirebaseAuthUserCollisionException -> EmailAlreadyExistsThrowable()
            is FirebaseAuthWeakPasswordException -> WeakPasswordThrowable(
                exception.reason ?: "Password is too weak"
            )
            is FirebaseAuthInvalidCredentialsException -> InvalidEmailThrowable()
            else -> exception
        }
        throw error
    }

    if(shouldSendEmailVerification) {
        val authActionCodeSettings = actionCodeSettings?.toAndroid()

        val request = if (authActionCodeSettings != null) {
            user.sendEmailVerification(authActionCodeSettings)
        } else {
            user.sendEmailVerification()
        }

        request.await()
    }

    return user.idToken(true)
}

@Throws(Throwable::class)
actual suspend fun FirebaseAuthenticator.isCurrentUserEmailVerified(): Boolean {
    val user = Firebase.auth.currentUser ?: return false
    user.reload().await()
    return Firebase.auth.currentUser?.isEmailVerified ?: false
}

@Throws(Throwable::class)
actual suspend fun FirebaseAuthenticator.sendSignInLinkToEmail(
    email: String,
    actionCodeSettings: ActionCodeSettings
) {
    val authActionCodeSettings = actionCodeSettings.toAndroid()
    Firebase.auth.sendSignInLinkToEmail(email, authActionCodeSettings).result
}

@Throws(Throwable::class)
actual suspend fun FirebaseAuthenticator.signInWithEmailLink(
    email: String,
    link: String
): String {
    val result = Firebase.auth.signInWithEmailLink(email, link).await()
    val user = result.user ?: throw Throwable("User cannot be null")
    return user.idToken(true)
}

@Throws(Throwable::class)
actual suspend fun FirebaseAuthenticator.verifyPhoneNumber(
    otp: String,
    verificationId: String
): String {
    val credential = PhoneAuthProvider.getCredential(verificationId, otp)
    val result = Firebase.auth.signInWithCredential(credential).await()
    val user = result.user ?: throw Throwable("User cannot be null")
    return user.idToken(true)
}

@Throws(Throwable::class)
actual suspend fun FirebaseAuthenticator.signInWithCustomToken(
    token: String,
): String {
    val result = Firebase.auth.signInWithCustomToken(token).await()
    val user = result.user ?: throw Throwable("User cannot be null")
    return user.idToken(true)
}

@Throws(Throwable::class)
actual suspend fun FirebaseAuthenticator.sendSignInOTPToPhone(
    phoneNumber: String,
    phoneVerifierProvider: PhoneVerifierProvider?
): PhoneVerifierMetadata {
    if (phoneVerifierProvider == null) {
        throw Throwable("Phone verifier provider cannot be null")
    }

    return suspendCancellableCoroutine { continuation ->
        val options = PhoneAuthOptions.newBuilder(Firebase.auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(phoneVerifierProvider.timeout, phoneVerifierProvider.unit)
            .setActivity(phoneVerifierProvider.activity)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(p0: PhoneAuthCredential) { }

                override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken,
                ) {
                    continuation.resume(
                        PhoneVerifierMetadata(
                            verificationId = verificationId,
                            phoneNumber
                        )
                    )
                }

                override fun onVerificationFailed(p0: FirebaseException) {
                    continuation.resumeWithException(Throwable(p0))
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

private fun ActionCodeSettings.toAndroid(): com.google.firebase.auth.ActionCodeSettings {
    return com.google.firebase.auth.ActionCodeSettings.newBuilder()
        .setUrl(url)
        .setHandleCodeInApp(canHandleCodeInApp)
        .setIOSBundleId(iOSBundleId)
        .setAndroidPackageName(
            androidPackageName,
            installIfNotAvailable,
            null
        )
        .build()
}

