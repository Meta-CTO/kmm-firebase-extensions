package com.metacto.kmm.firebase.auth.extensions

import kotlinx.coroutines.CancellableContinuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

fun <T> CancellableContinuation<T>.resumeIfActive(value: T) {
    if (isActive) resume(value)
}

fun CancellableContinuation<*>.exceptionIfActive(throwable: Throwable) {
    if (isActive) resumeWithException(throwable)
}
