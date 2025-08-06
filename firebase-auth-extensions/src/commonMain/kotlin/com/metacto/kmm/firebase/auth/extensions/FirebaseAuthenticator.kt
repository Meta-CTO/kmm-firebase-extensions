package com.metacto.kmm.firebase.auth.extensions

import com.metacto.kmm.auth.common.AuthOptions
import com.metacto.kmm.auth.common.AuthenticationMetadata
import com.metacto.kmm.auth.common.Authenticator
import com.metacto.kmm.auth.common.PhoneVerifierMetadata
import com.metacto.kmm.auth.common.PhoneVerifierProvider
import com.metacto.kmm.firebase.auth.ActionCodeSettings
import com.metacto.kmm.firebase.auth.extensions.constants.Constants
import com.metacto.kmm.sharedpreferences.KmmPreference

class EmailAlreadyExistsThrowable(message: String = "User with this email already exists") : Throwable(message)

class WeakPasswordThrowable(message: String) : Throwable("Password is too weak: $message")

class InvalidEmailThrowable(message: String = "Invalid email format") : Throwable(message)

@Throws(Throwable::class)
expect suspend fun FirebaseAuthenticator.getIdToken(forceRefresh: Boolean): String

@Throws(Throwable::class)
expect suspend fun FirebaseAuthenticator.sendSignInLinkToEmail(
    email: String,
    actionCodeSettings: ActionCodeSettings
)

@Throws(Throwable::class)
expect suspend fun FirebaseAuthenticator.signInWithEmailLink(email: String, link: String): String

@Throws(Throwable::class)
expect suspend fun FirebaseAuthenticator.signInWithEmailAndPassword(email: String, password: String): String

@Throws(Throwable::class)
expect suspend fun FirebaseAuthenticator.signUpWithEmailAndPassword(email: String, password: String, actionCodeSettings: ActionCodeSettings? = null): String

@Throws(Throwable::class)
expect suspend fun FirebaseAuthenticator.sendPasswordResetEmail(email: String, actionCodeSettings: ActionCodeSettings? = null): Boolean

@Throws(Throwable::class)
expect suspend fun FirebaseAuthenticator.sendEmailVerification(): Boolean

@Throws(Throwable::class)
expect suspend fun FirebaseAuthenticator.isCurrentUserEmailVerified(): Boolean

@Throws(Throwable::class)
expect suspend fun FirebaseAuthenticator.verifyPhoneNumber(otp: String, verificationId: String): String

@Throws(Throwable::class)
expect suspend fun FirebaseAuthenticator.sendSignInOTPToPhone(phoneNumber: String, phoneVerifierProvider: PhoneVerifierProvider?): PhoneVerifierMetadata

@Throws(Throwable::class)
expect suspend fun FirebaseAuthenticator.logout()

class FirebaseAuthenticator(
    private val actionCodeSettings: ActionCodeSettings,
    private val kmmPreference: KmmPreference
) : Authenticator {
    private val authClient by lazy { AuthClient() }

    @Throws(Throwable::class)
    override suspend fun authenticateCurrentUser(): String {
        return getIdToken(true)
    }

    @Throws(Throwable::class)
    override suspend fun authenticateWithGoogle(authOptions: AuthOptions): AuthenticationMetadata {
        authClient.setAuthOptions(authOptions)
        authClient.init()
        return authClient.signInWithGoogle()
    }

    @Throws(Throwable::class)
    override suspend fun authenticateWithApple(authOptions: AuthOptions): AuthenticationMetadata {
        authClient.setAuthOptions(authOptions)
        authClient.init()
        return authClient.signInWithApple()
    }

    @Throws(Throwable::class)
    override suspend fun sendEmailLink(email: String) {
        kmmPreference.putSecureString(Constants.SIGN_IN_EMAIL_LINK_EMAIL, email)
        sendSignInLinkToEmail(email, actionCodeSettings)
    }

    @Throws(Throwable::class)
    override suspend fun resendSignInLink() {
        val email = kmmPreference.getSecureString(Constants.SIGN_IN_EMAIL_LINK_EMAIL)
        if (email.isNullOrEmpty()) throw Throwable("Email is null or empty, please try again.")
        sendEmailLink(email)
    }

    @Throws(Throwable::class)
    override suspend fun verifyEmailLink(link: String): String {
        val email = kmmPreference.getSecureString(Constants.SIGN_IN_EMAIL_LINK_EMAIL)
        if (email.isNullOrEmpty()) throw Throwable("Email is null or empty, please try again.")
        return signInWithEmailLink(email, link)
    }

    @Throws(Throwable::class)
    override suspend fun sendPhoneVerification(
        phoneNumber: String,
        phoneVerificationProvider: PhoneVerifierProvider?
    ): PhoneVerifierMetadata {
        val metadata = sendSignInOTPToPhone(phoneNumber, phoneVerificationProvider)

        kmmPreference.putSecureString(
            Constants.VERIFICATION_PHONE_NUMBER_VERIFICATION_ID,
            metadata.verificationId
        )

        kmmPreference.putSecureString(
            Constants.VERIFICATION_PHONE_NUMBER,
            metadata.phoneNumber
        )

        return metadata
    }

    @Throws(Throwable::class)
    override suspend fun resendVerificationCode(phoneVerificationProvider: PhoneVerifierProvider?): PhoneVerifierMetadata {
        val phoneNumber =
            kmmPreference.getSecureString(Constants.VERIFICATION_PHONE_NUMBER)
        if (phoneNumber.isNullOrEmpty()) throw Throwable("Invalid phone number")
        return sendPhoneVerification(
            phoneNumber,
            phoneVerificationProvider
        )
    }

    @Throws(Throwable::class)
    override suspend fun verifyPhoneOTP(code: String): String {
        val verificationId =
            kmmPreference.getSecureString(Constants.VERIFICATION_PHONE_NUMBER_VERIFICATION_ID)
        if (verificationId.isNullOrEmpty()) throw Throwable("Unable to verify phone number")
        return verifyPhoneNumber(code, verificationId)
    }

    @Throws(Throwable::class)
    override suspend fun signInWithEmailPassword(email: String, password: String): String {
        if (email.isEmpty() || password.isEmpty()) {
            throw InvalidEmailThrowable("Email or password cannot be empty")
        }
        return signInWithEmailAndPassword(email, password)
    }

    @Throws(Throwable::class)
    override suspend fun signUpWithEmailPassword(email: String, password: String): String {
        if (email.isEmpty() || password.isEmpty()) {
            throw InvalidEmailThrowable("Email or password cannot be empty")
        }
        return signUpWithEmailAndPassword(email, password)
    }

    @Throws(Throwable::class)
    override suspend fun sendPasswordResetUsingEmail(email: String): Boolean {
        if (email.isEmpty()) {
            throw InvalidEmailThrowable("Email cannot be empty")
        }
        return sendPasswordResetEmail(email)
    }

    @Throws(Throwable::class)
    override suspend fun sendUserEmailVerification(): Boolean {
        return sendEmailVerification()
    }

    @Throws(Throwable::class)
    override suspend fun isCurrentEmailVerified(): Boolean {
        return isCurrentUserEmailVerified()
    }

    @Throws(Throwable::class)
    override suspend fun signOut() {
        logout()
    }
}