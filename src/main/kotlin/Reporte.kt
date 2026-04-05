package com.trobatapp

import kotlinx.serialization.Serializable

@Serializable
data class Ubicacion(
    val type: String = "Point",
    val coordinates: List<Double> = emptyList()
) {
    val latitud: Double
        get() = coordinates.getOrNull(1) ?: 0.0

    val longitud: Double
        get() = coordinates.getOrNull(0) ?: 0.0
}

@Serializable
data class Reporte(
    val id_solicitud: String = "",
    val url_foto: String = "",
    val estado: String = "activo",
    val ubicacion: Ubicacion? = null,
    val descripcion: String? = null,
    val fecha: String? = null
)