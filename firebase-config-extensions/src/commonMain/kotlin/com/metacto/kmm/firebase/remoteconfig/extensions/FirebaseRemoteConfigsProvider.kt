package com.metacto.kmm.firebase.remoteconfig.extensions

import com.metacto.kmm.firebase.remoteconfig.extensions.constants.Constants
import com.metacto.kmm.remoteconfig.common.RemoteConfigProvider
import dev.gitlive.firebase.remoteconfig.FirebaseRemoteConfig
import dev.gitlive.firebase.remoteconfig.get

class FirebaseRemoteConfigsProvider(
    private val remoteConfigPreferences: FirebaseRemoteConfigPreferences,
    private val firebaseConfigs: FirebaseRemoteConfig,
    private val logger: FirebaseRemoteConfigLogger? = null
) : RemoteConfigProvider {

    override suspend fun init(minFetchIntervalSeconds: Long) {
        setSettings(minFetchIntervalSeconds)
        loadDefaults()
        fetchConfigsFromRemote()
    }

    private suspend fun setSettings(minimumFetchIntervalSeconds: Long) {
        try {
            firebaseConfigs.settings {
                this.minimumFetchIntervalInSeconds = minimumFetchIntervalSeconds
            }
            logger?.log("$LOG_TAG: Updated settings")
        } catch (e: Throwable) {
            logger?.log("$LOG_TAG: Error: (${e.message})")
        }
    }

    private suspend fun loadDefaults() {
        try {
            // Get and validate cached configs
            val configsObject = remoteConfigPreferences.getJsonObject(Constants.CACHED_REMOTE_CONFIGS) ?: return

            // Set defaults
            firebaseConfigs.setDefaults(
                *configsObject.toPairs().toTypedArray()
            )

            logger?.log("$LOG_TAG: Loaded defaults ($configsObject)")
        } catch (e: Throwable) {
            logger?.log("$LOG_TAG: Error: (${e.message})")
        }
    }

    private suspend fun fetchConfigsFromRemote() {
        try {
            // Fetch
            firebaseConfigs.fetchAndActivate()
            val updatedConfigs = firebaseConfigs.all
                .map { (key, value) -> key to value.asString() }
                .toMap()
                .toJsonObject()

            // Then cache it
            remoteConfigPreferences.putObject(Constants.CACHED_REMOTE_CONFIGS, updatedConfigs)

            logger?.log("$LOG_TAG: Updated configs from remote ($updatedConfigs)")
        } catch (e: Throwable) {
            logger?.log("$LOG_TAG: Error: (${e.message})")
        }
    }

    override fun getString(key: String): String? {
        return firebaseConfigs[key]
    }

    @Throws(Throwable::class)
    override suspend fun forceGetString(key: String): String? {
        val value = getString(key)
        if (value == null || value == DEF_STRING_VALUE) {
            fetchConfigsFromRemote()
        }

        return getString(key)
    }

    override fun getBoolean(key: String): Boolean? {
        return firebaseConfigs[key]
    }

    @Throws(Throwable::class)
    override suspend fun forceGetBoolean(key: String): Boolean? {
        val value = getBoolean(key)
        if (value == null || value == DEF_BOOL_VALUE) {
            fetchConfigsFromRemote()
        }

        return getBoolean(key)
    }

    override fun getDouble(key: String): Double? {
        return firebaseConfigs[key]
    }

    @Throws(Throwable::class)
    override suspend fun forceGetDouble(key: String): Double? {
        val value = getDouble(key)
        if (value == null || value == DEF_DOUBLE_VALUE) {
            fetchConfigsFromRemote()
        }

        return getDouble(key)
    }

    override fun getLong(key: String): Long? {
        return firebaseConfigs[key]
    }

    @Throws(Throwable::class)
    override suspend fun forceGetLong(key: String): Long? {
        val value = getLong(key)
        if (value == null || value == DEF_LONG_VALUE) {
            fetchConfigsFromRemote()
        }

        return getLong(key)
    }

    override fun getInt(key: String): Int? {
        return firebaseConfigs[key]
    }

    @Throws(Throwable::class)
    override suspend fun forceGetInt(key: String): Int? {
        val value = getInt(key)
        if (value == null || value == DEF_INT_VALUE) {
            fetchConfigsFromRemote()
        }

        return getInt(key)
    }

    companion object {
        private const val LOG_TAG = "RemoteConfigs"
        private const val DEF_STRING_VALUE = ""
        private const val DEF_BOOL_VALUE = false
        private const val DEF_DOUBLE_VALUE = 0.0
        private const val DEF_LONG_VALUE = 0L
        private val DEF_INT_VALUE: Int? = null
    }
}
