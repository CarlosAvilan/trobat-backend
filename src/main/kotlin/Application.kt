package com.trobatapp

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.mongodb.client.model.Indexes
import com.mongodb.kotlin.client.coroutine.MongoClient
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
import com.trobatapp.routes.configureUsuariosReportantesRouting
import org.bson.Document

val uri = System.getenv("MONGODB_URI") ?: error("MONGODB_URI no configurado")
val client = MongoClient.create(uri)
val database = client.getDatabase("TrobatDB")

val casos = database.getCollection<Document>("casos")
val reportes = database.getCollection<Document>("reportes")
val usuarios = database.getCollection<Document>("usuarios")
val oficiales = database.getCollection<Document>("usuarios")
val usuariosReportantes = database.getCollection<Document>("Usuario_reportante")

fun main() {
    initFirebase()
    embeddedServer(Netty, port = 8081, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun initFirebase() {
    if (FirebaseApp.getApps().isNotEmpty()) return
    val serviceAccount = object {}.javaClass.classLoader
        .getResourceAsStream("firebase-service-account.json")
        ?: error("firebase-service-account.json no encontrado en resources")
    val options = FirebaseOptions.builder()
        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
        .setStorageBucket("trobat-40cea.firebasestorage.app")
        .build()
    FirebaseApp.initializeApp(options)
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
    configureUsuariosReportantesRouting()

    environment.monitor.subscribe(ApplicationStarted) {
        kotlinx.coroutines.runBlocking {
            reportes.createIndex(Indexes.geo2dsphere("location"))
            casos.createIndex(Indexes.geo2dsphere("missing_person.last_known_location"))
        }
    }
}
