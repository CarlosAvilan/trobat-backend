package com.trobatapp.routes

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import com.trobatapp.casos
import com.trobatapp.models.*
import com.trobatapp.reportes
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
                        .find(Filters.eq("caso_id", ObjectId(id)))
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

                if (!ObjectId.isValid(req.caso_id))
                    return@post call.respond(HttpStatusCode.BadRequest, MensajeResponse("caso_id inválido"))

                val casoExiste = casos.find(Filters.eq("_id", ObjectId(req.caso_id))).firstOrNull()
                if (casoExiste == null)
                    return@post call.respond(HttpStatusCode.NotFound, MensajeResponse("Caso no encontrado"))

                val locationDoc = Document("type", req.location.type)
                    .append("coordinates", req.location.coordinates)
                val metaDoc = Document("anonimo", req.metadata_seguridad.anonimo)
                val contactDoc = Document()
                    .append("nombre", req.datos_contacto.nombre)
                    .append("telefono", req.datos_contacto.telefono)
                    .append("email", req.datos_contacto.email)

                val reporteDoc = Document("caso_id", ObjectId(req.caso_id))
                    .append("location", locationDoc)
                    .append("timestamp", Date.from(Instant.now()))
                    .append("prioridad_policial", req.prioridad_policial)
                    .append("descripcion", req.descripcion)
                    .append("photo_url", req.photo_url)
                    .append("metadata_seguridad", metaDoc)
                    .append("datos_contacto", contactDoc)
                    .append("validado", false)

                reportes.insertOne(reporteDoc)
                casos.updateOne(
                    Filters.eq("_id", ObjectId(req.caso_id)),
                    Updates.inc("total_reportes", 1)
                )

                val newId = reporteDoc.getObjectId("_id").toHexString()
                call.respond(HttpStatusCode.Created, CrearCasoResponse(id = newId, mensaje = "Reporte creado exitosamente"))
            }

            // --- AUTENTICADO: leer reportes ---
            authenticate("auth-jwt") {

                get {
                    val casoId = call.request.queryParameters["caso_id"]
                    val filtro = if (casoId != null && ObjectId.isValid(casoId))
                        Filters.eq("caso_id", ObjectId(casoId))
                    else
                        Document()

                    try {
                        val lista = reportes.find(filtro).toList().map { it.toReporteCasoResponse() }
                        call.respond(lista)
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
                        Updates.set("validado", req.validado)
                    )

                    if (result.matchedCount == 0L) call.respond(HttpStatusCode.NotFound, MensajeResponse("Reporte no encontrado"))
                    else {
                        val estado = if (req.validado) "validado" else "invalidado"
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
    val metaDoc = get("metadata_seguridad", Document::class.java) ?: Document()
    val contactDoc = get("datos_contacto", Document::class.java) ?: Document()

    val casoId = try {
        getObjectId("caso_id").toHexString()
    } catch (e: Exception) {
        getString("caso_id") ?: ""
    }

    return ReporteCasoResponse(
        id = getObjectId("_id").toHexString(),
        caso_id = casoId,
        location = Ubicacion(
            type = locDoc.getString("type") ?: "Point",
            coordinates = coords.map { it.toDouble() }
        ),
        timestamp = getDate("timestamp")?.toInstant()?.toString()
            ?: get("timestamp")?.toString()
            ?: "",
        prioridad_policial = getBoolean("prioridad_policial") ?: false,
        descripcion = getString("descripcion") ?: "",
        photo_url = getString("photo_url"),
        metadata_seguridad = MetadataSeguridad(anonimo = metaDoc.getBoolean("anonimo") ?: true),
        datos_contacto = DatosContacto(
            nombre = contactDoc.getString("nombre"),
            telefono = contactDoc.getString("telefono"),
            email = contactDoc.getString("email")
        ),
        validado = getBoolean("validado") ?: false
    )
}
