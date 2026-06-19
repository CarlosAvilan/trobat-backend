package com.trobatapp

import com.trobatapp.models.DatosContacto
import com.trobatapp.models.Desaparecido
import com.trobatapp.models.Ubicacion
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ApplicationTest {

    @Test
    fun testRoot() = testApplication {
        environment {
            config = io.ktor.server.config.MapApplicationConfig(
                "jwt.secret" to "test-secret",
                "jwt.issuer" to "test-issuer",
                "jwt.audience" to "test-audience",
                "jwt.realm" to "test-realm"
            )
        }
        application {
            module()
        }
        client.get("/").apply {
            assertEquals(HttpStatusCode.OK, status)
        }
    }

    @Test
    fun testDesaparecidoSerializaUbicacionOriginal() {
        val desaparecido = Desaparecido(
            nombre = "Juan",
            descripcion = "Descripción",
            ubicacion_original = Ubicacion(
                type = "Point",
                coordinates = listOf(-58.3816, -34.6037)
            )
        )

        val json = Json.encodeToString(Desaparecido.serializer(), desaparecido)

        assertTrue(json.contains("\"ubicacion_original\""))
        assertTrue(json.contains("-58.3816"))
    }

    @Test
    fun testDatosContactoPoliciaSerializa() {
        val datos = DatosContacto(
            nombre = "Oficial Pérez",
            telefono = "123456789",
            email = "policia@test.com"
        )

        val json = Json.encodeToString(DatosContacto.serializer(), datos)

        assertTrue(json.contains("\"nombre\""))
        assertTrue(json.contains("\"telefono\""))
        assertTrue(json.contains("\"email\""))
    }

}
