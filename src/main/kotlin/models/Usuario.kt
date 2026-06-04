package com.trobatapp.models

import kotlinx.serialization.Serializable

@Serializable
data class RegistroRequest(
    val name: String,
    val email: String,
    val password: String
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
    val fcm_token: String? = null
)

@Serializable
data class LoginOficialRequest(
    val email_institucional: String,
    val password: String,
    val fcm_token: String? = null
)

@Serializable
data class LogoutRequest(
    val fcm_token: String
)

@Serializable
data class TokenResponse(
    val token: String,
    val tipo: String,
    val id: String,
    val nombre: String
)

@Serializable
data class UsuarioResponse(
    val id: String,
    val name: String,
    val email: String,
    val role: String,
    val is_verified: Boolean,
    val created_at: String
)

@Serializable
data class OficialResponse(
    val id: String,
    val nombre: String,
    val email_institucional: String,
    val rango: String,
    val legajo: String
)

@Serializable
data class AgregarFcmTokenRequest(
    val fcm_token: String
)
