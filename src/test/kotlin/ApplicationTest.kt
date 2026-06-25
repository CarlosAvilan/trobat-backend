package com.trobatapp

import com.trobatapp.models.ContactInfo
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
    fun testDesaparecidoSerializaUbicacion() {
        val desaparecido = Desaparecido(
            name = "Juan",
            description = "Descripción",
            age = 30,
            image = "",
            last_known_location = Ubicacion(
                type = "Point",
                coordinates = listOf(-58.3816, -34.6037)
            )
        )

        val json = Json.encodeToString(Desaparecido.serializer(), desaparecido)

        assertTrue(json.contains("\"last_known_location\""))
        assertTrue(json.contains("-58.3816"))
    }

    @Test
    fun testContactInfoSerializa() {
        val datos = ContactInfo(
            name = "Contacto Test",
            phone = "123456789",
            email = "contacto@test.com"
        )

        val json = Json.encodeToString(ContactInfo.serializer(), datos)

        assertTrue(json.contains("\"name\""))
        assertTrue(json.contains("\"phone\""))
        assertTrue(json.contains("\"email\""))
    }

}
