package com.metacto.kmm.firebase.remoteconfig

import com.metacto.kmm.remoteconfig.common.RemoteConfigProvider
import com.metacto.kmm.sharedpreferences.KmmPreference

expect class FirebaseRemoteConfigsProvider(
    remoteConfigPreferences: KmmPreference,
): RemoteConfigProvider