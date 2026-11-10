package com.example.hugabugahotel.data.model

// Espelha com.hugabuga.api.dto.ReservaDtos e PagamentoDtos (backend).

data class CriarReservaRequest(
    val cavernaId: String,
    val checkIn: String,
    val checkOut: String,
    val adultos: Int,
    val idadesCriancas: List<Int> = emptyList(),
    /** Obrigatório sem login: vira o e-mail do responsável. */
    val email: String? = null
)

data class HospedeDto(
    val nome: String,
    /** ADULTO ou CRIANCA. */
    val tipo: String,
    /** Obrigatória para criança (0 a 17). */
    val idade: Int? = null
)

data class HospedesRequest(
    val responsavelNome: String,
    val responsavelEmail: String,
    val celular: String,
    val pais: String = "BR",
    val tipoDocumento: String = "CPF",
    val documento: String,
    val pedidoEspecial: String? = null,
    val hospedes: List<HospedeDto>
)

data class ExtraItem(
    val id: String,
    val nome: String,
    val descricao: String?,
    val icone: String? = null,
    val precoCentavos: Long,
    /** POR_ESTADIA, POR_CRIANCA ou POR_PESSOA. */
    val cobranca: String = "POR_ESTADIA",
    val incluso: Boolean = false
)

data class SelecaoExtraDto(
    val extraId: String,
    val quantidade: Int = 1
)

data class AdicionarExtrasRequest(
    val extras: List<SelecaoExtraDto> = emptyList()
)

data class AplicarCupomRequest(
    val codigo: String
)

/** Combo quarto + Banquete (GET /restaurante/combos). */
data class ComboDto(
    val id: String,
    val nome: String,
    val descricao: String?,
    val precoAdultoCentavos: Long,
    val precoCriancaCentavos: Long
)

data class ComboRequest(
    val comboId: String,
    /** HH:mm (19:00 a 21:30). */
    val horario: String
)

data class ComboSelecionado(
    val comboId: String,
    val nome: String,
    val horario: String,
    val noites: Int,
    val adultosPagantes: Int,
    val criancasPagantes: Int,
    val valorCentavos: Long,
    val mesas: List<String> = emptyList()
)

data class ExtraSelecionado(
    val extraId: String,
    val nome: String,
    val quantidade: Int,
    val precoUnitarioCentavos: Long,
    val subtotalCentavos: Long
)

/** Reserva completa (é também o voucher). */
data class ReservaResponse(
    val codigo: String,
    val status: String,
    val cavernaId: String,
    val cavernaNome: String,
    val unidadeCodigo: String? = null,
    val checkIn: String,
    val checkOut: String,
    val noites: Int = 0,
    val adultos: Int,
    val criancas: Int = 0,
    val expiraEm: String? = null,
    val prorrogada: Boolean = false,
    val responsavelNome: String? = null,
    val responsavelEmail: String? = null,
    val celular: String? = null,
    val hospedes: List<HospedeDto> = emptyList(),
    val extras: List<ExtraSelecionado> = emptyList(),
    val combo: ComboSelecionado? = null,
    val cupom: String? = null,
    val pedidoEspecial: String? = null,
    val lateCheckout: Boolean = false,
    val valorDiarias: Long = 0,
    val valorExtras: Long = 0,
    val valorCombo: Long = 0,
    val desconto: Long = 0,
    val total: Long = 0,
    val pago: Boolean = false,
    val checkinOnlineEm: String? = null
)

data class CancelarResponse(
    val reserva: ReservaResponse,
    val reembolsoCentavos: Long,
    val multaCentavos: Long
)

data class PagamentoRequest(
    /** CARTAO, PIX ou NA_CHEGADA. */
    val metodo: String,
    val parcelas: Int? = null,
    val numeroCartao: String? = null,
    /** MM/AA. */
    val validade: String? = null,
    val cvv: String? = null,
    val aceitoRegras: Boolean = false
)

data class PagamentoResponse(
    val id: String,
    val reservaCodigo: String,
    val metodo: String,
    /** PENDENTE, APROVADO, RECUSADO ou EXPIRADO. */
    val status: String,
    val valorCentavos: Long,
    val parcelas: Int? = null,
    val valorParcelaCentavos: Long? = null,
    val cartaoFinal: String? = null,
    val cartaoBandeira: String? = null,
    val pixCopiaCola: String? = null,
    val pixExpiraEm: String? = null,
    val motivoRecusa: String? = null
)

data class AlterarDatasRequest(
    val checkIn: String,
    val checkOut: String
)
