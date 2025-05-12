package com.metacto.kmm.firebase.remoteconfig.extensions

import com.metacto.kmm.sharedpreferences.KmmPreference
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject


inline fun KmmPreference.getJsonObject(key: String): JsonObject? {
    val cachedConfigs = getString(key) ?: return null
    return Json.parseToJsonElement(cachedConfigs).jsonObject
}
