package com.metacto.kmm.firebase.remoteconfig

import com.metacto.kmm.remoteconfig.common.RemoteConfigProvider
import com.metacto.kmm.sharedpreferences.KmmPreference

expect class FirebaseRemoteConfigsProvider(
    remoteConfigPreferences: KmmPreference,
): RemoteConfigProvider {
    override suspend fun init(minFetchIntervalSeconds: Long)
    override fun getString(key: String): String?
    override fun getBoolean(key: String): Boolean?
    override fun getInt(key: String): Int?
    override fun getDouble(key: String): Double?
    override fun getLong(key: String): Long?
    override suspend fun forceGetString(key: String): String?
    override suspend fun forceGetBoolean(key: String): Boolean?
    override suspend fun forceGetInt(key: String): Int?
    override suspend fun forceGetDouble(key: String): Double?
    override suspend fun forceGetLong(key: String): Long?
}