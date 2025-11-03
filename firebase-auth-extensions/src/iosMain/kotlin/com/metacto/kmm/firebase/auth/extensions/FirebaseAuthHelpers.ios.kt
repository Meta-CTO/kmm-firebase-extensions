@file:OptIn(ExperimentalForeignApi::class)

package com.metacto.kmm.firebase.auth.extensions

import FirebaseAuth.FIRAuth
import FirebaseAuth.FIRUser
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSError
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Extension function to get ID token from FIRUser
 */
internal suspend fun FIRUser.idToken(forceRefresh: Boolean = true): String {
    return suspendCancellableCoroutine { continuation ->
        getIDTokenForcingRefresh(forceRefresh) { token, error ->
            when {
                error != null -> continuation.resumeWithException(Throwable(error.localizedDescription))
                token == null -> continuation.resumeWithException(Throwable("Unable to get id token due to unknown error"))
                else -> continuation.resume(token)
            }
        }
    }
}

/**
 * Extension function to get current user or throw exception
 */
internal fun FIRAuth.Companion.currentUserOrThrow(): FIRUser {
    return auth().currentUser() ?: throw Throwable("Unable to get id token from a null user")
}

/**
 * Helper function to handle iOS Firebase callbacks that only have an error parameter
 */
internal suspend fun <T> awaitCallback(
    errorMessage: String? = null,
    onSuccess: T,
    block: (callback: (NSError?) -> Unit) -> Unit
): T {
    return suspendCancellableCoroutine { continuation ->
        block { error ->
            if (error != null) {
                val message = errorMessage ?: error.localizedDescription
                continuation.resumeWithException(Throwable(message))
            } else {
                continuation.resume(onSuccess)
            }
        }
    }
}
