package com.trobatapp.DTO

import kotlinx.serialization.Serializable

@Serializable
internal class LogoutParamsDTO (
    val fcmToken : String
)