@file:OptIn(ExperimentalForeignApi::class)

package com.metacto.kmm.firebase.auth.extensions

import FirebaseAuth.FIRAuth
import FirebaseAuth.FIRGoogleAuthProvider
import com.metacto.kmm.auth.common.AuthOptions
import kotlinx.cinterop.ExperimentalForeignApi
import FirebaseAuth.FIROAuthProvider
import com.metacto.kmm.auth.common.AuthenticationMetadata
import kotlinx.coroutines.suspendCancellableCoroutine

@OptIn(ExperimentalForeignApi::class)
actual class AuthClient : AuthProvider {
    private lateinit var options: AuthOptions

    actual fun init() {}

    override suspend fun signInWithGoogle(): AuthenticationMetadata {
        val googleProvider = SignInWithGoogleProvider(
            presentingViewController = options.presentingViewController
        )

        val result = googleProvider.start()
        val credential = FIRGoogleAuthProvider.credentialWithIDToken(
            idToken = result.idToken,
            accessToken = result.accessToken.orEmpty()
        )

        return suspendCancellableCoroutine { cont ->
            FIRAuth.auth().signInWithCredential(credential) { data, nsError ->
                if (nsError != null) {
                    cont.exceptionIfActive(Throwable(nsError.localizedDescription))
                } else {
                    data?.user()?.getIDTokenForcingRefresh(true) { token, nsError ->
                        if (nsError != null) {
                            cont.exceptionIfActive(Throwable(nsError.localizedDescription))
                        } else if (token != null) {
                            cont.resumeIfActive(
                                AuthenticationMetadata(
                                    token,
                                    result.profileMetadata
                                )
                            )
                        } else {
                            cont.exceptionIfActive(Throwable("Token cannot be null"))
                        }
                    }
                }
            }
        }
    }

    override suspend fun signInWithApple(): AuthenticationMetadata {
        val appleProvider = SignInWithAppleProvider(
            presentationAnchor = options.presentationAnchor
        )
        val result = appleProvider.start()
        val credential = FIROAuthProvider.credentialWithProviderID(
            providerID = "apple.com",
            IDToken = result.idToken,
            rawNonce = "",
            accessToken = result.accessToken.orEmpty()
        )

        return suspendCancellableCoroutine { cont ->
            FIRAuth.auth().signInWithCredential(credential) { data, nsError ->
                if (nsError != null) {
                    cont.exceptionIfActive(Throwable(nsError.localizedDescription))
                } else {
                    data?.user()?.getIDTokenForcingRefresh(true) { token, nsError ->
                        if (nsError != null) {
                            cont.exceptionIfActive(Throwable(nsError.localizedDescription))
                        } else if (token != null) {
                            cont.resumeIfActive(
                                AuthenticationMetadata(
                                    token,
                                    result.profileMetadata
                                )
                            )
                        } else {
                            cont.exceptionIfActive(Throwable("Token cannot be null"))
                        }
                    }
                }
            }
        }
    }

    actual fun setAuthOptions(options: AuthOptions) {
        this.options = options
    }
}

