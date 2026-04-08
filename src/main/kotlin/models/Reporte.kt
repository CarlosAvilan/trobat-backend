package com.trobatapp.models

import kotlinx.serialization.Serializable
import com.trobatapp.models.Ubicacion

@Serializable
data class Reporte(
    val id_solicitud: String = "",
    val url_foto: String = "",
    val estado: String = "activo",
    val ubicacion: Ubicacion? = null,
    val descripcion: String? = null,
    val fecha: String? = null
)