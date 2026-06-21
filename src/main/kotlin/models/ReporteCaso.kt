package com.trobatapp.models

import kotlinx.serialization.Serializable

@Serializable
data class SecurityMetadata(
    val anonymous: Boolean = true
)

@Serializable
data class ContactInfo(
    val name: String? = null,
    val phone: String? = null,
    val email: String? = null
)

@Serializable
data class CrearReporteRequest(
    val case_id: String,
    val location: Ubicacion,
    val description: String,
    val photo_url: String? = null,
    val police_priority: Boolean = false,
    val security_metadata: SecurityMetadata = SecurityMetadata(),
    val contact_info: ContactInfo = ContactInfo()
)

@Serializable
data class ValidarReporteRequest(
    val validated: Boolean,
    val priority: String? = null
)

@Serializable
data class ReporteCasoResponse(
    val id: String,
    val case_id: String,
    val location: Ubicacion,
    val location_label: String? = null,
    val timestamp: String,
    val description: String,
    val photo_url: String?,
    val security_metadata: SecurityMetadata,
    val contact_info: ContactInfo,
    val validated: Boolean,
    val priority: String? = null
)

@Serializable
data class ReportesPaginados(
    val data: List<ReporteCasoResponse>,
    val total: Long,
    val page: Int,
    val limit: Int,
    val hasMore: Boolean
)
