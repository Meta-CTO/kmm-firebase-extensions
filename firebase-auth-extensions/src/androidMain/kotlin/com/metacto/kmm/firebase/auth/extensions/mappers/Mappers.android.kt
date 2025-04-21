package com.metacto.kmm.firebase.auth.extensions.mappers

import android.app.Activity
import com.metacto.kmm.auth.common.PhoneVerifierMetadata
import com.metacto.kmm.auth.common.PhoneVerifierProvider
//import dev.gitlive.firebase.auth.PhoneVerificationMetadata
//import dev.gitlive.firebase.auth.PhoneVerificationProvider
import java.util.concurrent.TimeUnit

//actual fun PhoneVerificationProvider.toPhoneVerifierProvider(): PhoneVerifierProvider {
//    return object : PhoneVerifierProvider {
//        override val activity: Activity = this@toPhoneVerifierProvider.activity
//        override val timeout: Long = this@toPhoneVerifierProvider.timeout
//        override val unit: TimeUnit = this@toPhoneVerifierProvider.unit
//        override suspend fun getVerificationCode(): String =
//            this@toPhoneVerifierProvider.getVerificationCode()
//
//        override fun onCodeSent(verificationId: String, triggerResend: () -> Unit) =
//            this@toPhoneVerifierProvider.onCodeSent(verificationId, triggerResend)
//    }
//}
//
//actual fun PhoneVerifierProvider.toPhoneVerificationProvider(): PhoneVerificationProvider {
//    return object : PhoneVerificationProvider {
//        override val activity: Activity = this@toPhoneVerificationProvider.activity
//        override val timeout: Long = this@toPhoneVerificationProvider.timeout
//        override val unit: TimeUnit = this@toPhoneVerificationProvider.unit
//        override suspend fun getVerificationCode() = this@toPhoneVerificationProvider.getVerificationCode()
//        override fun onCodeSent(verificationId: String, triggerResend: () -> Unit)  = this@toPhoneVerificationProvider.onCodeSent(verificationId, triggerResend)
//    }
//}
//
//actual fun PhoneVerifierMetadata.toPhoneVerificationMetadata(): PhoneVerificationMetadata {
//    return PhoneVerificationMetadata(
//        phoneNumber = this.phoneNumber,
//        verificationId = this.verificationId
//    )
//}
//
//actual fun PhoneVerificationMetadata.toPhoneVerifierMetadata(): PhoneVerifierMetadata {
//    return PhoneVerifierMetadata(
//        phoneNumber = this.phoneNumber,
//        verificationId = this.verificationId
//    )
//}