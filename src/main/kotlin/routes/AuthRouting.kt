package com.trobatapp.routes

import com.trobatapp.DTO.LoginParamsDTO
import com.trobatapp.DTO.RegistrarUsuarioParamsDTO
import com.trobatapp.service.IAuthService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.authRoutes(authService: IAuthService) {

    route("/auth") {

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
            try {
                val params = call.receive<LoginParamsDTO>()
                val resultado = authService.loginUsuario(params)

                if (resultado == true) {
                    call.respond(HttpStatusCode.OK, "Login exitoso")
                } else {
                    call.respond(HttpStatusCode.Conflict, "El email o la contraseña son incorrectos")
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, "Datos inválidos: ${e.localizedMessage}")
            }
        }

    }
}