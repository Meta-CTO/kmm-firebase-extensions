@file:OptIn(ExperimentalForeignApi::class)

package com.metacto.kmm.firebase.auth.extensions

import FirebaseAuth.FIRAuth
import FirebaseAuth.FIRGoogleAuthProvider
import FirebaseAuth.FIROAuthProvider
import com.metacto.kmm.auth.common.AuthOptions
import com.metacto.kmm.auth.common.AuthenticationMetadata
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@OptIn(ExperimentalForeignApi::class)
actual class AuthClient : AuthProvider {
    private lateinit var options: AuthOptions

    actual fun init() {}

    actual override suspend fun signInWithGoogle(): AuthenticationMetadata {
        val googleProvider = SignInWithGoogleProvider(
            presentingViewController = options.presentingViewController
        )

        val result = googleProvider.start()
        val credential = FIRGoogleAuthProvider.credentialWithIDToken(
            idToken = result.idToken,
            accessToken = result.accessToken.orEmpty()
        )

        return suspendCancellableCoroutine { continuation ->
            FIRAuth.auth().signInWithCredential(credential) { data, nsError ->
                if (nsError != null) {
                    continuation.resumeWithException(Throwable(nsError.localizedDescription))
                    return@signInWithCredential
                }

                data?.user()?.getIDTokenForcingRefresh(true) { token, tokenError ->
                    if (tokenError != null) {
                        continuation.resumeWithException(Throwable(tokenError.localizedDescription))
                        return@getIDTokenForcingRefresh
                    }

                    if (token == null) {
                        continuation.resumeWithException(Throwable("Token cannot be null"))
                        return@getIDTokenForcingRefresh
                    }

                    // Single resume point for success
                    continuation.resume(
                        AuthenticationMetadata(
                            token,
                            result.profileMetadata
                        )
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    actual override suspend fun signInWithApple(): AuthenticationMetadata {
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

        return suspendCancellableCoroutine { continuation ->
            FIRAuth.auth().signInWithCredential(credential) { data, nsError ->
                if (nsError != null) {
                    continuation.resumeWithException(Throwable(nsError.localizedDescription))
                    return@signInWithCredential
                }

                if (data?.user() == null) {
                    continuation.resumeWithException(Throwable("User cannot be null"))
                    return@signInWithCredential
                }

                data.user().getIDTokenForcingRefresh(true) { token, tokenError ->
                    if (tokenError != null) {
                        continuation.resumeWithException(Throwable(tokenError.localizedDescription))
                        return@getIDTokenForcingRefresh
                    }

                    if (token == null) {
                        continuation.resumeWithException(Throwable("Token cannot be null"))
                        return@getIDTokenForcingRefresh
                    }

                    continuation.resume(
                        AuthenticationMetadata(
                            token,
                            result.profileMetadata
                        )
                    )
                }
            }
        }
    }

    actual fun setAuthOptions(options: AuthOptions) {
        this.options = options
    }
}

