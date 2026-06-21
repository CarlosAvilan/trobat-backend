package com.trobatapp.models

import kotlinx.serialization.Serializable

@Serializable
data class CrearNotificacionRequest(
    val titulo: String,
    val descripcion: String
)

@Serializable
data class NotificacionLogResponse(
    val id: String,
    val oficial_id: String,
    val titulo: String,
    val descripcion: String,
    val created_at: String,
    val sent_at: String? = null,
    val fcm_status: String,
    val fcm_message_id: String? = null
)

@Serializable
data class NotificacionesPaginadas(
    val data: List<NotificacionLogResponse>,
    val total: Long,
    val page: Int,
    val limit: Int
)
