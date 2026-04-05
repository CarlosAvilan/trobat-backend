package com.trobatapp.plugins

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import kotlinx.serialization.json.Json

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true  // Ignora campos extra como el _id
            coerceInputValues = true  // Si algo viene nulo pero no debería, intenta arreglarlo
            isLenient = true          // Permite un formato de JSON más flexible
        })
    }
}
