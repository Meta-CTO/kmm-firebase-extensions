package com.metacto.kmm.auth.common

actual interface PhoneVerifierProvider{
    val verifier: ApplicationVerifier
    suspend fun getVerificationCode(verificationId: String): String
}