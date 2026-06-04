package com.trobatapp

import com.mongodb.client.model.Indexes
import com.mongodb.kotlin.client.coroutine.MongoClient
import com.trobatapp.models.Reporte
import com.trobatapp.service.AuthServiceImpl
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import com.trobatapp.plugins.*
import com.trobatapp.routes.configureAuthRouting
import com.trobatapp.routes.configureCasosRouting
import com.trobatapp.routes.configureReportesRouting
import com.trobatapp.routes.configureRouting
import com.trobatapp.routes.configureUsuariosRouting
import org.bson.Document

val uri = System.getenv("MONGODB_URI") ?: error("MONGODB_URI no configurado")
val client = MongoClient.create(uri)
val database = client.getDatabase("TrobatDB")

val coleccion = database.getCollection<Reporte>("reportes_fotos")
val casos = database.getCollection<Document>("casos")
val reportes = database.getCollection<Document>("reportes")
val usuarios = database.getCollection<Document>("usuarios")
val oficiales = database.getCollection<Document>("usuarios")

fun main() {
    embeddedServer(Netty, port = 8081, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    val authService = AuthServiceImpl(usuarios, oficiales)

    configureHTTP()
    configureSerialization()
    configureAuth()
    configureRouting()
    configureCasosRouting()
    configureReportesRouting()
    configureAuthRouting(authService)
    configureUsuariosRouting()

    environment.monitor.subscribe(ApplicationStarted) {
        kotlinx.coroutines.runBlocking {
            reportes.createIndex(Indexes.geo2dsphere("location"))
        }
    }
}
