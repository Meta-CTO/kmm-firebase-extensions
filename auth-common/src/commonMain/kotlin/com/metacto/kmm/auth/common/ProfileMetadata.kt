package com.metacto.kmm.auth.common

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProfileMetadata(
    @SerialName("firstName")
    val firstName: String?,
    @SerialName("lastName")
    val lastName: String?,
    @SerialName("email")
    val email: String?,
    @SerialName("phoneNumber")
    val phoneNumber: String?,
    @SerialName("pictureUrl")
    val pictureUrl: String?
)
