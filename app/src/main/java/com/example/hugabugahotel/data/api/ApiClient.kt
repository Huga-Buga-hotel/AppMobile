package com.example.hugabugahotel.data.api

import android.content.Context
import com.example.hugabugahotel.BuildConfig
import com.example.hugabugahotel.data.model.LoginResponse
import com.example.hugabugahotel.data.model.RefreshRequest
import com.google.gson.Gson
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Cliente HTTP da API. A URL base vem do build (BuildConfig.API_BASE_URL, ver app/build.gradle.kts):
 *  - debug:   http://10.0.2.2:8080/api/v1/ (o "localhost" do computador visto pelo emulador);
 *  - release: a URL pública do backend na AWS (propriedade hugabuga.apiUrl).
 * Dá para trocar em tempo de execução com setBaseUrl (ex.: celular físico na mesma rede Wi-Fi).
 */
object ApiClient {
    private val DEFAULT_BASE_URL: String = BuildConfig.API_BASE_URL
    private var baseUrl: String = DEFAULT_BASE_URL

    @Volatile
    private var apiService: HugaBugaApi? = null

    fun setBaseUrl(newBaseUrl: String, context: Context? = null) {
        val formattedUrl = if (newBaseUrl.endsWith("/")) newBaseUrl else "$newBaseUrl/"
        if (baseUrl != formattedUrl) {
            baseUrl = formattedUrl
            apiService = null
            context?.let { SessionManager(it).saveBaseUrl(formattedUrl) }
        }
    }

    fun getBaseUrl(): String = baseUrl

    /** Raiz do servidor (sem /api/v1/), para montar URLs relativas como as fotos das avaliações. */
    fun getServerRoot(): String = baseUrl.substringBefore("/api/")

    fun getApi(context: Context): HugaBugaApi {
        val appContext = context.applicationContext
        val savedUrl = SessionManager(appContext).getBaseUrl()
        if (savedUrl != null && savedUrl != baseUrl) {
            baseUrl = savedUrl
            apiService = null
        }

        return apiService ?: synchronized(this) {
            apiService ?: createApiService(appContext).also { apiService = it }
        }
    }

    private fun createApiService(appContext: Context): HugaBugaApi {
        val sessionManager = SessionManager(appContext)

        val authInterceptor = Interceptor { chain ->
            val requestBuilder = chain.request().newBuilder()
            sessionManager.getAccessToken()?.let { requestBuilder.header("Authorization", "Bearer $it") }
            // Reserva feita sem conta: o backend reconhece o dono pelo e-mail do responsável
            sessionManager.getResponsavelEmail()?.let { requestBuilder.header("X-Responsavel-Email", it) }
            chain.proceed(requestBuilder.build())
        }

        val builder = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .authenticator(TokenAuthenticator(sessionManager))
            .connectTimeout(15, TimeUnit.SECONDS)
            // uploads de fotos (até 5 x 25 MB) e o PDF do voucher podem demorar
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)

        if (BuildConfig.DEBUG) {
            // Nunca BODY: o corpo do pagamento leva número do cartão e CVV.
            builder.addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
        }

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(builder.build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(HugaBugaApi::class.java)
    }

    /**
     * O access token dura 15 min. Quando a API responde 401, tenta uma vez renovar com o refresh token
     * (POST /auth/refresh) e repete a chamada. Se não der, encerra a sessão local.
     */
    private class TokenAuthenticator(private val session: SessionManager) : Authenticator {
        private val gson = Gson()

        override fun authenticate(route: Route?, response: Response): Request? {
            val refresh = session.getRefreshToken() ?: return null
            if (response.request.url.encodedPath.endsWith("/auth/refresh")) return null
            if (response.priorResponse != null) { // já tentamos renovar e não deu
                session.clearAuth()
                return null
            }
            synchronized(this) {
                val tokenUsado = response.request.header("Authorization")?.removePrefix("Bearer ")
                val atual = session.getAccessToken()
                // outra requisição já renovou enquanto esperávamos
                if (atual != null && atual != tokenUsado) {
                    return response.request.newBuilder().header("Authorization", "Bearer $atual").build()
                }
                val novo = renovar(refresh) ?: run { session.clearAuth(); return null }
                session.updateTokens(novo.accessToken, novo.refreshToken)
                return response.request.newBuilder().header("Authorization", "Bearer ${novo.accessToken}").build()
            }
        }

        private fun renovar(refresh: String): LoginResponse? = try {
            val body = gson.toJson(RefreshRequest(refresh)).toRequestBody("application/json".toMediaType())
            val req = Request.Builder().url(baseUrl + "auth/refresh").post(body).build()
            OkHttpClient().newCall(req).execute().use { r ->
                if (r.isSuccessful) gson.fromJson(r.body?.string(), LoginResponse::class.java) else null
            }
        } catch (_: Exception) {
            null
        }
    }
}
