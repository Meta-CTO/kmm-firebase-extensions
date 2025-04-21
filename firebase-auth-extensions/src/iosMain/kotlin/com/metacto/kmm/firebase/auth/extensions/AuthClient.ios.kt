@file:OptIn(ExperimentalForeignApi::class)

package com.metacto.kmm.firebase.auth.extensions

//import cocoapods.FirebaseAuth.FIROAuthProvider
import FirebaseAuth.FIRGoogleAuthProvider
import com.metacto.kmm.auth.common.AuthOptions
import com.metacto.kmm.auth.common.ProfileMetadata
import dev.gitlive.firebase.auth.AuthCredential
import kotlinx.cinterop.ExperimentalForeignApi
import FirebaseAuth.FIROAuthProvider

actual class AuthClient : AuthProvider {
    private lateinit var options: AuthOptions

    actual fun init() {}

    override suspend fun signInWithGoogle(): AuthenticationResult {
        val googleProvider = SignInWithGoogleProvider(
            presentingViewController = options.presentingViewController
        )

        val result = googleProvider.start()
        val credential = FIRGoogleAuthProvider.credentialWithIDToken(
            idToken = result.idToken,
            accessToken = result.accessToken!!
        )

        return AuthenticationResult(
            AuthCredential(credential),
            result.profileMetadata
        )
    }

    override suspend fun signInWithApple(): AuthenticationResult {
        val appleProvider = SignInWithAppleProvider(
            presentationAnchor = options.presentationAnchor
        )
        val result = appleProvider.start()
        val credential = FIROAuthProvider.credentialWithProviderID(
            providerID = "apple.com",
            IDToken = result.idToken,
            rawNonce = "",
            accessToken = null
        )

        return AuthenticationResult(
            AuthCredential(credential),
            result.profileMetadata
        )
    }

    actual fun setAuthOptions(options: AuthOptions) {
        this.options = options
    }
}

