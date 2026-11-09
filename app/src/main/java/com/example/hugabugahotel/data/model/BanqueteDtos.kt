package com.example.hugabugahotel.data.model

data class RestauranteStatusResponse(
    val abertoAgora: Boolean,
    val proximoTurno: String?,
    val descontoHospedePercentual: Int
)

data class Prato(
    val id: String,
    val nome: String,
    val categoria: String,
    val descricao: String?,
    val precoCentavos: Long,
    val tag: String?
)

data class MesaHorario(
    val horario: String,
    val status: String // LIVRE ou LOTADO
)

data class ReservaMesaRequest(
    val data: String,
    val horario: String,
    val pessoas: Int,
    val cadeirinhaInfantil: Boolean = false,
    val aoLadoFogueira: Boolean = false
)
