package com.metacto.kmm.auth.common

import android.app.Activity
import java.util.concurrent.TimeUnit

actual interface PhoneVerifierProvider {
    val activity: Activity
    val timeout: Long
    val unit: TimeUnit
}