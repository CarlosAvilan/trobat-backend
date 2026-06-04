package com.trobatapp.service

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoCollection
import com.trobatapp.models.TokenResponse
import com.trobatapp.utils.JwtUtil
import kotlinx.coroutines.flow.firstOrNull
import org.bson.Document
import org.bson.types.ObjectId
import org.mindrot.jbcrypt.BCrypt
import java.time.Instant
import java.util.UUID

class AuthServiceImpl(
    private val usuarios: MongoCollection<Document>,
    private val oficiales: MongoCollection<Document>
) : IAuthService {

    override suspend fun registrarUsuario(name: String, email: String, password: String): String? {
        val existente = usuarios.find(Filters.eq("email", email)).firstOrNull()
        if (existente != null) return null

        val passwordHash = BCrypt.hashpw(password, BCrypt.gensalt())
        val doc = Document("id", UUID.randomUUID().toString())
            .append("name", name)
            .append("email", email)
            .append("password_hash", passwordHash)
            .append("fcm_tokens", emptyList<String>())
            .append("role", "user")
            .append("created_at", Instant.now().toString())
            .append("is_verified", false)

        usuarios.insertOne(doc)
        return doc.getObjectId("_id").toHexString()
    }

    override suspend fun loginUsuario(email: String, password: String, fcmToken: String?): TokenResponse? {
        val userDoc = usuarios.find(Filters.eq("email", email)).firstOrNull() ?: return null
        val hash = userDoc.getString("password_hash") ?: return null

        if (!BCrypt.checkpw(password, hash)) return null

        fcmToken?.let { token ->
            try {
                usuarios.updateOne(
                    Filters.eq("email", email),
                    Updates.addToSet("fcm_tokens", token)
                )
            } catch (e: Exception) {
                println("Error al guardar FCM token en login: ${e.message}")
            }
        }

        val id = userDoc.getObjectId("_id").toHexString()
        val nombre = userDoc.getString("name") ?: ""
        return TokenResponse(
            token = JwtUtil.generateToken(id = id, role = "user", nombre = nombre),
            tipo = "user",
            id = id,
            nombre = nombre
        )
    }

    override suspend fun loginOficial(emailInstitucional: String, password: String, fcmToken: String?): TokenResponse? {
        val oficialDoc = oficiales.find(Filters.eq("email_institucional", emailInstitucional)).firstOrNull() ?: return null
        val hash = oficialDoc.getString("hash_contrasenia") ?: return null

        if (!BCrypt.checkpw(password, hash)) return null

        fcmToken?.let { token ->
            try {
                oficiales.updateOne(
                    Filters.eq("email_institucional", emailInstitucional),
                    Updates.addToSet("fcm_tokens", token)
                )
            } catch (e: Exception) {
                println("Error al guardar FCM token en login oficial: ${e.message}")
            }
        }

        val id = oficialDoc.getObjectId("_id").toHexString()
        val nombre = oficialDoc.getString("nombre") ?: ""
        return TokenResponse(
            token = JwtUtil.generateToken(id = id, role = "oficial", nombre = nombre),
            tipo = "oficial",
            id = id,
            nombre = nombre
        )
    }

    override suspend fun logoutUsuario(id: String, role: String, fcmToken: String): Boolean {
        val collection = if (role == "oficial") oficiales else usuarios
        return try {
            val result = collection.updateOne(
                Filters.eq("_id", ObjectId(id)),
                Updates.pull("fcm_tokens", fcmToken)
            )
            result.matchedCount > 0
        } catch (e: Exception) {
            println("Error en logout: ${e.message}")
            false
        }
    }
}
