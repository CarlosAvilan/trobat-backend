package com.trobatapp.DTO

import kotlinx.serialization.Serializable

@Serializable
class RegistrarUsuarioParamsDTO(
    val name: String,
    val email: String,
    val password: String
) {
}