package com.metacto.kmm.firebase.remoteconfig

import com.metacto.kmm.firebase.remoteconfig.constants.RemoteConfigConstants
import com.metacto.kmm.firebase.remoteconfig.extensions.exceptionIfActive
import com.metacto.kmm.firebase.remoteconfig.extensions.getJsonObject
import com.metacto.kmm.firebase.remoteconfig.extensions.resumeIfActive
import com.metacto.kmm.firebase.remoteconfig.extensions.toJsonObject
import com.metacto.kmm.logger.Logger
import com.metacto.kmm.remoteconfig.common.RemoteConfigProvider
import com.metacto.kmm.sharedpreferences.KmmPreference
import com.metacto.kmm.sharedpreferences.putObject
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.double
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull

@OptIn(ExperimentalForeignApi::class)
actual class FirebaseRemoteConfigsProvider actual constructor(
    private val remoteConfigPreferences: KmmPreference
) : RemoteConfigProvider {
    private val firebaseConfigs = Dependencies.FirebaseRemoteConfigsProvider()
    private val logger = Logger(RemoteConfigConstants.CACHED_REMOTE_CONFIGS)

    override suspend fun init(minFetchIntervalSeconds: Long) {
        setSettings(minFetchIntervalSeconds)
        loadDefaults()
        fetchConfigsFromRemote()
    }

    private suspend fun setSettings(minimumFetchIntervalSeconds: Long) {
        return suspendCancellableCoroutine { continuation ->
            try {
                firebaseConfigs.setSettingsWithMinFetchIntervalSeconds(minimumFetchIntervalSeconds.toInt(), completion = { error ->
                    if (error != null) {
                        logger.log("${RemoteConfigConstants.LOG_TAG}: Error: (${error.localizedDescription})")
                        continuation.exceptionIfActive(
                            Throwable(error.localizedDescription)
                        )
                    } else {
                        logger.log("${RemoteConfigConstants.LOG_TAG}: Updated settings")
                        continuation.resumeIfActive(Unit)
                    }
                })
            } catch (error: Throwable) {
                logger.log("${RemoteConfigConstants.LOG_TAG}: Error: (${error.message})")
                continuation.exceptionIfActive(error)
            }
        }
    }

    private suspend fun loadDefaults() {
        // Get and validate cached configs
        val configsObject =
            remoteConfigPreferences.getJsonObject(RemoteConfigConstants.CACHED_REMOTE_CONFIGS)
                ?: return

        val mapValues = mutableMapOf<String, Any?>()

        configsObject.forEach { (key, jsonElement) ->
            val primitive = jsonElement as? JsonPrimitive
            val value: Any? = when {
                primitive == null -> null
                primitive.isString -> primitive.content
                primitive.booleanOrNull != null -> primitive.boolean
                primitive.intOrNull != null -> primitive.int
                primitive.doubleOrNull != null -> primitive.double
                else -> primitive.content // fallback to string if type isn't clear
            }
            mapValues[key] = value
        }

        return suspendCancellableCoroutine { continuation ->
            try {
                // Set defaults
                firebaseConfigs.loadDefaultsFrom(mapValues.toMap(), { error ->
                    if (error != null) {
                        logger.log("${RemoteConfigConstants.LOG_TAG}: Error: (${error.localizedDescription})")
                        continuation.exceptionIfActive(
                            Throwable(error.localizedDescription)
                        )
                    } else {
                        logger.log("${RemoteConfigConstants.LOG_TAG}: Updated settings")
                        continuation.resumeIfActive(Unit)
                    }
                })
            } catch (error: Throwable) {
                logger.log("${RemoteConfigConstants.LOG_TAG}: Error: (${error.message})")
            }
        }
    }

    private suspend fun fetchConfigsFromRemote() {
        return suspendCancellableCoroutine { continuation ->
            try {
                // Fetch
                firebaseConfigs.fetchConfigsFromRemoteWithCompletion({ values, error ->
                    if (error != null) {
                        logger.log("${RemoteConfigConstants.LOG_TAG}: Error: (${error.localizedDescription})")
                        continuation.exceptionIfActive(
                            Throwable(error.localizedDescription)
                        )
                    } else {
                        val updatedConfigs = values?.map { (key, value) -> key.toString() to value.toString() }
                            ?.toMap()
                            .orEmpty()
                            .toJsonObject()

                        // Then cache it
                        remoteConfigPreferences.putObject(
                            RemoteConfigConstants.CACHED_REMOTE_CONFIGS,
                            updatedConfigs
                        )

                        logger.log("${RemoteConfigConstants.LOG_TAG}: Updated configs from remote ($updatedConfigs)")
                        continuation.resumeIfActive(Unit)
                    }
                })
            } catch (error: Throwable) {
                logger.log("${RemoteConfigConstants.LOG_TAG}: Error: (${error.message})")
                continuation.exceptionIfActive(error)
            }
        }
    }

    override fun getString(key: String): String? {
        val value = firebaseConfigs.getStringForKey(key)
        return if (value == RemoteConfigConstants.DEF_STRING_VALUE) null else value
    }

    @Throws(Throwable::class)
    override suspend fun forceGetString(key: String): String? {
        val value = getString(key)
        if (value == null || value == RemoteConfigConstants.DEF_STRING_VALUE) {
            fetchConfigsFromRemote()
        }

        return getString(key)
    }

    override fun getBoolean(key: String): Boolean? {
        val value = firebaseConfigs.getBoolForKey(key)
        return if (value == RemoteConfigConstants.DEF_BOOL_VALUE) null else value
    }

    @Throws(Throwable::class)
    override suspend fun forceGetBoolean(key: String): Boolean? {
        val value = getBoolean(key)
        if (value == null || value == RemoteConfigConstants.DEF_BOOL_VALUE) {
            fetchConfigsFromRemote()
        }

        return getBoolean(key)
    }

    override fun getDouble(key: String): Double? {
        val value = firebaseConfigs.getDoubleForKey(key)
        return if (value == RemoteConfigConstants.DEF_DOUBLE_VALUE) null else value
    }

    @Throws(Throwable::class)
    override suspend fun forceGetDouble(key: String): Double? {
        val value = getDouble(key)
        if (value == null || value == RemoteConfigConstants.DEF_DOUBLE_VALUE) {
            fetchConfigsFromRemote()
        }

        return getDouble(key)
    }

    override fun getLong(key: String): Long? {
        val value = firebaseConfigs.getLongForKey(key)
        return if (value == RemoteConfigConstants.DEF_LONG_VALUE) null else value
    }

    @Throws(Throwable::class)
    override suspend fun forceGetLong(key: String): Long? {
        val value = getLong(key)
        if (value == null || value == RemoteConfigConstants.DEF_LONG_VALUE) {
            fetchConfigsFromRemote()
        }

        return getLong(key)
    }

    override fun getInt(key: String): Int? {
        val value = firebaseConfigs.getIntForKey(key)
        return if (value == RemoteConfigConstants.DEF_LONG_VALUE) null else value.toInt()
    }

    @Throws(Throwable::class)
    override suspend fun forceGetInt(key: String): Int? {
        val value = getInt(key)
        if (value == null || value == RemoteConfigConstants.DEF_INT_VALUE) {
            fetchConfigsFromRemote()
        }

        return getInt(key)
    }
}