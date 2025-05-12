package com.metacto.kmm.firebase.auth.extensions

import com.metacto.kmm.auth.common.ProfileMetadata

import android.app.Activity
import androidx.activity.result.ActivityResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.metacto.kmm.auth.common.AuthOptions
import com.metacto.kmm.auth.common.AuthenticationMetadata
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

actual class AuthClient : AuthProvider {
    private lateinit var gClient: GoogleSignInClient
    private lateinit var options: AuthOptions
    private var continuation: kotlin.coroutines.Continuation<AuthenticationMetadata?>? = null

    actual fun init() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(options.activity.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        gClient = GoogleSignIn.getClient(options.activity, gso)

        options.onResult = {
            if (it.resultCode == Activity.RESULT_CANCELED) {
                continuation?.resume(null)
            } else {
                setActivityResult(it)
            }
        }
    }

    private fun setActivityResult(result: ActivityResult) {
        if (result.resultCode == Activity.RESULT_OK) {
            if (result.data != null) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                try {
                    val account = task.getResult(ApiException::class.java)
                    val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                    val profile = ProfileMetadata(
                        firstName = account.givenName,
                        lastName = account.familyName,
                        email = account.email,
                        phoneNumber = null,
                        pictureUrl = account.photoUrl?.toString()
                    )

                    val firebaseUser = Firebase.auth.signInWithCredential(credential)
                    val idToken = firebaseUser.result.user?.getIdToken(true)?.result?.token
                        ?: throw Throwable("Failed to get ID token")

                    continuation?.resume(AuthenticationMetadata(idToken, profile, null))
                } catch (throwable: Throwable) {
                    continuation?.resumeWithException(throwable)
                }
            }
        }
    }

    override suspend fun signInWithApple(): AuthenticationMetadata? {
        TODO("Not yet implemented")
    }

    override suspend fun signInWithGoogle(): AuthenticationMetadata? {
        return suspendCancellableCoroutine { continuation ->
            this.continuation = continuation
            options.launcher.launch(gClient.signInIntent)
        }
    }

    actual fun setAuthOptions(options: AuthOptions) {
        this.options = options
    }
}
