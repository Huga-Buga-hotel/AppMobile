package com.example.hugabugahotel.data.api

import com.example.hugabugahotel.data.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

/**
 * Rotas da API do backend (base: .../api/v1/). A referência completa está no Swagger do backend
 * (/api/v1/docs). Valores de dinheiro em centavos; datas em ISO (yyyy-MM-dd).
 */
interface HugaBugaApi {

    // --- Auth ---
    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): Response<LoginResponse>

    @POST("auth/cadastro")
    suspend fun cadastro(@Body body: CadastroRequest): Response<LoginResponse>

    @POST("auth/refresh")
    suspend fun refresh(@Body body: RefreshRequest): Response<LoginResponse>

    @POST("auth/logout")
    suspend fun logout(@Body body: RefreshRequest): Response<Unit>

    @POST("auth/esqueci-senha")
    suspend fun esqueciSenha(@Body body: EsqueciSenhaRequest): Response<EsqueciSenhaResponse>

    @POST("auth/redefinir-senha")
    suspend fun redefinirSenha(@Body body: RedefinirSenhaRequest): Response<Unit>

    // --- Cavernas ---
    @GET("cavernas")
    suspend fun getCavernas(
        @Query("checkIn") checkIn: String? = null,
        @Query("checkOut") checkOut: String? = null,
        @Query("adultos") adultos: Int? = null,
        @Query("idadesCriancas") idadesCriancas: List<Int>? = null,
        @Query("categoria") categoria: String? = null,
        @Query("ordem") ordem: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 10
    ): Response<CavernListResponse>

    @GET("cavernas/{slug}")
    suspend fun getCavernaDetail(@Path("slug") slug: String): Response<CavernDetailResponse>

    @GET("cavernas/{id}/cotacao")
    suspend fun getCotacao(
        @Path("id") cavernaId: String,
        @Query("checkIn") checkIn: String,
        @Query("checkOut") checkOut: String,
        @Query("adultos") adultos: Int,
        @Query("idadesCriancas") idadesCriancas: List<Int>? = null
    ): Response<CotacaoResponse>

    @GET("cavernas/{id}/disponibilidade")
    suspend fun getDisponibilidade(
        @Path("id") cavernaId: String,
        @Query("de") de: String? = null,
        @Query("ate") ate: String? = null
    ): Response<List<DiaDisponibilidade>>

    @GET("cavernas/{id}/relatos")
    suspend fun getRelatos(@Path("id") cavernaId: String, @Query("page") page: Int = 0): Response<PaginaRelatos>

    @GET("comodidades")
    suspend fun getComodidades(): Response<List<ComodidadeItem>>

    // --- Reservas & Checkout ---
    @POST("reservas")
    suspend fun criarReserva(@Body body: CriarReservaRequest): Response<ReservaResponse>

    @PUT("reservas/{codigo}/hospedes")
    suspend fun atualizarHospedes(
        @Path("codigo") codigo: String,
        @Body body: HospedesRequest
    ): Response<ReservaResponse>

    @POST("reservas/{codigo}/prorrogar")
    suspend fun prorrogarReserva(@Path("codigo") codigo: String): Response<ReservaResponse>

    @GET("extras")
    suspend fun getExtras(): Response<List<ExtraItem>>

    @PUT("reservas/{codigo}/extras")
    suspend fun atualizarExtras(
        @Path("codigo") codigo: String,
        @Body body: AdicionarExtrasRequest
    ): Response<ReservaResponse>

    @GET("restaurante/combos")
    suspend fun getCombos(): Response<List<ComboDto>>

    @PUT("reservas/{codigo}/combo")
    suspend fun definirCombo(@Path("codigo") codigo: String, @Body body: ComboRequest): Response<ReservaResponse>

    @DELETE("reservas/{codigo}/combo")
    suspend fun removerCombo(@Path("codigo") codigo: String): Response<ReservaResponse>

    @POST("reservas/{codigo}/cupom")
    suspend fun aplicarCupom(
        @Path("codigo") codigo: String,
        @Body body: AplicarCupomRequest
    ): Response<ReservaResponse>

    @DELETE("reservas/{codigo}/cupom")
    suspend fun removerCupom(@Path("codigo") codigo: String): Response<ReservaResponse>

    @POST("reservas/{codigo}/pagamentos")
    suspend fun realizarPagamento(
        @Path("codigo") codigo: String,
        @Body body: PagamentoRequest
    ): Response<PagamentoResponse>

    @GET("pagamentos/{id}")
    suspend fun getStatusPagamento(@Path("id") id: String): Response<PagamentoResponse>

    @GET("reservas/{codigo}")
    suspend fun getVoucherReserva(@Path("codigo") codigo: String): Response<ReservaResponse>

    @GET("reservas/{codigo}/voucher.pdf")
    @Streaming
    suspend fun downloadVoucherPdf(@Path("codigo") codigo: String): Response<ResponseBody>

    @POST("reservas/{codigo}/checkin")
    suspend fun realizarCheckin(@Path("codigo") codigo: String): Response<ReservaResponse>

    @POST("reservas/{codigo}/cancelar")
    suspend fun cancelarReserva(@Path("codigo") codigo: String): Response<CancelarResponse>

    /** Avaliação: multipart com nota (1 a 5), texto e até 5 fotos de até 25 MB. */
    @Multipart
    @POST("reservas/{codigo}/relato")
    suspend fun enviarAvaliacao(
        @Path("codigo") codigo: String,
        @Part("nota") nota: RequestBody,
        @Part("texto") texto: RequestBody?,
        @Part fotos: List<MultipartBody.Part>
    ): Response<RelatoDto>

    // --- Atividades ---
    @GET("atividades")
    suspend fun getAtividades(@Query("categoria") categoria: String? = null): Response<AtividadesListResponse>

    @GET("atividades/{id}/sessoes")
    suspend fun getSessoesAtividade(
        @Path("id") atividadeId: String,
        @Query("de") de: String? = null,
        @Query("ate") ate: String? = null
    ): Response<List<SessaoAtividade>>

    @GET("reservas/{codigo}/agendamentos")
    suspend fun getAgendamentos(@Path("codigo") codigo: String): Response<List<AgendamentoDto>>

    @POST("reservas/{codigo}/agendamentos")
    suspend fun agendarAtividade(
        @Path("codigo") codigo: String,
        @Body body: AgendamentoRequest
    ): Response<AgendamentoDto>

    @DELETE("reservas/{codigo}/agendamentos/{id}")
    suspend fun cancelarAgendamento(
        @Path("codigo") codigo: String,
        @Path("id") id: String
    ): Response<Unit>

    @GET("reservas/{codigo}/agenda")
    suspend fun getAgendaTribo(
        @Path("codigo") codigo: String,
        @Query("data") data: String? = null
    ): Response<AgendaDiaResponse>

    @POST("alertas-vaga")
    suspend fun criarAlertaVaga(@Body body: AlertaVagaRequest): Response<Unit>

    // --- Chatbot / Xamã (contrato provisório, ver ChatDtos) ---
    @POST("chat")
    suspend fun enviarMensagemChatbot(@Body body: ChatbotRequest): Response<ChatbotResponse>

    @DELETE("chat/{id_sessao}")
    suspend fun limparHistoricoChat(@Path("id_sessao") idSessao: String): Response<Unit>

    // --- Restaurante O Banquete ---
    @GET("restaurante")
    suspend fun getRestauranteStatus(): Response<RestauranteStatusResponse>

    @GET("restaurante/cardapio")
    suspend fun getCardapio(@Query("categoria") categoria: String? = null): Response<List<Prato>>

    @GET("restaurante/horarios")
    suspend fun getHorariosMesa(@Query("data") data: String, @Query("pessoas") pessoas: Int): Response<List<MesaHorario>>

    @POST("restaurante/reservas-mesa")
    suspend fun reservarMesa(@Body body: ReservaMesaRequest): Response<MesaResponse>

    @DELETE("restaurante/reservas-mesa/{codigo}")
    suspend fun cancelarMesa(@Path("codigo") codigo: String): Response<MesaResponse>

    // --- Conta / Me (exige login) ---
    @GET("me")
    suspend fun getUserProfile(): Response<UserProfileResponse>

    @PATCH("me")
    suspend fun updateUserProfile(@Body body: UpdateProfileRequest): Response<UserProfileResponse>

    @PATCH("me/senha")
    suspend fun trocarSenha(@Body body: TrocarSenhaRequest): Response<Unit>

    @GET("me/preferencias")
    suspend fun getPreferencias(): Response<PreferenciasDto>

    @PUT("me/preferencias")
    suspend fun atualizarPreferencias(@Body body: PreferenciasDto): Response<PreferenciasDto>

    @GET("me/membros")
    suspend fun getMembrosTribo(): Response<List<MembroTribo>>

    @POST("me/membros")
    suspend fun adicionarMembroTribo(@Body body: MembroTribo): Response<MembroTribo>

    @DELETE("me/membros/{id}")
    suspend fun removerMembroTribo(@Path("id") id: String): Response<Unit>

    @GET("me/reservas")
    suspend fun getMinhasReservas(@Query("aba") aba: String = "proximas"): Response<MinhasReservasResponse>

    @GET("me/pagamentos")
    suspend fun getPagamentos(): Response<List<PagamentoResumoDto>>

    @GET("me/favoritos")
    suspend fun getCavernasFavoritas(): Response<List<CavernSummary>>

    @POST("me/favoritos/{cavernaId}")
    suspend fun adicionarFavorito(@Path("cavernaId") cavernaId: String): Response<Unit>

    @DELETE("me/favoritos/{cavernaId}")
    suspend fun removerFavorito(@Path("cavernaId") cavernaId: String): Response<Unit>
}
