package com.metacto.kmm.firebase.auth.extensions.mappers

import com.metacto.kmm.firebase.auth.extensions.exceptions.AuthCancelledThrowable

fun String.mapError(
    code: Int,
): Throwable {
    return when (code) {
        -5, // Google Sign-In user cancelled
        1001 -> AuthCancelledThrowable() // Apple Sign-In ASAuthorizationErrorCanceled
        else -> Throwable("{\"message\": \"$this\",\"code\": $code}")
    }
}