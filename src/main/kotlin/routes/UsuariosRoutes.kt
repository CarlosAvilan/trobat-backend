package com.trobatapp.routes

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import com.trobatapp.models.*
import com.trobatapp.oficiales
import com.trobatapp.usuarios
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

fun Application.configureUsuariosRouting() {
    routing {

        // --- AUTENTICADO: perfil y FCM de ciudadano ---
        authenticate("auth-jwt") {

            route("/usuarios") {

                get("/{id}") {
                    val id = call.parameters["id"]
                        ?: return@get call.respond(HttpStatusCode.BadRequest, MensajeResponse("ID requerido"))
                    if (!ObjectId.isValid(id))
                        return@get call.respond(HttpStatusCode.BadRequest, MensajeResponse("ID inválido"))

                    try {
                        val userDoc = usuarios.find(Filters.eq("_id", ObjectId(id))).firstOrNull()
                            ?: return@get call.respond(HttpStatusCode.NotFound, MensajeResponse("Usuario no encontrado"))
                        call.respond(userDoc.toUsuarioResponse())
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.InternalServerError, MensajeResponse(e.localizedMessage ?: "Error interno"))
                    }
                }

                post("/{id}/fcm-token") {
                    val id = call.parameters["id"]
                        ?: return@post call.respond(HttpStatusCode.BadRequest, MensajeResponse("ID requerido"))
                    if (!ObjectId.isValid(id))
                        return@post call.respond(HttpStatusCode.BadRequest, MensajeResponse("ID inválido"))

                    val req = try {
                        call.receive<AgregarFcmTokenRequest>()
                    } catch (e: Exception) {
                        return@post call.respond(HttpStatusCode.BadRequest, MensajeResponse("Cuerpo inválido"))
                    }

                    val result = usuarios.updateOne(
                        Filters.eq("_id", ObjectId(id)),
                        Updates.addToSet("fcm_tokens", req.fcm_token)
                    )
                    if (result.matchedCount == 0L) call.respond(HttpStatusCode.NotFound, MensajeResponse("Usuario no encontrado"))
                    else call.respond(MensajeResponse("Token FCM registrado"))
                }
            }

            // --- SOLO OFICIAL: gestión de oficiales ---
            route("/oficiales") {

                get {
                    if (!call.verificarRol("oficial")) return@get
                    try {
                        val lista = oficiales.find(Filters.exists("email_institucional")).toList().map { it.toOficialResponse() }
                        call.respond(lista)
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.InternalServerError, MensajeResponse(e.localizedMessage ?: "Error interno"))
                    }
                }

                get("/{id}") {
                    if (!call.verificarRol("oficial")) return@get

                    val id = call.parameters["id"]
                        ?: return@get call.respond(HttpStatusCode.BadRequest, MensajeResponse("ID requerido"))
                    if (!ObjectId.isValid(id))
                        return@get call.respond(HttpStatusCode.BadRequest, MensajeResponse("ID inválido"))

                    try {
                        val oficialDoc = oficiales.find(Filters.eq("_id", ObjectId(id))).firstOrNull()
                            ?: return@get call.respond(HttpStatusCode.NotFound, MensajeResponse("Oficial no encontrado"))
                        call.respond(oficialDoc.toOficialResponse())
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.InternalServerError, MensajeResponse(e.localizedMessage ?: "Error interno"))
                    }
                }

                post("/{id}/fcm-token") {
                    if (!call.verificarRol("oficial")) return@post

                    val id = call.parameters["id"]
                        ?: return@post call.respond(HttpStatusCode.BadRequest, MensajeResponse("ID requerido"))
                    if (!ObjectId.isValid(id))
                        return@post call.respond(HttpStatusCode.BadRequest, MensajeResponse("ID inválido"))

                    val req = try {
                        call.receive<AgregarFcmTokenRequest>()
                    } catch (e: Exception) {
                        return@post call.respond(HttpStatusCode.BadRequest, MensajeResponse("Cuerpo inválido"))
                    }

                    val result = oficiales.updateOne(
                        Filters.eq("_id", ObjectId(id)),
                        Updates.addToSet("fcm_tokens", req.fcm_token)
                    )
                    if (result.matchedCount == 0L) call.respond(HttpStatusCode.NotFound, MensajeResponse("Oficial no encontrado"))
                    else call.respond(MensajeResponse("Token FCM registrado"))
                }
            }
        }
    }
}

private fun Document.toUsuarioResponse(): UsuarioResponse =
    UsuarioResponse(
        id = getObjectId("_id").toHexString(),
        name = getString("name") ?: "",
        email = getString("email") ?: "",
        role = getString("role") ?: "user",
        is_verified = getBoolean("is_verified") ?: false,
        created_at = getString("created_at") ?: ""
    )

private fun Document.toOficialResponse(): OficialResponse =
    OficialResponse(
        id = getObjectId("_id").toHexString(),
        nombre = getString("nombre") ?: "",
        email_institucional = getString("email_institucional") ?: "",
        rango = getString("rango") ?: "",
        legajo = getString("legajo") ?: ""
    )
