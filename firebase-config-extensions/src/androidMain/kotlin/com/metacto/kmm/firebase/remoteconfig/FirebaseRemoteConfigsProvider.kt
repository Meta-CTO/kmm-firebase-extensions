package com.metacto.kmm.firebase.remoteconfig

import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.google.firebase.remoteconfig.remoteConfig
import com.metacto.kmm.firebase.remoteconfig.constants.RemoteConfigConstants
import com.metacto.kmm.firebase.remoteconfig.extensions.getJsonObject
import com.metacto.kmm.firebase.remoteconfig.extensions.toJsonObject
import com.metacto.kmm.logger.Logger
import com.metacto.kmm.remoteconfig.common.RemoteConfigProvider
import com.metacto.kmm.sharedpreferences.KmmPreference
import com.metacto.kmm.sharedpreferences.putObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

actual class FirebaseRemoteConfigsProvider actual constructor(
    private val remoteConfigPreferences: KmmPreference
) : RemoteConfigProvider {
    private val firebaseConfigs = Firebase.remoteConfig
    private val logger = Logger(RemoteConfigConstants.CACHED_REMOTE_CONFIGS)

    actual override suspend fun init(minFetchIntervalSeconds: Long) {
        setSettings(minFetchIntervalSeconds)
        loadDefaults()
        fetchConfigsFromRemote()
    }

    private suspend fun setSettings(minimumFetchIntervalSeconds: Long) {
        return suspendCancellableCoroutine { continuation ->
            try {
                val settings = FirebaseRemoteConfigSettings.Builder()
                    .setMinimumFetchIntervalInSeconds(minimumFetchIntervalSeconds).build()
                firebaseConfigs.setConfigSettingsAsync(settings)
                    .addOnSuccessListener {
                        logger.log("${RemoteConfigConstants.LOG_TAG}: Updated settings")
                        continuation.resume(Unit)
                    }.addOnFailureListener { error ->
                        logger.log("${RemoteConfigConstants.LOG_TAG}: Error: (${error.message})")
                        continuation.resumeWithException(error)
                    }
            } catch (error: Throwable) {
                logger.log("${RemoteConfigConstants.LOG_TAG}: Error: (${error.message})")
                continuation.resumeWithException(error)
            }
        }
    }

    private suspend fun loadDefaults() {
        // Get and validate cached configs
        val configsObject =
            remoteConfigPreferences.getJsonObject(RemoteConfigConstants.CACHED_REMOTE_CONFIGS)
                ?: return

        return suspendCancellableCoroutine { continuation ->
            try {
                // Set defaults
                firebaseConfigs.setDefaultsAsync(configsObject)
                    .addOnSuccessListener {
                        logger.log("${RemoteConfigConstants.LOG_TAG}: Updated settings")
                        continuation.resume(Unit)
                    }.addOnFailureListener { error ->
                        logger.log("${RemoteConfigConstants.LOG_TAG}: Error: (${error.message})")
                        continuation.resumeWithException(error)
                    }
            } catch (error: Throwable) {
                logger.log("${RemoteConfigConstants.LOG_TAG}: Error: (${error.message})")
                continuation.resumeWithException(error)
            }
        }
    }

    private suspend fun fetchConfigsFromRemote() {
        return suspendCancellableCoroutine { continuation ->
            try {
                // Fetch
                firebaseConfigs.fetchAndActivate()
                    .addOnSuccessListener {
                        val updatedConfigs = firebaseConfigs.all
                            .map { (key, value) -> key to value.asString() }
                            .toMap()
                            .toJsonObject()

                        // Then cache it
                        remoteConfigPreferences.putObject(
                            RemoteConfigConstants.CACHED_REMOTE_CONFIGS,
                            updatedConfigs
                        )

                        logger.log("${RemoteConfigConstants.LOG_TAG}: Updated configs from remote ($updatedConfigs)")
                        continuation.resume(Unit)
                    }.addOnFailureListener { error ->
                        logger.log("${RemoteConfigConstants.LOG_TAG}: Error: (${error.message})")
                        continuation.resumeWithException(error)
                    }
            } catch (error: Throwable) {
                logger.log("${RemoteConfigConstants.LOG_TAG}: Error: (${error.message})")
                continuation.resumeWithException(error)
            }
        }
    }

    actual override fun getString(key: String): String? {
        val value = firebaseConfigs.getString(key)
        return if (value == RemoteConfigConstants.DEF_STRING_VALUE) null else value
    }

    @Throws(Throwable::class)
    actual override suspend fun forceGetString(key: String): String? {
        val value = getString(key)
        if (value == null || value == RemoteConfigConstants.DEF_STRING_VALUE) {
            fetchConfigsFromRemote()
        }

        return getString(key)
    }

    actual override fun getBoolean(key: String): Boolean? {
        return getBooleanValue(key, false)
    }

    private fun getBooleanValue(key: String, isForceGet: Boolean): Boolean {
        val value = firebaseConfigs.getBoolean(key)
        return if (value == RemoteConfigConstants.DEF_BOOL_VALUE && isForceGet) {
            false
        } else {
            value
        }
    }

    @Throws(Throwable::class)
    actual override suspend fun forceGetBoolean(key: String): Boolean? {
        val value = getBoolean(key)
        if (value == null || value == RemoteConfigConstants.DEF_BOOL_VALUE) {
            fetchConfigsFromRemote()
        }

        return getBooleanValue(key, true)
    }

    actual override fun getDouble(key: String): Double? {
        val value = firebaseConfigs.getDouble(key)
        return if (value == RemoteConfigConstants.DEF_DOUBLE_VALUE) null else value
    }

    @Throws(Throwable::class)
    actual override suspend fun forceGetDouble(key: String): Double? {
        val value = getDouble(key)
        if (value == null || value == RemoteConfigConstants.DEF_DOUBLE_VALUE) {
            fetchConfigsFromRemote()
        }

        return getDouble(key)
    }

    actual override fun getLong(key: String): Long? {
        val value = firebaseConfigs.getLong(key)
        return if (value == RemoteConfigConstants.DEF_LONG_VALUE) null else value
    }

    @Throws(Throwable::class)
    actual override suspend fun forceGetLong(key: String): Long? {
        val value = getLong(key)
        if (value == null || value == RemoteConfigConstants.DEF_LONG_VALUE) {
            fetchConfigsFromRemote()
        }

        return getLong(key)
    }

    actual override fun getInt(key: String): Int? {
        val value = firebaseConfigs.getLong(key)
        return if (value == RemoteConfigConstants.DEF_LONG_VALUE) null else value.toInt()
    }

    @Throws(Throwable::class)
    actual override suspend fun forceGetInt(key: String): Int? {
        val value = getInt(key)
        if (value == null || value == RemoteConfigConstants.DEF_INT_VALUE) {
            fetchConfigsFromRemote()
        }

        return getInt(key)
    }
}
