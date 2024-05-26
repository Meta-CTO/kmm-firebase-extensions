package com.metacto.kmm.firebase.auth.extensions.mappers

import com.metacto.kmm.auth.common.ApplicationVerifier
import com.metacto.kmm.auth.common.PhoneVerifierMetadata
import com.metacto.kmm.auth.common.PhoneVerifierProvider
import dev.gitlive.firebase.auth.PhoneVerificationMetadata
import dev.gitlive.firebase.auth.PhoneVerificationProvider
import kotlin.js.Promise

actual fun PhoneVerificationProvider.toPhoneVerifierProvider(): PhoneVerifierProvider {
    return object : PhoneVerifierProvider {
        override val verifier: ApplicationVerifier
            get() = object : ApplicationVerifier {
                override fun verify(): Promise<String> =
                    this@toPhoneVerifierProvider.verifier.verify()

                override val type: String
                    get() = this@toPhoneVerifierProvider.verifier.type
            }

        override suspend fun getVerificationCode(verificationId: String): String =
            this@toPhoneVerifierProvider.getVerificationCode(verificationId)
    }
}

actual fun PhoneVerifierProvider.toPhoneVerificationProvider(): PhoneVerificationProvider {
    return object : PhoneVerificationProvider {
        override val verifier: dev.gitlive.firebase.auth.externals.ApplicationVerifier
            get() = object : dev.gitlive.firebase.auth.externals.ApplicationVerifier {
                override val type: String = this@toPhoneVerificationProvider.verifier.type

                override fun verify(): Promise<String> =
                    this@toPhoneVerificationProvider.verifier.verify()
            }

        override suspend fun getVerificationCode(verificationId: String) =
            this@toPhoneVerificationProvider.getVerificationCode(verificationId)
    }
}

actual fun PhoneVerifierMetadata.toPhoneVerificationMetadata(): PhoneVerificationMetadata {
    return PhoneVerificationMetadata(
        phoneNumber = this.phoneNumber,
        verificationId = this.verificationId
    )
}

actual fun PhoneVerificationMetadata.toPhoneVerifierMetadata(): PhoneVerifierMetadata {
    return PhoneVerifierMetadata(
        phoneNumber = this.phoneNumber,
        verificationId = this.verificationId
    )
}