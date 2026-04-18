package com.trobatapp

import com.mongodb.kotlin.client.coroutine.MongoClient
import com.trobatapp.models.Reporte
import com.trobatapp.models.Usuario
import com.trobatapp.service.IAuthServiceImpl
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.trobatapp.plugins.*
import com.trobatapp.routes.configureRouting
import io.ktor.http.HttpMethod
import io.ktor.server.plugins.cors.routing.*
import io.ktor.http.*

// Conexión a MongoDB Atlas
val uri = "mongodb+srv://trobatDBuser:4trob.yikes,,8@clustertrobat.mx4yx3s.mongodb.net/?retryWrites=true&w=majority"
val client = MongoClient.create(uri)
val database = client.getDatabase("TrobatDB")

val coleccion = database.getCollection<Reporte>("reportes_fotos")
val coleccionUsuarios = database.getCollection<Usuario>("usuarios")

//Configuracion JWT
val jwtSecret = "clave_123"
val jwtIssuer = "http://0.0.0.0:8080/"
val jwtAudience = "trobat-users"


fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    install(CORS) {
        // Peticiones desde origen de desarrollo
        allowHost("127.0.0.1:8080")
        allowHost("localhost:8080")

        // Permite los métodos
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Options)

        // Permite headers
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
    }

    install(Authentication) {
        jwt("auth-jwt") {
            realm = "Acceso a Trobat"
            verifier(
                JWT.require(Algorithm.HMAC256(jwtSecret))
                    .withAudience(jwtAudience)
                    .withIssuer(jwtIssuer)
                    .build()
            )
            validate { credential ->
                if (credential.payload.getClaim("email").asString() != "") {
                    JWTPrincipal(credential.payload)
                } else null
            }
        }
    }

    configureSerialization() // Para que el JSON funcione
    configureRouting(IAuthServiceImpl(coleccionUsuarios))      // Para que las rutas funcionen
}
