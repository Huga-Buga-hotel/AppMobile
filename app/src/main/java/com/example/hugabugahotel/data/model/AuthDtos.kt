package com.example.hugabugahotel.data.model

// Espelha com.hugabuga.api.dto.AuthDtos (backend). Valores de dinheiro sempre em centavos.

data class LoginRequest(
    val email: String,
    val senha: String,
    val lembrar: Boolean = true
)

data class LoginResponse(
    val accessToken: String,
    val refreshToken: String?,
    val expiraEmSegundos: Long,
    val usuarioId: String,
    val nome: String,
    /** VISITANTE ou ADMIN. */
    val role: String? = null
)

data class CadastroRequest(
    val nome: String,
    val sobrenome: String,
    val email: String,
    val senha: String,
    val celular: String,
    val pais: String = "BR",
    val tipoDocumento: String = "CPF",
    val numeroDocumento: String,
    /** Opcional (yyyy-MM-dd). */
    val dataNascimento: String? = null,
    val aceitaOfertas: Boolean = false
)

data class RefreshRequest(
    val refreshToken: String
)

data class EsqueciSenhaRequest(val email: String)

/** tokenDev só vem quando o backend roda no perfil dev (não há envio de e-mail). */
data class EsqueciSenhaResponse(val mensagem: String, val tokenDev: String? = null)

data class RedefinirSenhaRequest(val token: String, val novaSenha: String)
