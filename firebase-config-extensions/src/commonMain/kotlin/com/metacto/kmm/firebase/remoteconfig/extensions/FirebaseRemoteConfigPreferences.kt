package com.metacto.kmm.firebase.remoteconfig.extensions

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

interface FirebaseRemoteConfigPreferences {
    fun getString(key: String): String?
    fun putString(key: String, value: String)
}

inline fun <reified T> FirebaseRemoteConfigPreferences.getObject(key: String): T? {
    return try {
        val serializedObj = getString(key)
        serializedObj?.let { Json.decodeFromString(it) }
    } catch (_: Throwable) {
        null
    }
}

inline fun FirebaseRemoteConfigPreferences.getJsonObject(key: String): JsonObject? {
    val cachedConfigs = getString(key) ?: return null
    return Json.parseToJsonElement(cachedConfigs).jsonObject
}

inline fun <reified T> FirebaseRemoteConfigPreferences.putObject(key: String, value: T) {
    val serializedObj = Json.encodeToString(value)
    putString(key, serializedObj)
}
