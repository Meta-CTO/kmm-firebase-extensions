package com.metacto.kmm.firebase.auth.extensions

interface FirebaseAuthPreferences {
    fun getSecureString(key: String): String?
    fun putSecureString(key: String, value: String)
    fun removeSecureString(key: String)
}