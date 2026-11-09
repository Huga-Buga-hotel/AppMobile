package com.example.hugabugahotel.data.api

import android.content.Context
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    // IP do computador na rede local onde o backend está rodando (porta 8080)
    // Para emulador: "http://10.0.2.2:8080/api/v1/"
    // Para celular físico na mesma rede Wi-Fi: "http://192.168.1.10:8080/api/v1/"
    private const val DEFAULT_BASE_URL = "http://localhost:8080/api/v1/"
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

            // Token de Autenticação se logado
            val token = sessionManager.getAccessToken()
            if (!token.isNullOrEmpty()) {
                requestBuilder.addHeader("Authorization", "Bearer $token")
            }

            // Email do Responsável para operações de reserva sem conta
            val responsavelEmail = sessionManager.getResponsavelEmail()
            if (!responsavelEmail.isNullOrEmpty()) {
                requestBuilder.addHeader("X-Responsavel-Email", responsavelEmail)
            }

            chain.proceed(requestBuilder.build())
        }

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(HugaBugaApi::class.java)
    }
}
