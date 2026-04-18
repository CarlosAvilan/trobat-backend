package com.trobatapp.service

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.mongodb.client.model.Filters
import com.mongodb.kotlin.client.coroutine.MongoCollection
import com.trobatapp.DTO.LoginParamsDTO
import com.trobatapp.DTO.RegistrarUsuarioParamsDTO
import com.trobatapp.jwtAudience
import com.trobatapp.jwtIssuer
import com.trobatapp.jwtSecret
import com.trobatapp.models.Usuario
import kotlinx.coroutines.flow.firstOrNull
import org.mindrot.jbcrypt.BCrypt
import java.time.Instant
import java.util.Date
import java.util.UUID
import com.mongodb.client.model.Updates

class IAuthServiceImpl(private val coleccion: MongoCollection<Usuario>) : IAuthService {

    override suspend fun registrarUsuario(params: RegistrarUsuarioParamsDTO): Usuario? {

        val usuarioExistente = encontrarUsuarioPorEmail(params.email)
        if (usuarioExistente != null) return null

        //Encripto password
        val passwordHasheada = BCrypt.hashpw(params.password, BCrypt.gensalt())

        val nuevoUsuario = Usuario(
            id = UUID.randomUUID().toString(), // Generamos un ID único si no usamos ObjectId
            name = params.name,
            email = params.email,
            password_hash = passwordHasheada,
            role = "user",
            created_at = Instant.now().toString(),
            is_verified = false
        )

        return try {
            coleccion.insertOne(nuevoUsuario)
            nuevoUsuario
        } catch (e: Exception) {
            println("Error al registrar usuario: ${e.message}")
            null
        }
    }


    override suspend fun encontrarUsuarioPorEmail(email: String): Usuario? {
        return try {
            coleccion.find(Filters.eq("email", email)).firstOrNull()
        } catch (e: Exception) {
            println("Error al buscar usuario: ${e.message}")
            null
        }
    }

    override suspend fun loginUsuario(params: LoginParamsDTO): String? {
        val usuario = encontrarUsuarioPorEmail(params.email) ?: return null
        val passwordCorrecta = BCrypt.checkpw(params.password, usuario.password_hash)

        return if (passwordCorrecta) {
            // Si viene un token, lo agregamos a la lista en la DB
            params.fcmToken?.let { token ->
                try {
                    coleccion.updateOne(
                        Filters.eq("email", usuario.email),
                        Updates.addToSet("fcm_tokens", token)
                    )
                } catch (e: Exception) {
                    println("Error al guardar FCM Token: ${e.message}")
                }
            }

            // Generar el Token
            JWT.create()
                .withAudience(jwtAudience)
                .withIssuer(jwtIssuer)
                .withClaim("email", usuario.email)
                .withClaim("userId", usuario.id)
                .withExpiresAt(Date(System.currentTimeMillis() + 3600000)) // Expira en 1 hora
                .sign(Algorithm.HMAC256(jwtSecret))
        } else {
            null
        }
    }

    override suspend fun logoutUsuario(email: String, fcmToken: String): Boolean {
        return try {
            // Usamos $pull para remover específicamente ese token de la lista
            val resultado = coleccion.updateOne(
                Filters.eq("email", email),
                Updates.pull("fcm_tokens", fcmToken)
            )
            // Retornamos true si se encontró al usuario y se procesó la solicitud
            resultado.matchedCount > 0
        } catch (e: Exception) {
            println("Error en logout: ${e.message}")
            false
        }
    }
}