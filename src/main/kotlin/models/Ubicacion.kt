package com.trobatapp.models

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