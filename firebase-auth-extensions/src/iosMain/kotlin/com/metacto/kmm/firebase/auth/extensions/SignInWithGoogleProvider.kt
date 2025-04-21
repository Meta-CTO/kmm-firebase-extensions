package com.metacto.kmm.firebase.auth.extensions

import cocoapods.FirebaseCore.FIRApp
import GoogleSignIn.GIDConfiguration
import GoogleSignIn.GIDSignIn
import com.metacto.kmm.auth.common.AuthenticationMetadata
import com.metacto.kmm.auth.common.ProfileMetadata
import com.metacto.kmm.firebase.auth.extensions.mappers.mapError
import kotlinx.coroutines.suspendCancellableCoroutine

import platform.UIKit.UIViewController
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

            GIDSignIn.sharedInstance.signInWithPresentingViewController(
                presentingViewController,
                completion = { result, error ->
                    error?.let {
                        continuation.resumeWithException(
                            it.localizedDescription.mapError(it.code.toInt())
                        )
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