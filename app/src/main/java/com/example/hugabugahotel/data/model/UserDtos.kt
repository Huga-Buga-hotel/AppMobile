package com.example.hugabugahotel.data.model

// Espelha com.hugabuga.api.dto.ContaDtos (backend).

data class UserProfileResponse(
    val id: String,
    val nome: String,
    val sobrenome: String?,
    val email: String,
    val celular: String?,
    val pais: String?,
    val tipoDocumento: String?,
    val documentoMascarado: String?,
    val dataNascimento: String?,
    val aceitaOfertas: Boolean,
    /** VISITANTE ou ADMIN. */
    val role: String = "VISITANTE",
    /** pt-BR, en ou es. */
    val idioma: String = "pt-BR"
)

/** PATCH /me: campos nulos não são alterados. */
data class UpdateProfileRequest(
    val nome: String? = null,
    val sobrenome: String? = null,
    val celular: String? = null,
    val aceitaOfertas: Boolean? = null,
    val dataNascimento: String? = null
)

data class TrocarSenhaRequest(
    val senhaAtual: String,
    val novaSenha: String
)

data class PreferenciasDto(
    val idioma: String? = null,
    val notificacoesEmail: Boolean? = null,
    val notificacoesWhatsapp: Boolean? = null,
    val aceitaOfertas: Boolean? = null
)

data class MembroTribo(
    val id: String?,
    val nome: String,
    /** ADULTO ou CRIANCA. */
    val tipo: String,
    /** yyyy-MM-dd; obrigatória para criança. */
    val dataNascimento: String? = null,
    /** Calculada pelo backend (somente leitura). */
    val idade: Int? = null,
    val observacoes: String? = null
)

data class ReservaResumoDto(
    val codigo: String,
    val cavernaId: String,
    val cavernaNome: String?,
    val cavernaSlug: String?,
    val fotoUrl: String?,
    val checkIn: String,
    val checkOut: String,
    val noites: Int,
    val adultos: Int,
    val criancas: Int,
    val status: String,
    val total: Long,
    val pago: Boolean,
    /** "20 dias pra caverna". Nulo se já passou. */
    val diasParaCheckIn: Int?,
    val temCombo: Boolean,
    val podeAvaliar: Boolean
)

data class MesaResumoDto(
    val codigo: String,
    val data: String,
    val horario: String,
    val pessoas: Int,
    val status: String,
    val origem: String? = null
)

data class MinhasReservasResponse(
    val estadias: List<ReservaResumoDto> = emptyList(),
    val mesas: List<MesaResumoDto> = emptyList(),
    /** proximas, passadas, canceladas -> quantidade. */
    val contagem: Map<String, Int> = emptyMap()
)

/** Histórico de pagamentos (GET /me/pagamentos). Nenhum cartão fica salvo: só final e bandeira. */
data class PagamentoResumoDto(
    val id: String,
    val reservaCodigo: String?,
    val metodo: String,
    val status: String,
    val valorCentavos: Long,
    val parcelas: Int?,
    val cartaoFinal: String?,
    val cartaoBandeira: String?,
    val criadoEm: String,
    val aprovadoEm: String?
)

/** Notificação local do app (o backend não tem caixa de notificações). */
data class NotificacaoDto(
    val id: String?,
    val titulo: String,
    val descricao: String,
    val lida: Boolean? = false
)
