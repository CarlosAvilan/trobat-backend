package com.trobatapp.DTO

import kotlinx.serialization.Serializable

@Serializable
class LoginParamsDTO(
    val email: String,
    val password: String
) {
}