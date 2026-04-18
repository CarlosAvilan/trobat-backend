package com.trobatapp.routes

import com.trobatapp.DTO.LoginParamsDTO
import com.trobatapp.DTO.LogoutParamsDTO
import com.trobatapp.DTO.RegistrarUsuarioParamsDTO
import com.trobatapp.service.IAuthService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.authRoutes(authService: IAuthService) {

    route("/auth") {

        authenticate("auth-jwt") {
            post("/auth/logout") {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val email = principal?.payload?.getClaim("email")?.asString()
                        ?: return@post call.respond(HttpStatusCode.Unauthorized)

                    val params = call.receive<LogoutParamsDTO>()
                    val exito = authService.logoutUsuario(email, params.fcmToken)

                    if (exito) {
                        call.respond(HttpStatusCode.OK, "Sesión cerrada correctamente")
                    } else {
                        call.respond(HttpStatusCode.NotFound, "No se pudo cerrar la sesión")
                    }
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, "Error al procesar logout")
                }
            }
        }

        post("/registro") {
            try {
                val params = call.receive<RegistrarUsuarioParamsDTO>()
                val nuevoUsuario = authService.registrarUsuario(params)

                if (nuevoUsuario != null) {
                    call.respond(HttpStatusCode.Created, nuevoUsuario)
                } else {
                    call.respond(HttpStatusCode.Conflict, "El email ya se encuentra registrado")
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, "Datos inválidos: ${e.localizedMessage}")
            }
        }

        post("/login") {
            val params = call.receive<LoginParamsDTO>()
            val token = authService.loginUsuario(params)

            if (token != null) {
                call.respond(mapOf("token" to token))
            } else {
                call.respond(HttpStatusCode.Unauthorized, "Credenciales inválidas")
            }
        }

    }
}