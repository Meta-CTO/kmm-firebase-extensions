package com.metacto.kmm.firebase.auth.extensions.mappers

import com.metacto.kmm.auth.common.PhoneVerifierMetadata
import com.metacto.kmm.auth.common.PhoneVerifierProvider
import dev.gitlive.firebase.auth.PhoneVerificationMetadata
import dev.gitlive.firebase.auth.PhoneVerificationProvider

actual fun PhoneVerificationProvider.toPhoneVerifierProvider(): PhoneVerifierProvider {
    return object : PhoneVerifierProvider {}
}

actual fun PhoneVerifierProvider.toPhoneVerificationProvider(): PhoneVerificationProvider {
    return object : PhoneVerificationProvider {}
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