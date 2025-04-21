package com.metacto.kmm.firebase.auth.extensions

import com.metacto.kmm.auth.common.AuthenticationMetadata

interface AuthProvider {
    @Throws(Throwable::class)
    suspend fun signInWithGoogle(): AuthenticationMetadata?

    @Throws(Throwable::class)
    suspend fun signInWithApple(): AuthenticationMetadata?
}