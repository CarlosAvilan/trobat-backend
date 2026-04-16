package com.trobatapp.service

import com.trobatapp.DTO.RegistrarUsuarioParamsDTO
import com.trobatapp.models.Usuario

interface IUsuarioService {
    suspend fun registrarUsuario(params: RegistrarUsuarioParamsDTO): Usuario ?
    suspend fun encontrarUsuarioPorEmail(email:String): Usuario ?
}