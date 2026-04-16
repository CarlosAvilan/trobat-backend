package com.trobatapp.routes

import com.trobatapp.DTO.RegistrarUsuarioParamsDTO
import com.trobatapp.service.IUsuarioService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.usuarioRoutes(usuarioService: IUsuarioService) {

    route("/usuarios") {

        post("/registro") {
            try {
                val params = call.receive<RegistrarUsuarioParamsDTO>()
                val nuevoUsuario = usuarioService.registrarUsuario(params)

                if (nuevoUsuario != null) {
                    call.respond(HttpStatusCode.Created, nuevoUsuario)
                } else {
                    call.respond(HttpStatusCode.Conflict, "El email ya se encuentra registrado")
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, "Datos inválidos: ${e.localizedMessage}")
            }
        }

    }
}