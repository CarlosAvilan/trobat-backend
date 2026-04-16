package com.trobatapp.service

import com.mongodb.client.model.Filters
import com.mongodb.kotlin.client.coroutine.MongoCollection
import com.trobatapp.DTO.LoginParamsDTO
import com.trobatapp.DTO.RegistrarUsuarioParamsDTO
import com.trobatapp.models.Usuario
import kotlinx.coroutines.flow.firstOrNull
import java.time.Instant
import java.util.UUID

class IAuthServiceImpl(private val coleccion: MongoCollection<Usuario>) : IAuthService {

    override suspend fun registrarUsuario(params: RegistrarUsuarioParamsDTO): Usuario? {

        val usuarioExistente = encontrarUsuarioPorEmail(params.email)
        if (usuarioExistente != null) return null

        val nuevoUsuario = Usuario(
            id = UUID.randomUUID().toString(), // Generamos un ID único si no usamos ObjectId
            name = params.name,
            email = params.email,
            password_hash = params.password,
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
            val usuarioExistente = encontrarUsuarioPorEmail(params.email)

            if (usuarioExistente != null && params.password == usuarioExistente.password_hash){
                return true
            } else {
                return false
            }

        } catch (e: Exception) {
            println("Error al loguear usuario: ${e.message}")
            false
        }

    }

}