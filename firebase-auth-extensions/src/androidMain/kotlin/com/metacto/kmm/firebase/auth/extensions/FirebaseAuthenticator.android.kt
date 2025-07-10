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
import kotlinx.coroutines.suspendCancellableCoroutine

@Throws(Throwable::class)
actual suspend fun FirebaseAuthenticator.getIdToken(forceRefresh: Boolean): String {
    return suspendCancellableCoroutine { cont ->
        getIdTokenFromUser(Firebase.auth.currentUser, cont)
    }
}

@Throws(Throwable::class)
actual suspend fun FirebaseAuthenticator.signInWithEmailAndPassword(
    email: String,
    password: String
): String {
    return suspendCancellableCoroutine { cont ->
        Firebase.auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                getIdTokenFromUser(Firebase.auth.currentUser, cont)
            }
            .addOnFailureListener { exception ->
                cont.exceptionIfActive(exception)
            }
    }
}

@Throws(Throwable::class)
actual suspend fun FirebaseAuthenticator.sendPasswordResetEmail(email: String): Boolean {
    return suspendCancellableCoroutine { continuation ->
        Firebase.auth.sendPasswordResetEmail(email)
            .addOnSuccessListener {
                continuation.resumeIfActive(true)
            }
            .addOnFailureListener { error ->
                continuation.exceptionIfActive(error)
            }
    }
}

@Throws(Throwable::class)
actual suspend fun FirebaseAuthenticator.sendEmailVerification(): Boolean {
    val user = Firebase.auth.currentUser ?: throw Throwable("No current user found")
    return suspendCancellableCoroutine { continuation ->
        user.sendEmailVerification()
            .addOnSuccessListener {
                continuation.resumeIfActive(true)
            }
            .addOnFailureListener { error ->
                continuation.exceptionIfActive(error)
            }
    }
}

@Throws(Throwable::class)
actual suspend fun FirebaseAuthenticator.signUpWithEmailAndPassword(
    email: String,
    password: String
): String {
    return suspendCancellableCoroutine { cont ->
        Firebase.auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                result.user?.sendEmailVerification()?.addOnSuccessListener {
                    getIdTokenFromUser(Firebase.auth.currentUser, cont)
                }?.addOnFailureListener {
                    cont.exceptionIfActive(Throwable("Failed to send verification email"))
                }
            }
            .addOnFailureListener { exception ->
                when (exception) {
                    is FirebaseAuthUserCollisionException -> {
                        cont.exceptionIfActive(EmailAlreadyExistsThrowable())
                    }

                    is FirebaseAuthWeakPasswordException -> {
                        cont.exceptionIfActive(
                            WeakPasswordThrowable(
                                exception.reason ?: "Password is too weak"
                            )
                        )
                    }

                    is FirebaseAuthInvalidCredentialsException -> {
                        cont.exceptionIfActive(InvalidEmailThrowable())
                    }

                    else -> {
                        cont.exceptionIfActive(exception)
                    }
                }
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
                continuation.resumeIfActive(isVerified)
            }
            .addOnFailureListener { exception ->
                continuation.exceptionIfActive(exception)
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
    return suspendCancellableCoroutine { cont ->
        Firebase.auth.signInWithEmailLink(
            email,
            link
        ).addOnSuccessListener {
            getIdTokenFromUser(it.user, cont)
        }.addOnFailureListener { error ->
            cont.exceptionIfActive(error)
        }
    }
}

@Throws(Throwable::class)
actual suspend fun FirebaseAuthenticator.verifyPhoneNumber(
    otp: String,
    verificationId: String
): String {
    return suspendCancellableCoroutine { cont ->
        val credential = PhoneAuthProvider.getCredential(verificationId, otp)
        Firebase.auth.signInWithCredential(credential).addOnSuccessListener { result ->
            getIdTokenFromUser(result.user, cont)
        }.addOnFailureListener { error ->
            cont.exceptionIfActive(error)

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

private fun getIdTokenFromUser(
    user: com.google.firebase.auth.FirebaseUser?,
    continuation: kotlinx.coroutines.CancellableContinuation<String>
) {
    if (user == null) {
        continuation.exceptionIfActive(Throwable("User cannot be null"))
        return
    }
    
    user.getIdToken(true)
        .addOnSuccessListener { tokenResult ->
            val token = tokenResult.token
            if (token != null) {
                continuation.resumeIfActive(token)
            } else {
                continuation.exceptionIfActive(Throwable("Token cannot be null"))
            }
        }
        .addOnFailureListener { error ->
            continuation.exceptionIfActive(error)
        }
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

