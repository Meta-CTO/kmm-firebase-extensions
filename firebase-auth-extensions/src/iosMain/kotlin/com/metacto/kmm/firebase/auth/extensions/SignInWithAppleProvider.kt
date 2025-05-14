@file:OptIn(BetaInteropApi::class)

package com.metacto.kmm.firebase.auth.extensions

import com.metacto.kmm.auth.common.AuthenticationMetadata
import com.metacto.kmm.auth.common.ProfileMetadata
import com.metacto.kmm.firebase.auth.extensions.mappers.mapError
import kotlinx.cinterop.BetaInteropApi
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.AuthenticationServices.*
import platform.Foundation.NSError
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.darwin.NSObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
class SignInWithAppleProvider(
    private val presentationAnchor: ASPresentationAnchor
) : NSObject(),
    ASAuthorizationControllerDelegateProtocol,
    ASAuthorizationControllerPresentationContextProvidingProtocol {
    private var continuation: CancellableContinuation<AuthenticationMetadata>? = null

    suspend fun start(): AuthenticationMetadata {
        return suspendCancellableCoroutine { continuation ->
            this.continuation = continuation
            val request = ASAuthorizationAppleIDProvider().createRequest()
            request.requestedScopes = listOf(ASAuthorizationScopeEmail, ASAuthorizationScopeFullName)

            val controller = ASAuthorizationController(listOf(request))
            controller.delegate = this
            controller.presentationContextProvider = this
            controller.performRequests()
        }
    }

    override fun authorizationController(
        controller: ASAuthorizationController,
        didCompleteWithAuthorization: ASAuthorization
    ) {
        val credential =
            didCompleteWithAuthorization.credential as? ASAuthorizationAppleIDCredential
        val idToken = credential?.identityToken?.let {
            return@let NSString.create(it, NSUTF8StringEncoding) as String?
        }

        val profile = ProfileMetadata(
            firstName = credential?.fullName?.givenName,
            lastName = credential?.fullName?.familyName,
            email = credential?.email,
            phoneNumber = null,
            pictureUrl = null
        )

        idToken?.let {
            continuation?.resumeIfActive(AuthenticationMetadata(it, profile))
        } ?: continuation?.exceptionIfActive("idToken cannot be null".mapError(-1))
    }

    override fun authorizationController(
        controller: ASAuthorizationController,
        didCompleteWithError: NSError
    ) {
        continuation?.exceptionIfActive(
            didCompleteWithError.localizedDescription.mapError(didCompleteWithError.code.toInt())
        )
    }

    override fun presentationAnchorForAuthorizationController(controller: ASAuthorizationController): ASPresentationAnchor {
        return presentationAnchor
    }
}