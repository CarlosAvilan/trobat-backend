package com.trobatapp.models

import kotlinx.serialization.Serializable

@Serializable
data class MetadataSeguridad(
    val anonimo: Boolean = true
)

@Serializable
data class DatosContacto(
    val nombre: String? = null,
    val telefono: String? = null,
    val email: String? = null
)

@Serializable
data class CrearReporteRequest(
    val caso_id: String,
    val location: Ubicacion,
    val descripcion: String,
    val photo_url: String? = null,
    val prioridad_policial: Boolean = false,
    val metadata_seguridad: MetadataSeguridad = MetadataSeguridad(),
    val datos_contacto: DatosContacto = DatosContacto()
)

@Serializable
data class ValidarReporteRequest(
    val validado: Boolean
)

@Serializable
data class ReporteCasoResponse(
    val id: String,
    val caso_id: String,
    val location: Ubicacion,
    val timestamp: String,
    val prioridad_policial: Boolean,
    val descripcion: String,
    val photo_url: String?,
    val metadata_seguridad: MetadataSeguridad,
    val datos_contacto: DatosContacto,
    val validado: Boolean
)
