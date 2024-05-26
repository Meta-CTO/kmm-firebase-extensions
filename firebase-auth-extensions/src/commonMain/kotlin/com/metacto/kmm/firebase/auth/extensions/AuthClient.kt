package com.metacto.kmm.firebase.auth.extensions

import com.metacto.kmm.auth.common.AuthOptions

expect class AuthClient() : AuthProvider {
    fun init()
    fun setAuthOptions(options: AuthOptions)
}
