package com.example.hugabugahotel.data.model

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
    val saldoOssos: Int,
    val nivel: String,
    val idioma: String = "PT"
)

data class UpdateProfileRequest(
    val nome: String? = null,
    val celular: String? = null,
    val aceitaOfertas: Boolean? = null,
    val dataNascimento: String? = null
)

data class TrocarSenhaRequest(
    val senhaAtual: String,
    val novaSenha: String
)

data class MembroTribo(
    val id: String?,
    val nome: String,
    val tipo: String, // ADULTO ou CRIANCA
    val dataNascimento: String? = null,
    val idade: Int? = null,
    val observacoes: String? = null
)

data class ExtratoItemDto(
    val quantidade: Int,
    val motivo: String,
    val reservaId: String?,
    val criadoEm: String
)

data class OssosResponse(
    val saldo: Int,
    val acumulado: Int,
    val nivel: String,
    val proximoNivel: String?,
    val faltam: Int,
    val extrato: List<ExtratoItemDto> = emptyList()
)

data class MesaResumoDto(
    val codigo: String,
    val data: String,
    val horario: String,
    val pessoas: Int,
    val status: String
)

data class MinhasReservasResponse(
    val estadias: List<ReservaResponse> = emptyList(),
    val mesas: List<MesaResumoDto> = emptyList()
)

data class PagamentoSalvoDto(
    val id: String?,
    val titulo: String,
    val detalhes: String,
    val metodo: String,
    val principal: Boolean? = false
)

data class NotificacaoDto(
    val id: String?,
    val titulo: String,
    val descricao: String,
    val lida: Boolean? = false
)

data class FavoritoRequest(
    val cavernaId: String
)
