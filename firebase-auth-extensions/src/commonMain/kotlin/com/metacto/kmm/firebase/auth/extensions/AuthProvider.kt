package com.metacto.kmm.firebase.auth.extensions

import com.metacto.kmm.auth.common.ProfileMetadata
import dev.gitlive.firebase.auth.AuthCredential

data class AuthenticationResult(
    val authCredential: AuthCredential,
    val profileMetadata: ProfileMetadata
)

interface AuthProvider {
    @Throws(Throwable::class)
    suspend fun signInWithGoogle(): AuthenticationResult?

    @Throws(Throwable::class)
    suspend fun signInWithApple(): AuthenticationResult
}