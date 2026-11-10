package com.example.hugabugahotel.data.model

// Espelha com.hugabuga.api.dto.CavernaDtos (backend).

data class CavernSummary(
    val id: String,
    val slug: String,
    val nome: String,
    /** FAMILIA, CASAL, AMIGOS ou GRUPO. */
    val categoria: String,
    val ala: String?,
    val resumo: String?,
    val capacidade: Int,
    val camas: Int,
    val areaM2: Int,
    val precoBaseCentavos: Long,
    /** Só vem quando a busca tem datas. */
    val totalEstadiaCentavos: Long?,
    val notaMedia: Double,
    val totalRelatos: Int,
    val acessivel: Boolean,
    val aceitaPets: Boolean,
    val fotoUrl: String?,
    val unidadesLivres: Int?,
    val ultimas: Boolean,
    /** Ids das comodidades (ex.: fogueira-privativa). */
    val comodidades: List<String> = emptyList()
)

data class SugestaoData(val checkIn: String, val checkOut: String, val rotulo: String)

data class CavernListResponse(
    val total: Int,
    val filtrosAtivos: List<String> = emptyList(),
    val avisos: List<String> = emptyList(),
    val itens: List<CavernSummary> = emptyList(),
    val sugestoes: List<SugestaoData> = emptyList()
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

/** Uma noite do calendário de disponibilidade. */
data class DiaDisponibilidade(
    val data: String,
    val unidadesLivres: Int,
    val totalUnidades: Int,
    val disponivel: Boolean,
    val precoNoiteCentavos: Long,
    val feriado: Boolean
)

data class RelatoDto(
    val id: String,
    val nota: Int,
    val texto: String?,
    val autor: String,
    val criadoEm: String,
    /** URLs relativas à raiz do servidor (ex.: /api/v1/arquivos/relatos/x.jpg). */
    val fotos: List<String> = emptyList()
)

data class PaginaRelatos(
    val itens: List<RelatoDto> = emptyList(),
    val page: Int,
    val size: Int,
    val total: Long
)
