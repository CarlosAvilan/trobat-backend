package com.trobatapp.routes

import com.trobatapp.models.*
import com.trobatapp.service.IAuthService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureAuthRouting(authService: IAuthService) {
    routing {
        route("/auth") {

            post("/registro") {
                val req = try {
                    call.receive<RegistroRequest>()
                } catch (e: Exception) {
                    return@post call.respond(HttpStatusCode.BadRequest, MensajeResponse("Cuerpo inválido"))
                }

                if (req.name.isBlank() || req.email.isBlank() || req.password.isBlank()) {
                    return@post call.respond(HttpStatusCode.BadRequest, MensajeResponse("Todos los campos son obligatorios"))
                }

                val newId = authService.registrarUsuario(req.name, req.email, req.password)
                if (newId != null) {
                    call.respond(HttpStatusCode.Created, CrearCasoResponse(id = newId, mensaje = "Usuario registrado exitosamente"))
                } else {
                    call.respond(HttpStatusCode.Conflict, MensajeResponse("El email ya está registrado"))
                }
            }

            post("/login") {
                val req = try {
                    call.receive<LoginRequest>()
                } catch (e: Exception) {
                    return@post call.respond(HttpStatusCode.BadRequest, MensajeResponse("Cuerpo inválido"))
                }

                val response = authService.loginUsuario(req.email, req.password, req.fcm_token)
                if (response != null) {
                    call.respond(response)
                } else {
                    call.respond(HttpStatusCode.Unauthorized, MensajeResponse("Credenciales inválidas"))
                }
            }

            post("/login/oficial") {
                val req = try {
                    call.receive<LoginOficialRequest>()
                } catch (e: Exception) {
                    return@post call.respond(HttpStatusCode.BadRequest, MensajeResponse("Cuerpo inválido"))
                }

                val response = authService.loginOficial(req.email_institucional, req.password, req.fcm_token)
                if (response != null) {
                    call.respond(response)
                } else {
                    call.respond(HttpStatusCode.Unauthorized, MensajeResponse("Credenciales inválidas"))
                }
            }

            authenticate("auth-jwt") {
                post("/logout") {
                    val principal = call.principal<JWTPrincipal>()
                    val id = principal?.payload?.getClaim("id")?.asString()
                        ?: return@post call.respond(HttpStatusCode.Unauthorized, MensajeResponse("Token inválido"))
                    val role = principal.payload.getClaim("role")?.asString() ?: "user"

                    val req = try {
                        call.receive<LogoutRequest>()
                    } catch (e: Exception) {
                        return@post call.respond(HttpStatusCode.BadRequest, MensajeResponse("Cuerpo inválido"))
                    }

                    val exito = authService.logoutUsuario(id, role, req.fcm_token)
                    if (exito) {
                        call.respond(MensajeResponse("Sesión cerrada correctamente"))
                    } else {
                        call.respond(HttpStatusCode.NotFound, MensajeResponse("No se pudo cerrar la sesión"))
                    }
                }
            }
        }
    }
}
