package com.example.hugabugahotel.data.model

// Espelha com.hugabuga.api.dto.AtividadeDtos (backend).

data class AtividadeItem(
    val id: String,
    val nome: String,
    /** CRIANCAS, FAMILIA, AVENTURA, NOITE ou ADULTOS. */
    val categoria: String,
    val descricao: String? = null,
    val duracaoMin: Int = 0,
    val idadeMin: Int? = null,
    val idadeMax: Int? = null,
    /** 0 = grátis. Vai para a conta da toca (pago no hotel). */
    val precoCentavos: Long = 0L,
    /** POR_PESSOA ou POR_CRIANCA. */
    val cobranca: String? = null,
    val destaque: String? = null,
    /** A API ainda não tem foto de atividade: o card mostra o placeholder. */
    val fotoUrl: String? = null
) {
    val displayNome: String get() = nome
    val displayDuracao: Int get() = duracaoMin
}

data class AtividadesListResponse(
    val itens: List<AtividadeItem> = emptyList(),
    val contagemPorCategoria: Map<String, Int> = emptyMap()
)

data class SessaoAtividade(
    val id: String,
    /** ISO 8601 com fuso (ex.: 2026-12-02T09:00:00-03:00). */
    val inicio: String,
    val vagasTotais: Int,
    val vagasLivres: Int,
    val esgotada: Boolean
)

data class AgendamentoRequest(
    val sessaoId: String,
    val quantidade: Int = 1
)

data class AgendamentoDto(
    val id: String,
    val sessaoId: String,
    val atividade: String,
    val inicio: String,
    val quantidade: Int,
    val valorCentavos: Long,
    /** ATIVO ou CANCELADO. */
    val status: String
)

data class AgendaItemDto(
    val hora: String,
    /** CAFE, ATIVIDADE ou MESA. */
    val tipo: String,
    val titulo: String,
    val detalhe: String? = null
)

data class AgendaDiaResponse(
    val data: String,
    val itens: List<AgendaItemDto> = emptyList()
)

/** Botão "Avisar": tipo CAVERNA ou ATIVIDADE. */
data class AlertaVagaRequest(
    val email: String,
    val tipo: String = "ATIVIDADE",
    val cavernaId: String? = null,
    val sessaoId: String? = null,
    val checkIn: String? = null,
    val checkOut: String? = null,
    val pessoas: Int? = null
)
