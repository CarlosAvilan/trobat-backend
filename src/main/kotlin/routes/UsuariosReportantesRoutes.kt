package com.trobatapp.routes

import com.mongodb.client.model.Filters
import com.trobatapp.models.*
import com.trobatapp.usuariosReportantes
import com.trobatapp.utils.JwtUtil
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.flow.firstOrNull
import org.bson.Document
import org.bson.types.ObjectId
import org.mindrot.jbcrypt.BCrypt
import java.time.Instant

fun Application.configureUsuariosReportantesRouting() {
    routing {
        route("/usuarios-reportantes") {

            post("/registro") {
                val req = try {
                    call.receive<RegistroReportanteRequest>()
                } catch (e: Exception) {
                    return@post call.respond(HttpStatusCode.BadRequest, MensajeResponse("Cuerpo inválido: ${e.localizedMessage}"))
                }

                if (req.email.isBlank() || req.password.isBlank())
                    return@post call.respond(HttpStatusCode.BadRequest, MensajeResponse("email y password son obligatorios"))

                val existente = usuariosReportantes.find(Filters.eq("email", req.email)).firstOrNull()
                if (existente != null)
                    return@post call.respond(HttpStatusCode.Conflict, MensajeResponse("El email ya está registrado"))

                val infoDoc = Document("national_id", req.personal_info.national_id)
                    .append("full_name", req.personal_info.full_name)
                    .append("phone", req.personal_info.phone)

                val doc = Document("email", req.email)
                    .append("password_hash", BCrypt.hashpw(req.password, BCrypt.gensalt()))
                    .append("personal_info", infoDoc)
                    .append("created_at", Instant.now().toString())

                usuariosReportantes.insertOne(doc)
                val newId = doc.getObjectId("_id").toHexString()
                call.respond(HttpStatusCode.Created, CrearCasoResponse(id = newId, mensaje = "Usuario registrado exitosamente"))
            }

            post("/login") {
                val req = try {
                    call.receive<LoginReportanteRequest>()
                } catch (e: Exception) {
                    return@post call.respond(HttpStatusCode.BadRequest, MensajeResponse("Cuerpo inválido: ${e.localizedMessage}"))
                }

                val userDoc = usuariosReportantes.find(Filters.eq("email", req.email)).firstOrNull()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, MensajeResponse("Credenciales inválidas"))

                val hash = userDoc.getString("password_hash")
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, MensajeResponse("Credenciales inválidas"))

                if (!BCrypt.checkpw(req.password, hash))
                    return@post call.respond(HttpStatusCode.Unauthorized, MensajeResponse("Credenciales inválidas"))

                val id = userDoc.getObjectId("_id").toHexString()
                val infoDoc = userDoc.get("personal_info", Document::class.java) ?: Document()
                val fullName = infoDoc.getString("full_name") ?: ""

                call.respond(TokenResponse(
                    token = JwtUtil.generateToken(id = id, role = "reportante", nombre = fullName),
                    tipo = "reportante",
                    id = id,
                    nombre = fullName
                ))
            }

            authenticate("auth-jwt") {

                get("/perfil") {
                    val principal = call.principal<JWTPrincipal>()
                    val id = principal?.payload?.getClaim("id")?.asString()
                        ?: return@get call.respond(HttpStatusCode.Unauthorized, MensajeResponse("Token inválido"))

                    if (!ObjectId.isValid(id))
                        return@get call.respond(HttpStatusCode.BadRequest, MensajeResponse("ID inválido"))

                    val userDoc = usuariosReportantes.find(Filters.eq("_id", ObjectId(id))).firstOrNull()
                        ?: return@get call.respond(HttpStatusCode.NotFound, MensajeResponse("Usuario no encontrado"))

                    call.respond(userDoc.toUsuarioReportanteResponse())
                }
            }
        }
    }
}

private fun Document.toUsuarioReportanteResponse(): UsuarioReportanteResponse {
    val infoDoc = get("personal_info", Document::class.java) ?: Document()
    return UsuarioReportanteResponse(
        id = getObjectId("_id").toHexString(),
        email = getString("email") ?: "",
        personal_info = InformacionPersonal(
            national_id = infoDoc.getString("national_id") ?: "",
            full_name = infoDoc.getString("full_name") ?: "",
            phone = infoDoc.getString("phone") ?: ""
        )
    )
}
