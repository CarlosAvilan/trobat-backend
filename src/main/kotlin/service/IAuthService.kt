package com.trobatapp.service

import com.trobatapp.models.TokenResponse

interface IAuthService {
    suspend fun registrarUsuario(name: String, email: String, password: String): String?
    suspend fun loginUsuario(email: String, password: String, fcmToken: String?): TokenResponse?
    suspend fun loginOficial(emailInstitucional: String, password: String, fcmToken: String?): TokenResponse?
    suspend fun logoutUsuario(id: String, role: String, fcmToken: String): Boolean
    suspend fun resetPasswordOficial(emailInstitucional: String, nuevaPassword: String): Boolean
}
