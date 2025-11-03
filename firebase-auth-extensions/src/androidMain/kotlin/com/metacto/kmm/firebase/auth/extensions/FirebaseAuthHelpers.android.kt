package com.metacto.kmm.firebase.auth.extensions

import com.google.android.gms.tasks.Task
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Extension function to get ID token from FirebaseUser
 */
internal suspend fun FirebaseUser.idToken(forceRefresh: Boolean = true): String {
    return suspendCancellableCoroutine { continuation ->
        getIdToken(forceRefresh)
            .addOnSuccessListener { tokenResult ->
                val token = tokenResult.token
                if (token != null) {
                    continuation.resume(token)
                } else {
                    continuation.resumeWithException(Throwable("Token cannot be null"))
                }
            }
            .addOnFailureListener { error ->
                continuation.resumeWithException(error)
            }
    }
}

/**
 * Extension function to get current user or throw exception
 */
internal fun currentUserOrThrow(): FirebaseUser {
    return Firebase.auth.currentUser ?: throw Throwable("User cannot be null")
}

/**
 * Extension function to await a Task<T>
 */
internal suspend fun <T> Task<T>.await(): T {
    return suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { result ->
            continuation.resume(result)
        }
        addOnFailureListener { error ->
            continuation.resumeWithException(error)
        }
    }
}
