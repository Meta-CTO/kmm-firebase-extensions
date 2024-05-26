package com.metacto.kmm.auth.common

data class PhoneVerifierMetadata(
    val verificationId: String,
    val phoneNumber: String
)