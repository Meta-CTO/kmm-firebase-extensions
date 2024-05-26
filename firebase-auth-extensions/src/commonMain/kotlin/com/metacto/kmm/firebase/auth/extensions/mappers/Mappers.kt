package com.metacto.kmm.firebase.auth.extensions.mappers

import com.metacto.kmm.auth.common.PhoneVerifierMetadata
import com.metacto.kmm.auth.common.PhoneVerifierProvider
import dev.gitlive.firebase.auth.PhoneVerificationMetadata
import dev.gitlive.firebase.auth.PhoneVerificationProvider

expect fun PhoneVerificationProvider.toPhoneVerifierProvider(): PhoneVerifierProvider

expect fun PhoneVerifierProvider.toPhoneVerificationProvider(): PhoneVerificationProvider

expect fun PhoneVerifierMetadata.toPhoneVerificationMetadata(): PhoneVerificationMetadata

expect fun PhoneVerificationMetadata.toPhoneVerifierMetadata(): PhoneVerifierMetadata