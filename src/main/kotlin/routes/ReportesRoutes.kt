package com.trobatapp.routes

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import com.trobatapp.casos
import com.trobatapp.models.*
import com.trobatapp.reportes
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

fun Application.configureReportesRouting() {
    routing {

        // --- PÚBLICO: reportes de un caso específico ---
        route("/casos") {
            get("/{id}/reportes") {
                val id = call.parameters["id"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, MensajeResponse("ID requerido"))
                if (!ObjectId.isValid(id))
                    return@get call.respond(HttpStatusCode.BadRequest, MensajeResponse("ID inválido"))

                try {
                    val lista = reportes
                        .find(Filters.eq("case_id", ObjectId(id)))
                        .toList()
                        .map { it.toReporteCasoResponse() }
                    call.respond(lista)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, MensajeResponse(e.localizedMessage ?: "Error interno"))
                }
            }
        }

        route("/reportes") {

            // --- PÚBLICO: crear reporte (avistamiento anónimo) ---
            post {
                val req = try {
                    call.receive<CrearReporteRequest>()
                } catch (e: Exception) {
                    return@post call.respond(HttpStatusCode.BadRequest, MensajeResponse("Cuerpo inválido: ${e.localizedMessage}"))
                }

                if (!ObjectId.isValid(req.case_id))
                    return@post call.respond(HttpStatusCode.BadRequest, MensajeResponse("case_id inválido"))

                val casoExiste = casos.find(Filters.eq("_id", ObjectId(req.case_id))).firstOrNull()
                if (casoExiste == null)
                    return@post call.respond(HttpStatusCode.NotFound, MensajeResponse("Caso no encontrado"))

                val locationDoc = Document("type", req.location.type)
                    .append("coordinates", req.location.coordinates)
                val securityDoc = Document("anonymous", req.security_metadata.anonymous)
                val contactDoc = Document()
                    .append("name", req.contact_info.name)
                    .append("phone", req.contact_info.phone)
                    .append("email", req.contact_info.email)

                val locationLabel = GeocodingUtil.reverseGeocode(
                    lat = req.location.latitud,
                    lon = req.location.longitud
                )

                val reporteDoc = Document("case_id", ObjectId(req.case_id))
                    .append("location", locationDoc)
                    .append("location_label", locationLabel)
                    .append("timestamp", Date.from(Instant.now()))
                    .append("description", req.description)
                    .append("photo_url", req.photo_url)
                    .append("security_metadata", securityDoc)
                    .append("contact_info", contactDoc)
                    .append("validated", false)

                reportes.insertOne(reporteDoc)
                casos.updateOne(
                    Filters.eq("_id", ObjectId(req.case_id)),
                    Updates.inc("total_reports", 1)
                )

                val newId = reporteDoc.getObjectId("_id").toHexString()
                call.respond(HttpStatusCode.Created, CrearCasoResponse(id = newId, mensaje = "Reporte creado exitosamente"))
            }

            // --- AUTENTICADO: leer reportes ---
            authenticate("auth-jwt") {

                get {
                    val caseId = call.request.queryParameters["case_id"]
                    val page = call.request.queryParameters["page"]?.toIntOrNull()?.coerceAtLeast(0) ?: 0
                    val limit = call.request.queryParameters["limit"]?.toIntOrNull()?.coerceIn(1, 100) ?: 20
                    val filtro = if (caseId != null && ObjectId.isValid(caseId))
                        Filters.eq("case_id", ObjectId(caseId))
                    else
                        Document()

                    try {
                        val total = reportes.countDocuments(filtro)
                        val lista = reportes.find(filtro)
                            .skip(page * limit)
                            .limit(limit)
                            .toList()
                            .map { it.toReporteCasoResponse() }
                        call.respond(ReportesPaginados(
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
                        val reporte = reportes.find(Filters.eq("_id", ObjectId(id))).firstOrNull()
                            ?: return@get call.respond(HttpStatusCode.NotFound, MensajeResponse("Reporte no encontrado"))
                        call.respond(reporte.toReporteCasoResponse())
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.InternalServerError, MensajeResponse(e.localizedMessage ?: "Error interno"))
                    }
                }

                // --- SOLO OFICIAL: validar reporte ---
                patch("/{id}/validar") {
                    if (!call.verificarRol("oficial")) return@patch

                    val id = call.parameters["id"]
                        ?: return@patch call.respond(HttpStatusCode.BadRequest, MensajeResponse("ID requerido"))
                    if (!ObjectId.isValid(id))
                        return@patch call.respond(HttpStatusCode.BadRequest, MensajeResponse("ID inválido"))

                    val req = try {
                        call.receive<ValidarReporteRequest>()
                    } catch (e: Exception) {
                        return@patch call.respond(HttpStatusCode.BadRequest, MensajeResponse("Cuerpo inválido"))
                    }

                    val result = reportes.updateOne(
                        Filters.eq("_id", ObjectId(id)),
                        Updates.set("validated", req.validated)
                    )

                    if (result.matchedCount == 0L) call.respond(HttpStatusCode.NotFound, MensajeResponse("Reporte no encontrado"))
                    else {
                        val estado = if (req.validated) "validado" else "invalidado"
                        call.respond(MensajeResponse("Reporte $estado exitosamente"))
                    }
                }
            }
        }
    }
}

private fun Document.toReporteCasoResponse(): ReporteCasoResponse {
    val locDoc = get("location", Document::class.java) ?: Document()
    val coords = locDoc.getList("coordinates", Number::class.java) ?: emptyList()
    val securityDoc = get("security_metadata", Document::class.java) ?: Document()
    val contactDoc = get("contact_info", Document::class.java) ?: Document()

    val caseId = try {
        getObjectId("case_id").toHexString()
    } catch (e: Exception) {
        getString("case_id") ?: ""
    }

    return ReporteCasoResponse(
        id = getObjectId("_id").toHexString(),
        case_id = caseId,
        location = Ubicacion(
            type = locDoc.getString("type") ?: "Point",
            coordinates = coords.map { it.toDouble() }
        ),
        location_label = getString("location_label"),
        timestamp = getDate("timestamp")?.toInstant()?.toString()
            ?: get("timestamp")?.toString()
            ?: "",
        description = getString("description") ?: "",
        photo_url = getString("photo_url"),
        security_metadata = SecurityMetadata(anonymous = securityDoc.getBoolean("anonymous") ?: true),
        contact_info = ContactInfo(
            name = contactDoc.getString("name"),
            phone = contactDoc.getString("phone"),
            email = contactDoc.getString("email")
        ),
        validated = getBoolean("validated") ?: false
    )
}
