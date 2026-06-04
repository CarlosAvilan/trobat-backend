package com.trobatapp.config

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import io.ktor.server.application.*
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("FirebaseConfig")

fun Application.configureFirebase() {
    if (FirebaseApp.getApps().isNotEmpty()) {
        logger.info("Firebase ya inicializado, saltando.")
        return
    }

    val stream = this::class.java.classLoader.getResourceAsStream("firebase-service-account.json")
        ?: error("No se encontró firebase-service-account.json en resources")

    val options = FirebaseOptions.builder()
        .setCredentials(GoogleCredentials.fromStream(stream))
        .build()

    FirebaseApp.initializeApp(options)
    logger.info("Firebase Admin SDK inicializado correctamente.")
}
