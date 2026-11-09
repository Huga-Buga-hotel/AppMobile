package com.example.hugabugahotel.data.model

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
    val campos: List<CampoErro> = emptyList()
)
