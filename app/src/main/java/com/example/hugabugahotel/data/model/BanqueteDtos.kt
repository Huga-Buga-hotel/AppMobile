package com.example.hugabugahotel.data.model

// Espelha com.hugabuga.api.dto.RestauranteDtos (backend).

data class TurnoDto(val nome: String, val abre: String, val fecha: String)

data class RestauranteStatusResponse(
    val turnos: List<TurnoDto> = emptyList(),
    val abertoAgora: Boolean,
    val turnoAtual: String?,
    val descontoHospedePct: Int,
    val capacidadePessoas: Int
)

data class Prato(
    val id: String,
    val nome: String,
    /** NA_FOGUEIRA, ENTRADAS, MENU_KIDS, DA_HORTA, SOBREMESAS ou POCOES. */
    val categoria: String,
    val descricao: String?,
    val precoCentavos: Long,
    val tags: List<String> = emptyList()
)

data class MesaHorario(
    /** HH:mm:ss */
    val horario: String,
    /** LIVRE ou LOTADO. */
    val status: String,
    val vagasRestantes: Int
)

/**
 * Com codigoReserva: mesa de hóspede (estadia CONFIRMADA, 15% de desconto).
 * Sem: visitante de fora (nomeContato, emailContato e celularContato obrigatórios se não estiver logado).
 */
data class ReservaMesaRequest(
    val codigoReserva: String? = null,
    val nomeContato: String? = null,
    val emailContato: String? = null,
    val celularContato: String? = null,
    val data: String,
    val horario: String,
    val pessoas: Int,
    val cadeirinhas: Int = 0,
    val pertoFogueira: Boolean = false
)

data class MesaResponse(
    val codigo: String,
    /** HOSPEDE, COMBO ou EXTERNO. */
    val origem: String,
    val codigoReserva: String?,
    val data: String,
    val horario: String,
    val pessoas: Int,
    val cadeirinhas: Int,
    val pertoFogueira: Boolean,
    val status: String,
    val nomeContato: String?,
    val descontoHospedePct: Int
)
