
package com.trobatapp.models

import kotlinx.serialization.Serializable

@Serializable
data class Desaparecido(
    val nombre: String = "",
    val descripcion: String = "",
    val ubicacion_original: Ubicacion? = null,
    val ultima_ubicacion_oficial: Ubicacion? = null
)

@Serializable
data class RepresentanteExterno(
    val nombre: String = "",
    val email: String = "",
    val telefono: String = ""
)

@Serializable
data class CrearCasoRequest(
    val oficial_administrador_id: String,
    val agentes_asignados: List<String> = emptyList(),
    val desaparecido: Desaparecido,
    val representante_externo: RepresentanteExterno
)

@Serializable
data class ActualizarEstadoRequest(
    val estado: String
)

@Serializable
data class CasoResponse(
    val id: String,
    val oficial_administrador_id: String,
    val agentes_asignados: List<String>,
    val desaparecido: Desaparecido,
    val representante_externo: RepresentanteExterno,
    val estado: String,
    val total_reportes: Int,
    val fecha_creacion: String
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
