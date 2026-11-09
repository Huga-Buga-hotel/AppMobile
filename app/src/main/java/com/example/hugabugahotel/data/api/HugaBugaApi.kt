package com.example.hugabugahotel.data.api

import com.example.hugabugahotel.data.model.*
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

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

    @POST("reservas/{codigo}/cupom")
    suspend fun aplicarCupom(
        @Path("codigo") codigo: String,
        @Body body: AplicarCupomRequest
    ): Response<ReservaResponse>

    @DELETE("reservas/{codigo}/cupom")
    suspend fun removerCupom(@Path("codigo") codigo: String): Response<ReservaResponse>

    @POST("reservas/{codigo}/ossos")
    suspend fun usarOssos(
        @Path("codigo") codigo: String,
        @Body body: UsarOssosRequest
    ): Response<ReservaResponse>

    @DELETE("reservas/{codigo}/ossos")
    suspend fun desfazerOssos(@Path("codigo") codigo: String): Response<ReservaResponse>

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
    suspend fun downloadVoucherPdf(
        @Path("codigo") codigo: String,
        @Header("X-Responsavel-Email") responsavelEmail: String? = null
    ): Response<ResponseBody>

    @POST("reservas/{codigo}/checkin")
    suspend fun realizarCheckin(
        @Path("codigo") codigo: String,
        @Body body: CheckinRequest
    ): Response<ReservaResponse>

    @POST("reservas/{codigo}/cancelar")
    suspend fun cancelarReserva(@Path("codigo") codigo: String): Response<ReservaResponse>

    // --- Atividades ---
    @GET("atividades")
    suspend fun getAtividades(@Query("categoria") categoria: String? = null): Response<AtividadesListResponse>

    @GET("atividades/{id}/sessoes")
    suspend fun getSessoesAtividade(
        @Path("id") atividadeId: String,
        @Query("de") de: String? = null,
        @Query("ate") ate: String? = null
    ): Response<List<SessaoAtividade>>

    @POST("reservas/{codigo}/agendamentos")
    suspend fun agendarAtividade(
        @Path("codigo") codigo: String,
        @Body body: AgendamentoRequest
    ): Response<ResponseBody>

    @DELETE("reservas/{codigo}/agendamentos/{id}")
    suspend fun cancelarAgendamento(
        @Path("codigo") codigo: String,
        @Path("id") id: String
    ): Response<ResponseBody>

    @GET("reservas/{codigo}/agenda")
    suspend fun getAgendaTribo(
        @Path("codigo") codigo: String,
        @Query("data") data: String? = null
    ): Response<AgendaDiaResponse>

    // --- Alertas de Vaga ---
    @POST("alertas-vaga")
    suspend fun criarAlertaVaga(@Body body: AlertaVagaRequest): Response<ResponseBody>

    // --- Chatbot / Xamã ---
    @POST("chat")
    suspend fun enviarMensagemChatbot(@Body body: ChatbotRequest): Response<ChatbotResponse>

    @DELETE("chat/{id_sessao}")
    suspend fun limparHistoricoChat(@Path("id_sessao") idSessao: String): Response<Unit>

    // --- Restaurante O Banquete ---
    @GET("restaurante")
    suspend fun getRestauranteStatus(): Response<RestauranteStatusResponse>

    @GET("restaurante/cardapio")
    suspend fun getCardapio(@Query("categoria") categoria: String? = null): Response<List<Prato>>

    // --- Conta / Me ---
    @GET("me")
    suspend fun getUserProfile(): Response<UserProfileResponse>

    @PATCH("me")
    suspend fun updateUserProfile(@Body body: UpdateProfileRequest): Response<UserProfileResponse>

    @GET("me/membros")
    suspend fun getMembrosTribo(): Response<List<MembroTribo>>

    @POST("me/membros")
    suspend fun adicionarMembroTribo(@Body body: MembroTribo): Response<MembroTribo>

    @GET("me/ossos")
    suspend fun getOssos(): Response<OssosResponse>

    @GET("me/reservas")
    suspend fun getMinhasReservas(@Query("aba") aba: String = "proximas"): Response<MinhasReservasResponse>

    @GET("me/pagamentos")
    suspend fun getPagamentosSalvos(): Response<List<PagamentoSalvoDto>>

    @GET("me/favoritos")
    suspend fun getCavernasFavoritas(): Response<List<CavernSummary>>

    @POST("me/favoritos/{cavernaId}")
    suspend fun adicionarFavorito(@Path("cavernaId") cavernaId: String): Response<Unit>

    @DELETE("me/favoritos/{cavernaId}")
    suspend fun removerFavorito(@Path("cavernaId") cavernaId: String): Response<Unit>

    @GET("me/notificacoes")
    suspend fun getNotificacoes(): Response<List<NotificacaoDto>>
}
