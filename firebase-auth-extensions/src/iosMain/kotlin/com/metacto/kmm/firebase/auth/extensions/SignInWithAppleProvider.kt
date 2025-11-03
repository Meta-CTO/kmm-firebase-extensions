@file:OptIn(BetaInteropApi::class, ExperimentalForeignApi::class)

package com.metacto.kmm.firebase.auth.extensions

import com.metacto.kmm.auth.common.AuthenticationMetadata
import com.metacto.kmm.auth.common.ProfileMetadata
import com.metacto.kmm.firebase.auth.extensions.exceptions.AuthCancelledThrowable
import com.metacto.kmm.firebase.auth.extensions.exceptions.AuthThrowable
import kotlinx.cinterop.*
import kotlinx.coroutines.CompletableDeferred
import platform.AuthenticationServices.*
import platform.Foundation.NSError
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.darwin.NSObject

private class AppleSignInDelegate(
    private val deferred: CompletableDeferred<AuthenticationMetadata>,
    private val presentationAnchor: ASPresentationAnchor,
    private val onComplete: () -> Unit
) : NSObject(),
    ASAuthorizationControllerDelegateProtocol,
    ASAuthorizationControllerPresentationContextProvidingProtocol {

    override fun authorizationController(
        controller: ASAuthorizationController,
        didCompleteWithAuthorization: ASAuthorization
    ) {
        onComplete()

        val credential = didCompleteWithAuthorization.credential as? ASAuthorizationAppleIDCredential

        val idToken = credential?.identityToken?.let {
            NSString.create(it, NSUTF8StringEncoding) as String?
        }

        val accessToken = credential?.authorizationCode?.let {
            NSString.create(it, NSUTF8StringEncoding) as String?
        }

        val profile = ProfileMetadata(
            firstName = credential?.fullName?.givenName,
            lastName = credential?.fullName?.familyName,
            email = credential?.email,
            phoneNumber = null,
            pictureUrl = null
        )

        if (idToken != null) {
            deferred.complete(AuthenticationMetadata(idToken, profile, accessToken))
        } else {
            deferred.completeExceptionally(Throwable("idToken cannot be null"))
        }
    }

    override fun authorizationController(
        controller: ASAuthorizationController,
        didCompleteWithError: NSError
    ) {
        onComplete()

        val error = when (didCompleteWithError.code.toInt()) {
            ASAuthorizationErrorCanceled.toInt() -> AuthCancelledThrowable()
            else -> AuthThrowable(
                message = didCompleteWithError.localizedDescription,
                code = didCompleteWithError.code.toInt()
            )
        }
        deferred.completeExceptionally(error)
    }

    override fun presentationAnchorForAuthorizationController(
        controller: ASAuthorizationController
    ): ASPresentationAnchor {
        return presentationAnchor
    }
}

class SignInWithAppleProvider(
    private val presentationAnchor: ASPresentationAnchor
) {
    private var controller: ASAuthorizationController? = null
    private var delegate: AppleSignInDelegate? = null

    suspend fun start(): AuthenticationMetadata {
        val deferred = CompletableDeferred<AuthenticationMetadata>()

        delegate = AppleSignInDelegate(
            deferred = deferred,
            presentationAnchor = presentationAnchor,
            onComplete = {
                controller = null
                delegate = null
            }
        )

        val request = ASAuthorizationAppleIDProvider().createRequest()
        request.requestedScopes = listOf(ASAuthorizationScopeEmail, ASAuthorizationScopeFullName)

        controller = ASAuthorizationController(listOf(request))
        controller?.delegate = delegate
        controller?.presentationContextProvider = delegate

        deferred.invokeOnCompletion {
            controller = null
            delegate = null
        }

        controller?.performRequests()

        return deferred.await()
    }
}
