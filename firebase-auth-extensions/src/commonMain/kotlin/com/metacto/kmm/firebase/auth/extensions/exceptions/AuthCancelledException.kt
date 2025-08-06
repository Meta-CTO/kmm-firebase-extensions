package com.metacto.kmm.firebase.auth.extensions.exceptions

class AuthCancelledThrowable(
    message: String = "User cancelled the authentication"
) : Throwable(message)