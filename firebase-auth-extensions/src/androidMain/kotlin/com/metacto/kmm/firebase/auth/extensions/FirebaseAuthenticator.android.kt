package com.metacto.kmm.firebase.auth.extensions

import com.google.firebase.Firebase
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
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
    return suspendCancellableCoroutine { continuation ->
        val user = Firebase.auth.currentUser

        if (user == null) {
            continuation.resumeWithException(Throwable("User cannot be null"))
            return@suspendCancellableCoroutine
        }

        user.getIdToken(forceRefresh)
            .addOnSuccessListener { tokenResult ->
                val token = tokenResult.token
                if (token != null) {
                    continuation.resume(token)
                } else {
                    continuation.resumeWithException(Throwable("Token cannot be null"))
                }
            }
            .addOnFailureListener { error ->
                continuation.resumeWithException(error)
            }
    }
}

@Throws(Throwable::class)
actual suspend fun FirebaseAuthenticator.signInWithEmailAndPassword(
    email: String,
    password: String
): String {
    return suspendCancellableCoroutine { continuation ->
        Firebase.auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val user = result.user

                if (user == null) {
                    continuation.resumeWithException(Throwable("User cannot be null"))
                    return@addOnSuccessListener
                }

                user.getIdToken(true)
                    .addOnSuccessListener { tokenResult ->
                        val token = tokenResult.token
                        if (token != null) {
                            continuation.resume(token)
                        } else {
                            continuation.resumeWithException(Throwable("Token cannot be null"))
                        }
                    }
                    .addOnFailureListener { error ->
                        continuation.resumeWithException(error)
                    }
            }
            .addOnFailureListener { exception ->
                continuation.resumeWithException(exception)
            }
    }
}

@Throws(Throwable::class)
actual suspend fun FirebaseAuthenticator.sendPasswordResetEmail(
    email: String,
    actionCodeSettings: ActionCodeSettings?
): Boolean {
    val authActionCodeSettings = actionCodeSettings?.toAndroid()
    return suspendCancellableCoroutine { continuation ->
        Firebase.auth.sendPasswordResetEmail(email, authActionCodeSettings)
            .addOnSuccessListener {
                continuation.resume(true)
            }
            .addOnFailureListener { error ->
                continuation.resumeWithException(error)
            }
    }
}

@Throws(Throwable::class)
actual suspend fun FirebaseAuthenticator.sendEmailVerification(): Boolean {
    val user = Firebase.auth.currentUser ?: throw Throwable("No current user found")
    return suspendCancellableCoroutine { continuation ->
        user.sendEmailVerification()
            .addOnSuccessListener {
                continuation.resume(true)
            }
            .addOnFailureListener { error ->
                continuation.resumeWithException(error)
            }
    }
}

@Throws(Throwable::class)
actual suspend fun FirebaseAuthenticator.signUpWithEmailAndPassword(
    email: String,
    password: String,
    actionCodeSettings: ActionCodeSettings?
): String {
    val authActionCodeSettings = actionCodeSettings?.toAndroid()

    return suspendCancellableCoroutine { continuation ->
        Firebase.auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val user = result.user

                if (user == null) {
                    continuation.resumeWithException(Throwable("User cannot be null"))
                    return@addOnSuccessListener
                }

                val request = if (authActionCodeSettings != null) {
                    user.sendEmailVerification(authActionCodeSettings)
                } else {
                    user.sendEmailVerification()
                }

                request.addOnSuccessListener {
                    user.getIdToken(true)
                        .addOnSuccessListener { tokenResult ->
                            val token = tokenResult.token
                            if (token != null) {
                                continuation.resume(token)
                            } else {
                                continuation.resumeWithException(Throwable("Token cannot be null"))
                            }
                        }
                        .addOnFailureListener { error ->
                            continuation.resumeWithException(error)
                        }
                }.addOnFailureListener {
                    continuation.resumeWithException(Throwable("Failed to send verification email"))
                }
            }
            .addOnFailureListener { exception ->
                val error = when (exception) {
                    is FirebaseAuthUserCollisionException -> EmailAlreadyExistsThrowable()
                    is FirebaseAuthWeakPasswordException -> WeakPasswordThrowable(
                        exception.reason ?: "Password is too weak"
                    )
                    is FirebaseAuthInvalidCredentialsException -> InvalidEmailThrowable()
                    else -> exception
                }
                continuation.resumeWithException(error)
            }
    }
}

@Throws(Throwable::class)
actual suspend fun FirebaseAuthenticator.isCurrentUserEmailVerified(): Boolean {
    val user = Firebase.auth.currentUser ?: return false

    return suspendCancellableCoroutine { continuation ->
        user.reload()
            .addOnSuccessListener {
                val isVerified = Firebase.auth.currentUser?.isEmailVerified ?: false
                continuation.resume(isVerified)
            }
            .addOnFailureListener { exception ->
                continuation.resumeWithException(exception)
            }
    }
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
    return suspendCancellableCoroutine { continuation ->
        Firebase.auth.signInWithEmailLink(email, link)
            .addOnSuccessListener { result ->
                val user = result.user

                if (user == null) {
                    continuation.resumeWithException(Throwable("User cannot be null"))
                    return@addOnSuccessListener
                }

                user.getIdToken(true)
                    .addOnSuccessListener { tokenResult ->
                        val token = tokenResult.token
                        if (token != null) {
                            continuation.resume(token)
                        } else {
                            continuation.resumeWithException(Throwable("Token cannot be null"))
                        }
                    }
                    .addOnFailureListener { error ->
                        continuation.resumeWithException(error)
                    }
            }
            .addOnFailureListener { error ->
                continuation.resumeWithException(error)
            }
    }
}

@Throws(Throwable::class)
actual suspend fun FirebaseAuthenticator.verifyPhoneNumber(
    otp: String,
    verificationId: String
): String {
    return suspendCancellableCoroutine { continuation ->
        val credential = PhoneAuthProvider.getCredential(verificationId, otp)

        Firebase.auth.signInWithCredential(credential)
            .addOnSuccessListener { result ->
                val user = result.user

                if (user == null) {
                    continuation.resumeWithException(Throwable("User cannot be null"))
                    return@addOnSuccessListener
                }

                user.getIdToken(true)
                    .addOnSuccessListener { tokenResult ->
                        val token = tokenResult.token
                        if (token != null) {
                            continuation.resume(token)
                        } else {
                            continuation.resumeWithException(Throwable("Token cannot be null"))
                        }
                    }
                    .addOnFailureListener { error ->
                        continuation.resumeWithException(error)
                    }
            }
            .addOnFailureListener { error ->
                continuation.resumeWithException(error)
            }
    }
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
                override fun onVerificationCompleted(p0: PhoneAuthCredential) {
                    // NOT USED HERE
                }

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

