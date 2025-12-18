package com.metacto.kmm.auth.common

interface Authenticator {
    @Throws(Throwable::class)
    suspend fun authenticateCurrentUser(): String

    @Throws(Throwable::class)
    suspend fun authenticateWithGoogle(authOptions: AuthOptions): AuthenticationMetadata

    @Throws(Throwable::class)
    suspend fun authenticateWithApple(authOptions: AuthOptions): AuthenticationMetadata

    @Throws(Throwable::class)
    suspend fun sendEmailLink(email: String)

    @Throws(Throwable::class)
    suspend fun resendSignInLink()

    @Throws(Throwable::class)
    suspend fun verifyEmailLink(link: String): String

    @Throws(Throwable::class)
    suspend fun sendPhoneVerification(phoneNumber: String, phoneVerificationProvider: PhoneVerifierProvider?): PhoneVerifierMetadata

    @Throws(Throwable::class)
    suspend fun resendVerificationCode(phoneVerificationProvider: PhoneVerifierProvider?): PhoneVerifierMetadata

    @Throws(Throwable::class)
    suspend fun verifyPhoneOTP(code: String): String

    @Throws(Throwable::class)
    suspend fun signInWithEmailPassword(email: String, password: String): String

    @Throws(Throwable::class)
    suspend fun signUpWithEmailPassword(email: String, password: String, shouldSendEmailVerification: Boolean = true): String

    @Throws(Throwable::class)
    suspend fun sendPasswordResetUsingEmail(email: String): Boolean

    @Throws(Throwable::class)
    suspend fun sendUserEmailVerification(): Boolean

    @Throws(Throwable::class)
    suspend fun isCurrentEmailVerified(): Boolean

    @Throws(Throwable::class)
    suspend fun authenticateWithCustomToken(token: String): String

    @Throws(Throwable::class)
    suspend fun signOut()
}