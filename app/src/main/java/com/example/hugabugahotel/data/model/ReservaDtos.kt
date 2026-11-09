package com.example.hugabugahotel.data.model

data class CriarReservaRequest(
    val cavernaId: String,
    val checkIn: String,
    val checkOut: String,
    val adultos: Int,
    val idadesCriancas: List<Int> = emptyList(),
    val email: String? = null
)

data class HospedeDto(
    val nome: String,
    val sobrenome: String?,
    val tipo: String, // ADULTO ou CRIANCA
    val idade: Int? = null
)

data class HospedesRequest(
    val responsavelNome: String,
    val responsavelSobrenome: String,
    val responsavelEmail: String,
    val responsavelCelular: String,
    val hospedes: List<HospedeDto> = emptyList()
)

data class ExtraItem(
    val id: String,
    val nome: String,
    val descricao: String?,
    val precoCentavos: Long
)

data class SelecaoExtraDto(
    val extraId: String,
    val quantidade: Int
)

data class AdicionarExtrasRequest(
    val extras: List<SelecaoExtraDto> = emptyList()
)

data class AplicarCupomRequest(
    val codigo: String
)

data class UsarOssosRequest(
    val quantidade: Int
)

data class PagamentoRequest(
    val metodo: String, // CARTAO, PIX, NA_CHEGADA
    val parcelas: Int? = 1,
    val numeroCartao: String? = null,
    val validade: String? = null,
    val cvv: String? = null,
    val aceitoRegras: Boolean = true
)

data class PagamentoResponse(
    val pagamentoId: String,
    val status: String,
    val qrCodePix: String? = null,
    val copiaEColaPix: String? = null,
    val expiraEm: String? = null
)

data class ReservaResponse(
    val codigo: String,
    val cavernaId: String,
    val cavernaNome: String,
    val checkIn: String,
    val checkOut: String,
    val adultos: Int,
    val status: String,
    val totalCentavos: Long,
    val expiracaoReserva: String? = null,
    val qrCodeAcesso: String? = null
)

data class CheckinRequest(
    val aceitoTermos: Boolean = true
)
