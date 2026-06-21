package com.trobatapp.routes

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import com.trobatapp.casos
import com.trobatapp.models.*
import com.trobatapp.utils.GeocodingUtil
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

            get("/cercanos") {
                val lat = call.request.queryParameters["lat"]?.toDoubleOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest, MensajeResponse("lat requerido"))
                val lng = call.request.queryParameters["lng"]?.toDoubleOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest, MensajeResponse("lng requerido"))
                val radiusKm = call.request.queryParameters["radio"]?.toDoubleOrNull()?.coerceIn(1.0, 50.0) ?: 5.0
                val page  = call.request.queryParameters["page"]?.toIntOrNull()?.coerceAtLeast(0) ?: 0
                val limit = call.request.queryParameters["limit"]?.toIntOrNull()?.coerceIn(1, 100) ?: 20

                try {
                    val geoNearStage = Document("\$geoNear", Document()
                        .append("near", Document("type", "Point").append("coordinates", listOf(lng, lat)))
                        .append("distanceField", "distance_meters")
                        .append("maxDistance", radiusKm * 1000)
                        .append("spherical", true)
                        .append("key", "missing_person.last_known_location")
                    )

                    val total = casos.aggregate(listOf(
                        geoNearStage,
                        Document("\$count", "total")
                    )).firstOrNull()?.getInteger("total")?.toLong() ?: 0L

                    val lista = casos.aggregate(listOf(
                        geoNearStage,
                        Document("\$skip", page * limit),
                        Document("\$limit", limit)
                    )).toList().map { doc ->
                        val distanceKm = (doc["distance_meters"] as? Number)?.toDouble()?.div(1000.0) ?: 0.0
                        CasoCercanoResponse(caso = doc.toCasoResponse(), distance_km = distanceKm)
                    }

                    call.respond(CasosCercanosPaginados(
                        data = lista,
                        total = total,
                        page = page,
                        limit = limit,
                        radius_km = radiusKm,
                        hasMore = (page * limit + lista.size).toLong() < total
                    ))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, MensajeResponse(e.localizedMessage ?: "Error interno"))
                }
            }

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

                    if (!ObjectId.isValid(req.admin_officer_id))
                        return@post call.respond(HttpStatusCode.BadRequest, MensajeResponse("admin_officer_id inválido"))

                    val agentesInvalidos = req.assigned_agents.filter { !ObjectId.isValid(it) }
                    if (agentesInvalidos.isNotEmpty())
                        return@post call.respond(HttpStatusCode.BadRequest, MensajeResponse("IDs de agentes inválidos: $agentesInvalidos"))

                    val locationLabel = req.missing_person.last_known_location?.let { ub ->
                        GeocodingUtil.reverseGeocode(lat = ub.latitud, lon = ub.longitud)
                    }

                    val missingPersonDoc = Document("name", req.missing_person.name)
                        .append("description", req.missing_person.description)
                        .append("age", req.missing_person.age)
                        .append("image", req.missing_person.image)
                        .append("last_seen_date", req.missing_person.last_seen_date)
                        .append("location_description", req.missing_person.location_description)
                        .append("location_label", locationLabel)
                    req.missing_person.last_known_location?.let { ub ->
                        missingPersonDoc.append(
                            "last_known_location",
                            Document("type", ub.type).append("coordinates", ub.coordinates)
                        )
                    }

                    val contactDoc = Document("name", req.external_contact.name)
                        .append("email", req.external_contact.email)
                        .append("phone", req.external_contact.phone)

                    val casoDoc = Document("admin_officer_id", ObjectId(req.admin_officer_id))
                        .append("assigned_agents", req.assigned_agents.map { ObjectId(it) })
                        .append("missing_person", missingPersonDoc)
                        .append("external_contact", contactDoc)
                        .append("status", "active_investigation")
                        .append("total_reports", 0)
                        .append("created_at", Date.from(Instant.now()))

                    casos.insertOne(casoDoc)
                    val newId = casoDoc.getObjectId("_id").toHexString()
                    call.respond(HttpStatusCode.Created, CrearCasoResponse(id = newId, message = "Caso creado exitosamente"))
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

                    val validStatuses = setOf("active_investigation", "resolved", "closed")
                    if (req.status !in validStatuses)
                        return@patch call.respond(
                            HttpStatusCode.BadRequest,
                            MensajeResponse("Estado inválido. Opciones: $validStatuses")
                        )

                    val result = casos.updateOne(
                        Filters.eq("_id", ObjectId(id)),
                        Updates.set("status", req.status)
                    )
                    if (result.matchedCount == 0L) call.respond(HttpStatusCode.NotFound, MensajeResponse("Caso no encontrado"))
                    else call.respond(MensajeResponse("Estado actualizado a ${req.status}"))
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
                        Updates.addToSet("assigned_agents", ObjectId(agenteId))
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
                        Updates.pull("assigned_agents", ObjectId(agenteId))
                    )
                    if (result.matchedCount == 0L) call.respond(HttpStatusCode.NotFound, MensajeResponse("Caso no encontrado"))
                    else call.respond(MensajeResponse("Agente removido del caso"))
                }
            }
        }
    }
}

private fun Document.toCasoResponse(): CasoResponse {
    val missingDoc = get("missing_person", Document::class.java) ?: Document()
    val contactDoc = get("external_contact", Document::class.java) ?: Document()
    val locDoc = missingDoc.get("last_known_location", Document::class.java)

    val location = locDoc?.let {
        val coords = it.getList("coordinates", Number::class.java) ?: emptyList()
        Ubicacion(type = it.getString("type") ?: "Point", coordinates = coords.map { n -> n.toDouble() })
    }

    val assignedAgents = try {
        getList("assigned_agents", ObjectId::class.java)?.map { it.toHexString() } ?: emptyList()
    } catch (e: Exception) {
        getList("assigned_agents", String::class.java) ?: emptyList()
    }

    val adminOfficerId = try {
        getObjectId("admin_officer_id").toHexString()
    } catch (e: Exception) {
        getString("admin_officer_id") ?: ""
    }

    return CasoResponse(
        id = getObjectId("_id").toHexString(),
        admin_officer_id = adminOfficerId,
        assigned_agents = assignedAgents,
        missing_person = Desaparecido(
            name = missingDoc.getString("name") ?: "",
            description = missingDoc.getString("description") ?: "",
            age = missingDoc.getInteger("age") ?: 0,
            image = missingDoc.getString("image") ?: "",
            last_seen_date = missingDoc.getString("last_seen_date") ?: "",
            location_description = missingDoc.getString("location_description") ?: "",
            last_known_location = location,
            location_label = missingDoc.getString("location_label")
        ),
        external_contact = RepresentanteExterno(
            name = contactDoc.getString("name") ?: "",
            email = contactDoc.getString("email") ?: "",
            phone = contactDoc.getString("phone") ?: ""
        ),
        status = getString("status") ?: "active_investigation",
        total_reports = getInteger("total_reports") ?: 0,
        created_at = getDate("created_at")?.toInstant()?.toString()
            ?: get("created_at")?.toString()
            ?: ""
    )
}
