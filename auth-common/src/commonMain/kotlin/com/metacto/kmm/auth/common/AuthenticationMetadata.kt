package com.metacto.kmm.auth.common

data class AuthenticationMetadata(
    val idToken: String,
    val profileMetadata: ProfileMetadata,
    val accessToken: String? = null,
)