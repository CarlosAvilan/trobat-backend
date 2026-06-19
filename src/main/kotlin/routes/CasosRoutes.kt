package com.trobatapp.routes

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import com.trobatapp.casos
import com.trobatapp.models.*
import com.trobatapp.utils.verificarRol
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import org.bson.Document
import org.bson.types.ObjectId
import java.time.Instant
import java.util.Date

fun Application.configureCasosRouting() {
    routing {
        route("/casos") {

            // --- PÚBLICO ---

            get {
                val page = call.request.queryParameters["page"]?.toIntOrNull()?.coerceAtLeast(0) ?: 0
                val limit = call.request.queryParameters["limit"]?.toIntOrNull()?.coerceIn(1, 100) ?: 20
                try {
                    val total = casos.countDocuments()
                    val lista = casos.find()
                        .skip(page * limit)
                        .limit(limit)
                        .toList()
                        .map { it.toCasoResponse() }
                    call.respond(CasosPaginados(
                        data = lista,
                        total = total,
                        page = page,
                        limit = limit,
                        hasMore = (page * limit + lista.size).toLong() < total
                    ))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, MensajeResponse(e.localizedMessage ?: "Error interno"))
                }
            }

            get("/{id}") {
                val id = call.parameters["id"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, MensajeResponse("ID requerido"))
                if (!ObjectId.isValid(id))
                    return@get call.respond(HttpStatusCode.BadRequest, MensajeResponse("ID inválido"))

                try {
                    val caso = casos.find(Filters.eq("_id", ObjectId(id))).firstOrNull()
                        ?: return@get call.respond(HttpStatusCode.NotFound, MensajeResponse("Caso no encontrado"))
                    call.respond(caso.toCasoResponse())
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, MensajeResponse(e.localizedMessage ?: "Error interno"))
                }
            }

            // --- SOLO OFICIAL ---

            authenticate("auth-jwt") {

                post {
                    if (!call.verificarRol("oficial")) return@post

                    val req = try {
                        call.receive<CrearCasoRequest>()
                    } catch (e: Exception) {
                        return@post call.respond(HttpStatusCode.BadRequest, MensajeResponse("Cuerpo inválido: ${e.localizedMessage}"))
                    }

                    if (!ObjectId.isValid(req.oficial_administrador_id))
                        return@post call.respond(HttpStatusCode.BadRequest, MensajeResponse("oficial_administrador_id inválido"))

                    val agentesInvalidos = req.agentes_asignados.filter { !ObjectId.isValid(it) }
                    if (agentesInvalidos.isNotEmpty())
                        return@post call.respond(HttpStatusCode.BadRequest, MensajeResponse("IDs de agentes inválidos: $agentesInvalidos"))

                    val desaparecidoDoc = Document("nombre", req.desaparecido.nombre)
                        .append("descripcion", req.desaparecido.descripcion)
                        req.desaparecido.ubicacion_original?.let { ub ->
                            desaparecidoDoc.append(
                                "ubicacion_original",
                                Document("type", ub.type).append("coordinates", ub.coordinates)
                            )
                        }
                        req.desaparecido.ultima_ubicacion_oficial?.let { ub ->
                            desaparecidoDoc.append(
                                "ultima_ubicacion_oficial",
                                Document("type", ub.type).append("coordinates", ub.coordinates)
                            )
                        }

                    val repDoc = Document("nombre", req.representante_externo.nombre)
                        .append("email", req.representante_externo.email)
                        .append("telefono", req.representante_externo.telefono)

                    val casoDoc = Document("oficial_administrador_id", ObjectId(req.oficial_administrador_id))
                        .append("agentes_asignados", req.agentes_asignados.map { ObjectId(it) })
                        .append("desaparecido", desaparecidoDoc)
                        .append("representante_externo", repDoc)
                        .append("estado", "investigacion_activa")
                        .append("total_reportes", 0)
                        .append("fecha_creacion", Date.from(Instant.now()))

                    casos.insertOne(casoDoc)
                    val newId = casoDoc.getObjectId("_id").toHexString()
                    call.respond(HttpStatusCode.Created, CrearCasoResponse(id = newId, mensaje = "Caso creado exitosamente"))
                }

                patch("/{id}/estado") {
                    if (!call.verificarRol("oficial")) return@patch

                    val id = call.parameters["id"]
                        ?: return@patch call.respond(HttpStatusCode.BadRequest, MensajeResponse("ID requerido"))
                    if (!ObjectId.isValid(id))
                        return@patch call.respond(HttpStatusCode.BadRequest, MensajeResponse("ID inválido"))

                    val req = try {
                        call.receive<ActualizarEstadoRequest>()
                    } catch (e: Exception) {
                        return@patch call.respond(HttpStatusCode.BadRequest, MensajeResponse("Cuerpo inválido"))
                    }

                    val estadosValidos = setOf("investigacion_activa", "cerrado", "suspendido")
                    if (req.estado !in estadosValidos)
                        return@patch call.respond(
                            HttpStatusCode.BadRequest,
                            MensajeResponse("Estado inválido. Opciones: $estadosValidos")
                        )

                    val result = casos.updateOne(
                        Filters.eq("_id", ObjectId(id)),
                        Updates.set("estado", req.estado)
                    )
                    if (result.matchedCount == 0L) call.respond(HttpStatusCode.NotFound, MensajeResponse("Caso no encontrado"))
                    else call.respond(MensajeResponse("Estado actualizado a ${req.estado}"))
                }

                post("/{id}/agentes/{agenteId}") {
                    if (!call.verificarRol("oficial")) return@post

                    val id = call.parameters["id"]
                        ?: return@post call.respond(HttpStatusCode.BadRequest, MensajeResponse("ID requerido"))
                    val agenteId = call.parameters["agenteId"]
                        ?: return@post call.respond(HttpStatusCode.BadRequest, MensajeResponse("ID de agente requerido"))
                    if (!ObjectId.isValid(id) || !ObjectId.isValid(agenteId))
                        return@post call.respond(HttpStatusCode.BadRequest, MensajeResponse("ID inválido"))

                    val result = casos.updateOne(
                        Filters.eq("_id", ObjectId(id)),
                        Updates.addToSet("agentes_asignados", ObjectId(agenteId))
                    )
                    if (result.matchedCount == 0L) call.respond(HttpStatusCode.NotFound, MensajeResponse("Caso no encontrado"))
                    else call.respond(MensajeResponse("Agente agregado al caso"))
                }

                delete("/{id}/agentes/{agenteId}") {
                    if (!call.verificarRol("oficial")) return@delete

                    val id = call.parameters["id"]
                        ?: return@delete call.respond(HttpStatusCode.BadRequest, MensajeResponse("ID requerido"))
                    val agenteId = call.parameters["agenteId"]
                        ?: return@delete call.respond(HttpStatusCode.BadRequest, MensajeResponse("ID de agente requerido"))
                    if (!ObjectId.isValid(id) || !ObjectId.isValid(agenteId))
                        return@delete call.respond(HttpStatusCode.BadRequest, MensajeResponse("ID inválido"))

                    val result = casos.updateOne(
                        Filters.eq("_id", ObjectId(id)),
                        Updates.pull("agentes_asignados", ObjectId(agenteId))
                    )
                    if (result.matchedCount == 0L) call.respond(HttpStatusCode.NotFound, MensajeResponse("Caso no encontrado"))
                    else call.respond(MensajeResponse("Agente removido del caso"))
                }
            }
        }
    }
}

private fun Document.toCasoResponse(): CasoResponse {
    val desDoc = get("desaparecido", Document::class.java) ?: Document()
    val repDoc = get("representante_externo", Document::class.java) ?: Document()
    val ubicacionOriginalDoc = desDoc.get("ubicacion_original", Document::class.java)
    val ubicacionOficialDoc = desDoc.get("ultima_ubicacion_oficial", Document::class.java)

    val ubicacionOriginal = ubicacionOriginalDoc?.let {
        val coords = it.getList("coordinates", Number::class.java) ?: emptyList()
        Ubicacion(type = it.getString("type") ?: "Point", coordinates = coords.map { n -> n.toDouble() })
    }
    val ubicacionOficial = ubicacionOficialDoc?.let {
        val coords = it.getList("coordinates", Number::class.java) ?: emptyList()
        Ubicacion(type = it.getString("type") ?: "Point", coordinates = coords.map { n -> n.toDouble() })
    }

    val agentes = try {
        getList("agentes_asignados", ObjectId::class.java)?.map { it.toHexString() } ?: emptyList()
    } catch (e: Exception) {
        getList("agentes_asignados", String::class.java) ?: emptyList()
    }

    val oficialId = try {
        getObjectId("oficial_administrador_id").toHexString()
    } catch (e: Exception) {
        getString("oficial_administrador_id") ?: ""
    }

    return CasoResponse(
        id = getObjectId("_id").toHexString(),
        oficial_administrador_id = oficialId,
        agentes_asignados = agentes,
        desaparecido = Desaparecido(
            nombre = desDoc.getString("nombre") ?: "",
            descripcion = desDoc.getString("descripcion") ?: "",
            ubicacion_original = ubicacionOriginal,
            ultima_ubicacion_oficial = ubicacionOficial
        ),
        representante_externo = RepresentanteExterno(
            nombre = repDoc.getString("nombre") ?: "",
            email = repDoc.getString("email") ?: "",
            telefono = repDoc.getString("telefono") ?: ""
        ),
        estado = getString("estado") ?: "investigacion_activa",
        total_reportes = getInteger("total_reportes") ?: 0,
        fecha_creacion = getDate("fecha_creacion")?.toInstant()?.toString()
            ?: get("fecha_creacion")?.toString()
            ?: ""
    )
}
