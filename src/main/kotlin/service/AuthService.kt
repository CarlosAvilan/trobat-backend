package com.trobatapp.service

import com.mongodb.client.model.Filters
import com.mongodb.kotlin.client.coroutine.MongoCollection
import com.trobatapp.DTO.LoginParamsDTO
import com.trobatapp.DTO.RegistrarUsuarioParamsDTO
import com.trobatapp.models.Usuario
import kotlinx.coroutines.flow.firstOrNull
import org.mindrot.jbcrypt.BCrypt
import java.time.Instant
import java.util.UUID

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

    override suspend fun loginUsuario(params: LoginParamsDTO): Boolean {
        return try {
            val usuario = encontrarUsuarioPorEmail(params.email) ?: return false
            BCrypt.checkpw(params.password, usuario.password_hash)
        } catch (e: Exception) {
            println("Error al loguear usuario: ${e.message}")
            false
        }
    }

}