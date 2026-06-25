package com.trobatapp.routes

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import com.mongodb.client.model.Updates
import com.trobatapp.models.*
import com.trobatapp.notificaciones
import com.trobatapp.service.FcmService
import com.trobatapp.utils.verificarRol
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import org.bson.Document
import org.bson.types.ObjectId
import java.time.Instant
import java.util.Date

fun Application.configureNotificacionesRouting() {
    routing {
        authenticate("auth-jwt") {
            route("/notificaciones") {

                post {
                    if (!call.verificarRol("oficial")) return@post

                    val req = try {
                        call.receive<CrearNotificacionRequest>()
                    } catch (e: Exception) {
                        return@post call.respond(HttpStatusCode.BadRequest, MensajeResponse("Cuerpo inválido"))
                    }

                    if (req.titulo.isBlank())
                        return@post call.respond(HttpStatusCode.BadRequest, MensajeResponse("El título es obligatorio"))
                    if (req.descripcion.isBlank())
                        return@post call.respond(HttpStatusCode.BadRequest, MensajeResponse("La descripción es obligatoria"))

                    val oficialId = call.principal<JWTPrincipal>()
                        ?.payload?.getClaim("id")?.asString() ?: ""

                    val createdAt = Date.from(Instant.now())
                    val doc = Document("oficial_id", oficialId)
                        .append("titulo", req.titulo)
                        .append("descripcion", req.descripcion)
                        .append("created_at", createdAt)
                        .append("fcm_status", "pending")

                    notificaciones.insertOne(doc)
                    val docId = doc.getObjectId("_id")

                    val (fcmStatus, sentAt, messageId) = try {
                        val msgId = withContext(Dispatchers.IO) { FcmService.sendToTopic(req.titulo, req.descripcion) }
                        val now = Date.from(Instant.now())
                        notificaciones.updateOne(
                            Filters.eq("_id", docId),
                            Updates.combine(
                                Updates.set("sent_at", now),
                                Updates.set("fcm_status", "success"),
                                Updates.set("fcm_message_id", msgId)
                            )
                        )
                        Triple("success", now.toInstant().toString(), msgId)
                    } catch (e: Exception) {
                        notificaciones.updateOne(
                            Filters.eq("_id", docId),
                            Updates.set("fcm_status", "error")
                        )
                        Triple("error", null, null)
                    }

                    call.respond(
                        HttpStatusCode.Created,
                        NotificacionLogResponse(
                            id = docId.toHexString(),
                            oficial_id = oficialId,
                            titulo = req.titulo,
                            descripcion = req.descripcion,
                            created_at = createdAt.toInstant().toString(),
                            sent_at = sentAt,
                            fcm_status = fcmStatus,
                            fcm_message_id = messageId
                        )
                    )
                }

                get {
                    if (!call.verificarRol("oficial")) return@get

                    val page = call.request.queryParameters["page"]?.toIntOrNull()?.coerceAtLeast(0) ?: 0
                    val limit = call.request.queryParameters["limit"]?.toIntOrNull()?.coerceIn(1, 100) ?: 20

                    val total = notificaciones.countDocuments()
                    val lista = notificaciones.find()
                        .sort(Sorts.descending("created_at"))
                        .skip(page * limit)
                        .limit(limit)
                        .toList()
                        .map { it.toNotificacionLogResponse() }

                    call.respond(NotificacionesPaginadas(data = lista, total = total, page = page, limit = limit))
                }
            }
        }
    }
}

private fun Document.toNotificacionLogResponse() = NotificacionLogResponse(
    id = getObjectId("_id").toHexString(),
    oficial_id = getString("oficial_id") ?: "",
    titulo = getString("titulo") ?: "",
    descripcion = getString("descripcion") ?: "",
    created_at = getDate("created_at")?.toInstant()?.toString() ?: "",
    sent_at = getDate("sent_at")?.toInstant()?.toString(),
    fcm_status = getString("fcm_status") ?: "unknown",
    fcm_message_id = getString("fcm_message_id")
)
