package com.metacto.kmm.auth.common

import android.app.Activity
import android.content.Intent
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher

actual class AuthOptions(
    val activity: Activity,
    val launcher: ActivityResultLauncher<Intent>,
    val onCanceled: () -> Unit = {}
) {
    var onResult: (ActivityResult) -> Unit = {}
}