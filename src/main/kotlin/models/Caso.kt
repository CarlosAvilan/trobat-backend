package com.trobatapp.models

import kotlinx.serialization.Serializable

@Serializable
data class Desaparecido(
    val name: String = "",
    val description: String = "",
    val age: Int,
    val image: String,
    val last_known_location: Ubicacion? = null,
    val location_label: String? = null
)

@Serializable
data class RepresentanteExterno(
    val name: String = "",
    val email: String = "",
    val phone: String = ""
)

@Serializable
data class CrearCasoRequest(
    val admin_officer_id: String,
    val assigned_agents: List<String> = emptyList(),
    val missing_person: Desaparecido,
    val external_contact: RepresentanteExterno
)

@Serializable
data class ActualizarEstadoRequest(
    val status: String
)

@Serializable
data class CasoResponse(
    val id: String,
    val admin_officer_id: String,
    val assigned_agents: List<String>,
    val missing_person: Desaparecido,
    val external_contact: RepresentanteExterno,
    val status: String,
    val total_reports: Int,
    val created_at: String
)

@Serializable
data class MensajeResponse(val mensaje: String)

@Serializable
data class CrearCasoResponse(val id: String, val mensaje: String)

@Serializable
data class CasosPaginados(
    val data: List<CasoResponse>,
    val total: Long,
    val page: Int,
    val limit: Int,
    val hasMore: Boolean
)

@Serializable
data class CasoCercanoResponse(
    val caso: CasoResponse,
    val distance_km: Double
)

@Serializable
data class CasosCercanosPaginados(
    val data: List<CasoCercanoResponse>,
    val total: Long,
    val page: Int,
    val limit: Int,
    val radius_km: Double,
    val hasMore: Boolean
)
