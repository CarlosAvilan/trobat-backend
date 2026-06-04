package com.trobatapp.utils

import com.trobatapp.models.MensajeResponse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*

suspend fun ApplicationCall.verificarRol(vararg roles: String): Boolean {
    val principal = principal<JWTPrincipal>()
    val role = principal?.payload?.getClaim("role")?.asString()
    return if (role != null && role in roles) {
        true
    } else {
        respond(HttpStatusCode.Forbidden, MensajeResponse("Acceso denegado. Roles requeridos: ${roles.toList()}"))
        false
    }
}
