package com.metacto.kmm.firebase.auth.extensions.mappers

fun String.mapError(
    code: Int,
): Throwable {
    return Throwable("{\"message\": \"$this\",\"code\": $code}")
}