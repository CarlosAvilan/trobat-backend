package com.trobatapp

import com.mongodb.kotlin.client.coroutine.MongoClient
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import com.trobatapp.plugins.* // Importante para encontrar configureRouting

// Conexión a MongoDB Atlas
val uri = "mongodb+srv://trobatDBuser:4trob.yikes,,8@clustertrobat.mx4yx3s.mongodb.net/?retryWrites=true&w=majority"
val client = MongoClient.create(uri)
val database = client.getDatabase("TrobatDB")
val coleccion = database.getCollection<Reporte>("reportes_fotos")

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    configureSerialization() // Para que el JSON funcione
    configureRouting()      // Para que las rutas funcionen
}
