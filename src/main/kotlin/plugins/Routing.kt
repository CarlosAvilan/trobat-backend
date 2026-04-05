package com.trobatapp.plugins

import com.trobatapp.Reporte
import com.trobatapp.Ubicacion
import com.trobatapp.coleccion
import io.ktor.http.content.*
import java.io.File
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.flow.toList

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("Backend de Trobat conectado!")
        }

        get("/ver-reportes") {
            try {
                val lista = coleccion.find().toList()
                call.respond(lista)
            } catch (e: Exception) {
                call.respondText("Error al leer Atlas: ${e.localizedMessage}")
            }
        }

        post("/crear-reporte") {
            val multipart = call.receiveMultipart()

            var idSolicitud = ""
            var descripcion = ""
            var estado = "activo"
            var latitud = 0.0
            var longitud = 0.0
            var fileName = ""

            multipart.forEachPart { part ->
                when (part) {

                    is PartData.FormItem -> {
                        when (part.name) {
                            "id_solicitud" -> idSolicitud = part.value
                            "descripcion" -> descripcion = part.value
                            "estado" -> estado = part.value
                            "latitud" -> latitud = part.value.toDouble()
                            "longitud" -> longitud = part.value.toDouble()
                        }
                    }

                    is PartData.FileItem -> {
                        fileName = "${System.currentTimeMillis()}_${part.originalFileName}"

                        val file = File("uploads/$fileName")
                        part.streamProvider().use { input ->
                            file.outputStream().buffered().use { output ->
                                input.copyTo(output)
                            }
                        }
                    }

                    else -> {}
                }
                part.dispose()
            }

            val ubicacion = Ubicacion(
                type = "Point",
                coordinates = listOf(longitud, latitud) // ⚠️ GeoJSON
            )

            val reporte = Reporte(
                id_solicitud = idSolicitud,
                url_foto = "http://localhost:8080/uploads/$fileName",
                estado = estado,
                ubicacion = ubicacion,
                descripcion = descripcion,
                fecha = java.time.Instant.now().toString()
            )

            coleccion.insertOne(reporte)

            call.respondText("Reporte creado con imagen")
        }
    }
}

