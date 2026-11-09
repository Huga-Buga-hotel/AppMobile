package com.example.hugabugahotel.data.model

data class CavernSummary(
    val id: String,
    val slug: String,
    val nome: String,
    val categoria: String,
    val ala: String?,
    val resumo: String?,
    val capacidade: Int,
    val camas: Int,
    val areaM2: Int,
    val precoBaseCentavos: Long,
    val totalEstadiaCentavos: Long?,
    val notaMedia: Double,
    val totalRelatos: Int,
    val acessivel: Boolean,
    val aceitaPets: Boolean,
    val fotoUrl: String?,
    val unidadesLivres: Int?,
    val ultimas: Boolean,
    val comodidades: List<String> = emptyList()
)

data class CavernListResponse(
    val total: Int,
    val filtrosAtivos: List<String> = emptyList(),
    val avisos: List<String> = emptyList(),
    val itens: List<CavernSummary> = emptyList(),
    val sugestoes: List<String> = emptyList()
)

data class FotoDto(
    val url: String,
    val legenda: String?
)

data class ComodidadeItem(
    val id: String,
    val nome: String,
    val icone: String?
)

data class CavernDetailResponse(
    val caverna: CavernSummary,
    val descricao: String?,
    val fotos: List<FotoDto> = emptyList(),
    val comodidades: List<ComodidadeItem> = emptyList()
)

data class CotacaoNoiteDetail(
    val data: String,
    val precoNoiteCentavos: Long,
    val adicionalAdultosCentavos: Long
)

data class CotacaoResponse(
    val noites: Int,
    val adultosCobrados: Int,
    val adultosExtras: Int,
    val criancasGratis: Int,
    val valorDiariasCentavos: Long,
    val detalhe: List<CotacaoNoiteDetail> = emptyList(),
    val feriado: Boolean
)
