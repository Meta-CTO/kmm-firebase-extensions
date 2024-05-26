package com.metacto.kmm.firebase.remoteconfig.extensions

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement

fun <K, V> Map<K, V>.toPairs(): List<Pair<K, V>> {
    return this.map { (key, value) -> key to value }
}

inline fun <reified V> Map<String, V>.toJsonObject(): JsonObject {
    val elements = this.map { (key, value) ->
        key to Json.encodeToJsonElement(value)
    }
    return JsonObject(elements.toMap())
}

fun <K, V> Map<K, V>.getOrNull(key: K): V? {
    return getOrElse(key) { null }
}