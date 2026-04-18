package com.trobatapp.service

import com.trobatapp.DTO.LoginParamsDTO
import com.trobatapp.DTO.RegistrarUsuarioParamsDTO
import com.trobatapp.models.Usuario

interface IAuthService {
    suspend fun registrarUsuario(params: RegistrarUsuarioParamsDTO): Usuario ?
    suspend fun encontrarUsuarioPorEmail(email:String): Usuario ?
    suspend fun loginUsuario(params: LoginParamsDTO): String?
    suspend fun logoutUsuario(email: String, fcmToken: String): Boolean
}