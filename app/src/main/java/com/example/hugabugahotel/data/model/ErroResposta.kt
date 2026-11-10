package com.example.hugabugahotel.data.model

/** Formato padrão de erro do backend: toda resposta 4xx/5xx vem assim. */
data class CampoErro(
    val campo: String,
    val erro: String
)

data class ErroResposta(
    val status: Int,
    val codigo: String,
    val titulo: String,
    val mensagem: String,
    val ref: String?,
    val campos: List<CampoErro>? = null
)
