package com.trobatapp.models

import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class Usuario(
    val id: String? = null, // Uso String para ID para evitar problemas de serialización en JSON
    val name: String,
    val email: String,
    val password_hash: String,
    val fcm_tokens: List<String> = emptyList(),
    val role: String = "user",
    val created_at: String? = null,
    val last_login: String? = null,
    val is_verified: Boolean = false
)