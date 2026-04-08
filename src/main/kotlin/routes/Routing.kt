package com.trobatapp.routes

import com.trobatapp.models.Reporte
import com.trobatapp.models.ReporteRespuesta
import com.trobatapp.models.Ubicacion
import com.trobatapp.coleccion
import io.ktor.http.*
import io.ktor.http.content.*
import java.io.File
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.flow.toList
import java.time.Instant
import java.util.UUID

fun Application.configureRouting() {
    routing {
        staticFiles("/uploads", File("uploads"))

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

        get("/reportes-cercanos") {

            val lat = call.request.queryParameters["lat"]?.toDoubleOrNull()
            val lng = call.request.queryParameters["lng"]?.toDoubleOrNull()
            val radio = call.request.queryParameters["radio"]?.toDoubleOrNull() ?: 1000.0 // metros

            if (lat == null || lng == null) {
                call.respond(HttpStatusCode.BadRequest, "Parámetros lat y lng son obligatorios")
                return@get
            }

            try {
                val pipeline = listOf(
                    org.bson.Document("\$geoNear", org.bson.Document()
                        .append("near", org.bson.Document("type", "Point")
                            .append("coordinates", listOf(lng, lat)))
                        .append("distanceField", "distancia")
                        .append("maxDistance", radio)
                        .append("spherical", true)
                    )
                )

                val resultados = coleccion
                    .withDocumentClass(org.bson.Document::class.java)
                    .aggregate(pipeline)
                    .toList()

                val respuesta = resultados.map { doc ->

                    val ubicacion = doc.get("ubicacion", org.bson.Document::class.java)
                    val coordinates = ubicacion.getList("coordinates", Number::class.java)

                    val lng = coordinates[0].toDouble()
                    val lat = coordinates[1].toDouble()

                    val distanciaMetros = (doc.get("distancia") as Number).toDouble()

                    ReporteRespuesta(
                        id = doc.getString("id_solicitud"),
                        lat = lat,
                        lng = lng,
                        descripcion = doc.getString("descripcion"),
                        imagen = doc.getString("url_foto"),
                        distancia_km = distanciaMetros / 1000
                    )
                }

                call.respond(respuesta)

            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, e.localizedMessage)
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

            var error = false
            var errorMessage = ""

            multipart.forEachPart { part ->
                when (part) {

                    is PartData.FormItem -> {
                        when (part.name) {
                            "id_solicitud" -> idSolicitud = part.value
                            "descripcion" -> descripcion = part.value
                            "estado" -> estado = part.value
                            "latitud" -> latitud = part.value.toDoubleOrNull() ?: 0.0
                            "longitud" -> longitud = part.value.toDoubleOrNull() ?: 0.0
                        }
                    }

                    is PartData.FileItem -> {

                        val contentType = part.contentType?.toString() ?: ""

                        // Validar tipo de archivo
                        if (!contentType.startsWith("image/")) {
                            error = true
                            errorMessage = "Solo se permiten imágenes"
                            part.dispose()
                            return@forEachPart
                        }

                        // Nombre único
                        fileName = "${UUID.randomUUID()}_${part.originalFileName}"

                        val bytes = part.streamProvider().readBytes()

                        // Validar tamaño (5MB)
                        if (bytes.size > 5 * 1024 * 1024) {
                            error = true
                            errorMessage = "La imagen es demasiado grande"
                            part.dispose()
                            return@forEachPart
                        }

                        File("uploads/$fileName").writeBytes(bytes)
                    }

                    else -> {}
                }
                part.dispose()
            }

            if (error) {
                call.respond(HttpStatusCode.BadRequest, errorMessage)
                return@post
            }

            // Validaciones finales
            if (idSolicitud.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, "id_solicitud es obligatorio")
                return@post
            }

            if (fileName.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, "Debe subir una imagen")
                return@post
            }

            if (latitud == 0.0 && longitud == 0.0) {
                call.respond(HttpStatusCode.BadRequest, "Ubicación inválida")
                return@post
            }

            // GeoJSON
            val ubicacion = Ubicacion(
                type = "Point",
                coordinates = listOf(longitud, latitud)
            )

            // Crear objeto
            val reporte = Reporte(
                id_solicitud = idSolicitud,
                url_foto = "http://localhost:8080/uploads/$fileName",
                estado = estado,
                ubicacion = ubicacion,
                descripcion = descripcion,
                fecha = Instant.now().toString()
            )

            // Guardar en Mongo
            coleccion.insertOne(reporte)

            // Log útil
            println("Nuevo reporte: $idSolicitud - $latitud,$longitud")

            call.respond(HttpStatusCode.OK, "Reporte creado con imagen")
        }
    }
}

