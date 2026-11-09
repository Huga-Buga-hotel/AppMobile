package com.example.hugabugahotel.data.model

data class AtividadeItem(
    val id: String,
    val nome: String? = null,
    val titulo: String? = null,
    val categoria: String,
    val descricao: String? = null,
    val precoCentavos: Long = 0L,
    val duracaoMin: Int? = null,
    val duracaoMinutos: Int? = null,
    val idadeMin: Int? = null,
    val idadeMax: Int? = null,
    val cobranca: String? = null,
    val faixaEtaria: String? = null,
    val fotoUrl: String? = null,
    val destaque: String? = null
) {
    val displayNome: String
        get() = nome ?: titulo ?: "Atividade"

    val displayDuracao: Int
        get() = duracaoMin ?: duracaoMinutos ?: 45
}

data class AtividadesListResponse(
    val itens: List<AtividadeItem> = emptyList(),
    val contagemPorCategoria: Map<String, Int> = emptyMap()
)

data class SessaoAtividade(
    val id: String,
    val atividadeId: String,
    val inicio: String? = null,
    val dataHora: String? = null,
    val vagasTotais: Int? = null,
    val vagasLivres: Int? = null,
    val vagasDisponiveis: Int? = null,
    val esgotada: Boolean = false
)

data class AgendamentoRequest(
    val sessaoId: String,
    val quantidade: Int = 1
)

data class AgendaItemDto(
    val hora: String,
    val tipo: String, // CAFE, ATIVIDADE, MESA
    val titulo: String,
    val detalhe: String? = null,
    val subtitulo: String? = null
)

data class AgendaDiaResponse(
    val data: String,
    val itens: List<AgendaItemDto> = emptyList()
)

data class ChatbotRequest(
    val mensagem: String,
    @com.google.gson.annotations.SerializedName("id_sessao")
    val idSessao: String
)

data class ChatbotResponse(
    val resposta: String? = null,
    val mensagem: String? = null,
    val text: String? = null,
    val remetente: String? = null
) {
    val displayResposta: String
        get() = resposta ?: mensagem ?: text ?: "Ugh! A Xamã ouviu sua mensagem."
}

data class AlertaVagaRequest(
    val atividadeId: String,
    val data: String
)
