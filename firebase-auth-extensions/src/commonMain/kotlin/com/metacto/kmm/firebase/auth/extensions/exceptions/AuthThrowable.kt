package com.metacto.kmm.firebase.auth.extensions.exceptions

class AuthThrowable(
    override val message: String,
    val code: Int
) : Throwable(message)