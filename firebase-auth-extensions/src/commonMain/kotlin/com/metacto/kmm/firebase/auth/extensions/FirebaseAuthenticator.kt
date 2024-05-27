package com.metacto.kmm.firebase.auth.extensions

import com.metacto.kmm.auth.common.AuthOptions
import com.metacto.kmm.auth.common.AuthenticationMetadata
import com.metacto.kmm.auth.common.Authenticator
import com.metacto.kmm.auth.common.PhoneVerifierMetadata
import com.metacto.kmm.auth.common.PhoneVerifierProvider
import com.metacto.kmm.firebase.auth.extensions.constants.Constants
import com.metacto.kmm.firebase.auth.extensions.mappers.toPhoneVerificationProvider
import com.metacto.kmm.firebase.auth.extensions.mappers.toPhoneVerifierMetadata
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.ActionCodeSettings
import dev.gitlive.firebase.auth.AuthCredential
import dev.gitlive.firebase.auth.PhoneAuthProvider
import dev.gitlive.firebase.auth.auth

class FirebaseAuthenticator(
    private val actionCodeSettings: ActionCodeSettings,
    private val firebaseAuthPreferences: FirebaseAuthPreferences
) : Authenticator {
    private val authClient by lazy { AuthClient() }

    @Throws(Throwable::class)
    override suspend fun authenticateCurrentUser(): String {
        val user = Firebase.auth.currentUser
            ?: throw Throwable("Unable to authenticate current user, current user is null")
        return user.getIdToken(true) ?: throw Throwable("Unable to get idToken")
    }

    @Throws(Throwable::class)
    override suspend fun authenticateWithGoogle(authOptions: AuthOptions): AuthenticationMetadata? {
        authClient.setAuthOptions(authOptions)
        authClient.init()
        val result = authClient.signInWithGoogle() ?: return null
        val idToken = authenticateWithCredentials(result.authCredential)
        return AuthenticationMetadata(idToken, result.profileMetadata)
    }

    @Throws(Throwable::class)
    override suspend fun authenticateWithApple(): AuthenticationMetadata {
        val result = authClient.signInWithApple()
        val idToken = authenticateWithCredentials(result.authCredential)
        return AuthenticationMetadata(idToken, result.profileMetadata)
    }

    @Throws(Throwable::class)
    override suspend fun sendEmailLink(email: String) {
        firebaseAuthPreferences.putSecureString(Constants.SIGN_IN_EMAIL_LINK_EMAIL, email)
        Firebase.auth.sendSignInLinkToEmail(email, actionCodeSettings)
    }

    @Throws(Throwable::class)
    override suspend fun resendSignInLink() {
        val email = firebaseAuthPreferences.getSecureString(Constants.SIGN_IN_EMAIL_LINK_EMAIL)
        if (email.isNullOrEmpty()) throw Throwable("Email is null or empty, please try again.")
        sendEmailLink(email)
    }

    @Throws(Throwable::class)
    override suspend fun verifyEmailLink(link: String): String {
        val email = firebaseAuthPreferences.getSecureString(Constants.SIGN_IN_EMAIL_LINK_EMAIL)
        if (email.isNullOrEmpty()) throw Throwable("Email is null or empty, please try again.")
        val user = Firebase.auth.signInWithEmailLink(email, link).user
            ?: throw Throwable("Signing in failed user is null")
        return user.getIdToken(true) ?: throw Throwable("Unable to getIdToken")
    }

    @Throws(Throwable::class)
    override suspend fun sendPhoneVerification(
        phoneNumber: String,
        phoneVerificationProvider: PhoneVerifierProvider
    ): PhoneVerifierMetadata {
        val metadata = PhoneAuthProvider().verifyPhoneNumber(
            phoneNumber,
            phoneVerificationProvider.toPhoneVerificationProvider()
        )
        firebaseAuthPreferences.putSecureString(
            Constants.VERIFICATION_PHONE_NUMBER_VERIFICATION_ID,
            metadata.verificationId
        )

        firebaseAuthPreferences.putSecureString(
            Constants.VERIFICATION_PHONE_NUMBER,
            metadata.phoneNumber
        )

        return metadata.toPhoneVerifierMetadata()
    }

    @Throws(Throwable::class)
    override suspend fun resendVerificationCode(phoneVerificationProvider: PhoneVerifierProvider): PhoneVerifierMetadata {
        val phoneNumber = firebaseAuthPreferences.getSecureString(Constants.VERIFICATION_PHONE_NUMBER)
        if (phoneNumber.isNullOrEmpty()) throw Throwable("Invalid phone number")
        return PhoneAuthProvider().verifyPhoneNumber(
            phoneNumber,
            phoneVerificationProvider.toPhoneVerificationProvider()
        ).toPhoneVerifierMetadata()
    }

    @Throws(Throwable::class)
    override suspend fun verifyPhoneVerification(code: String): String {
        val verificationId =
            firebaseAuthPreferences.getSecureString(Constants.VERIFICATION_PHONE_NUMBER_VERIFICATION_ID)
        if (verificationId.isNullOrEmpty()) throw Throwable("Unable to verify phone number")
        val credentials = PhoneAuthProvider().credential(verificationId, code)
        return authenticateWithCredentials(credentials)
    }

    @Throws(Throwable::class)
    override suspend fun signOut() {
        Firebase.auth.signOut()
    }

    @Throws(Throwable::class)
    private suspend fun authenticateWithCredentials(credentials: AuthCredential): String {
        val user = Firebase.auth.signInWithCredential(credentials).user
            ?: throw Throwable("Signing in failed user is null")
        return user.getIdToken(true) ?: throw Throwable("Unable to getIdToken")
    }
}