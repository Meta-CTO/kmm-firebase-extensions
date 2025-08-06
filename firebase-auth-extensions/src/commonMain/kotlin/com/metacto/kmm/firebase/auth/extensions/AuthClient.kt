package com.metacto.kmm.firebase.auth.extensions

import com.metacto.kmm.auth.common.AuthOptions
import com.metacto.kmm.auth.common.AuthenticationMetadata

expect class AuthClient() : AuthProvider {
    fun init()
    fun setAuthOptions(options: AuthOptions)
    override suspend fun signInWithApple(): AuthenticationMetadata
    override suspend fun signInWithGoogle(): AuthenticationMetadata
}
