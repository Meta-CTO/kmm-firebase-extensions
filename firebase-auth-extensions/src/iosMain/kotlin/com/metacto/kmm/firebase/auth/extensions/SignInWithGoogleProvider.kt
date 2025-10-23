package com.metacto.kmm.firebase.auth.extensions

import FirebaseCore.FIRApp
import GoogleSignIn.GIDConfiguration
import GoogleSignIn.GIDSignIn
import GoogleSignIn.kGIDSignInErrorCodeCanceled
import com.metacto.kmm.auth.common.AuthenticationMetadata
import com.metacto.kmm.auth.common.ProfileMetadata
import com.metacto.kmm.firebase.auth.extensions.exceptions.AuthCancelledThrowable
import com.metacto.kmm.firebase.auth.extensions.exceptions.AuthThrowable
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.UIKit.UIViewController
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
class SignInWithGoogleProvider(
    private val presentingViewController: UIViewController
) {
    @Throws(Throwable::class)
    suspend fun start(): AuthenticationMetadata {
        return suspendCancellableCoroutine { continuation ->
            val clientId = FIRApp.defaultApp()?.options?.clientID
                ?: throw Throwable("clientId cannot be null")

            GIDSignIn.sharedInstance.configuration = GIDConfiguration(clientId)

            dispatch_async(dispatch_get_main_queue()) {
                GIDSignIn.sharedInstance.signInWithPresentingViewController(
                    presentingViewController,
                    completion = { result, error ->
                        error?.let {
                            val exception = when (it.code.toInt()) {
                                kGIDSignInErrorCodeCanceled.toInt() -> AuthCancelledThrowable() // Google Sign-In user cancelled
                                else -> AuthThrowable(
                                    message = it.localizedDescription,
                                    code = it.code.toInt()
                                )
                            }
                            continuation.resumeWithException(exception)
                        }

                        val accessToken = result?.user?.accessToken?.tokenString
                        result?.user?.idToken?.tokenString?.let { idToken ->
                            val profile = ProfileMetadata(
                                firstName = result.user.profile?.givenName,
                                lastName = result.user.profile?.familyName,
                                email = result.user.profile?.email,
                                phoneNumber = null,
                                pictureUrl = result.user.profile?.imageURLWithDimension(1080u)?.absoluteString
                            )

                            continuation.resume(AuthenticationMetadata(idToken, profile, accessToken))
                        }
                    }
                )
            }
        }
    }
}