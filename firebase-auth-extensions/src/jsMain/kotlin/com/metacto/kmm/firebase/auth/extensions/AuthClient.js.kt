package com.metacto.kmm.firebase.auth.extensions

import com.metacto.kmm.auth.common.AuthOptions
import com.metacto.kmm.auth.common.ProfileMetadata
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.AuthCredential
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.auth.externals.GoogleAuthProvider
import dev.gitlive.firebase.auth.externals.OAuthProvider

actual class AuthClient : AuthProvider {

    actual fun init() {}

    override suspend fun signInWithApple(): AuthenticationResult {
        val result = Firebase.auth.signInWithPopup(
            OAuthProvider("apple.com")
        )
        val profile = ProfileMetadata(
            firstName = result.user?.displayName,
            lastName = null,
            email = result.user?.email,
            phoneNumber = result.user?.phoneNumber,
            pictureUrl = result.user?.photoURL
        )

        return AuthenticationResult(
            AuthCredential(result.js.credential!!),
            profile
        )
    }

    override suspend fun signInWithGoogle(): AuthenticationResult {
        val result = Firebase.auth.signInWithPopup(
            GoogleAuthProvider()
        )
        val profile = ProfileMetadata(
            firstName = result.user?.displayName,
            lastName = null,
            email = result.user?.email,
            phoneNumber = result.user?.phoneNumber,
            pictureUrl = result.user?.photoURL
        )
        return AuthenticationResult(AuthCredential(result.js.credential!!), profile)
    }

    actual fun setAuthOptions(options: AuthOptions) {
    }
}