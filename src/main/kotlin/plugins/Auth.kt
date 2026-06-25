package com.trobatapp.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.trobatapp.utils.JwtUtil
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*

fun Application.configureAuth() {
    val secret = System.getenv("JWT_SECRET")
        ?: environment.config.property("jwt.secret").getString()
    val issuer = environment.config.property("jwt.issuer").getString()
    val audience = environment.config.property("jwt.audience").getString()
    val realm = environment.config.property("jwt.realm").getString()

    JwtUtil.secret = secret
    JwtUtil.issuer = issuer
    JwtUtil.audience = audience

    install(Authentication) {
        jwt("auth-jwt") {
            this.realm = realm
            verifier(
                JWT.require(Algorithm.HMAC256(secret))
                    .withIssuer(issuer)
                    .withAudience(audience)
                    .build()
            )
            validate { credential ->
                val id = credential.payload.getClaim("id").asString()
                if (!id.isNullOrBlank()) JWTPrincipal(credential.payload) else null
            }
        }
    }
}
