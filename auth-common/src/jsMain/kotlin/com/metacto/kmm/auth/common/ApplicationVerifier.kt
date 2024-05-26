package com.metacto.kmm.auth.common

import kotlin.js.Promise

interface ApplicationVerifier {
    val type: String
    fun verify(): Promise<String>
}
