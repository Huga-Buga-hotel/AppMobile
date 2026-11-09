package com.example.hugabugahotel.data.model

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
    val nome: String
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
    val dataNascimento: String,
    val aceitaOfertas: Boolean = true
)

data class RefreshRequest(
    val refreshToken: String
)
