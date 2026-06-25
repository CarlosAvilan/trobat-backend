package com.trobatapp.models

import kotlinx.serialization.Serializable

@Serializable
data class InformacionPersonal(
    val national_id: String,
    val full_name: String,
    val phone: String
)

@Serializable
data class RegistroReportanteRequest(
    val email: String,
    val password: String,
    val personal_info: InformacionPersonal
)

@Serializable
data class LoginReportanteRequest(
    val email: String,
    val password: String
)

@Serializable
data class UsuarioReportanteResponse(
    val id: String,
    val email: String,
    val personal_info: InformacionPersonal
)
