package com.trobatapp.models

import kotlinx.serialization.Serializable

@Serializable
data class ReporteRespuesta(
    val id: String,
    val lat: Double,
    val lng: Double,
    val ubicacion_label: String? = null,
    val descripcion: String?,
    val imagen: String,
    val distancia_km: Double
)