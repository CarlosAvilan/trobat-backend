package com.trobatapp.utils

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.util.Date

object JwtUtil {
    lateinit var secret: String
    lateinit var issuer: String
    lateinit var audience: String

    fun generateToken(id: String, role: String, nombre: String): String =
        JWT.create()
            .withIssuer(issuer)
            .withAudience(audience)
            .withClaim("id", id)
            .withClaim("role", role)
            .withClaim("nombre", nombre)
            .withExpiresAt(Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000L))
            .sign(Algorithm.HMAC256(secret))
}
