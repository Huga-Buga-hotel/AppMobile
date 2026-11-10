package com.example.hugabugahotel.data.api

import android.content.Context
import android.content.SharedPreferences
import com.example.hugabugahotel.data.model.NotificacaoDto
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Estado local do app: tokens, e-mail do responsável (reserva sem conta), idioma, URL da API,
 * códigos das reservas feitas sem conta e notificações locais. Reservas, agendamentos e pagamentos
 * vivem no backend: nunca são simulados aqui.
 */
class SessionManager(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("hugabuga_session", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_ROLE = "user_role"
        private const val KEY_RESPONSAVEL_EMAIL = "responsavel_email"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_BASE_URL = "base_url"
        private const val KEY_GUEST_BOOKINGS = "guest_bookings"
        private const val KEY_NOTIFICATIONS = "saved_notifications"
    }

    // ---------- sessão ----------

    fun saveSession(accessToken: String, refreshToken: String?, userId: String?, userName: String?,
                    userEmail: String?, role: String? = null) {
        prefs.edit().apply {
            putString(KEY_ACCESS_TOKEN, accessToken)
            refreshToken?.let { putString(KEY_REFRESH_TOKEN, it) }
            userId?.let { putString(KEY_USER_ID, it) }
            userName?.let { putString(KEY_USER_NAME, it) }
            userEmail?.let { putString(KEY_USER_EMAIL, it) }
            role?.let { putString(KEY_USER_ROLE, it) }
            remove(KEY_RESPONSAVEL_EMAIL) // logado, o dono é reconhecido pelo token
            apply()
        }
    }

    fun updateTokens(accessToken: String, refreshToken: String?) {
        prefs.edit().putString(KEY_ACCESS_TOKEN, accessToken).apply {
            refreshToken?.let { putString(KEY_REFRESH_TOKEN, it) }
        }.apply()
    }

    /** Sai da conta, mas mantém idioma, URL e notificações. */
    fun clearAuth() {
        prefs.edit()
            .remove(KEY_ACCESS_TOKEN).remove(KEY_REFRESH_TOKEN).remove(KEY_USER_ID)
            .remove(KEY_USER_NAME).remove(KEY_USER_EMAIL).remove(KEY_USER_ROLE)
            .apply()
    }

    fun clearSession() = prefs.edit().clear().apply()

    fun getAccessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)
    fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)
    fun getUserName(): String? = prefs.getString(KEY_USER_NAME, null)
    fun getUserEmail(): String? = prefs.getString(KEY_USER_EMAIL, null)
    fun getRole(): String? = prefs.getString(KEY_USER_ROLE, null)
    fun isLoggedIn(): Boolean = !getAccessToken().isNullOrEmpty()

    // ---------- reserva sem conta ----------

    /** E-mail do responsável, enviado no header X-Responsavel-Email quando não há login. */
    fun saveResponsavelEmail(email: String) {
        prefs.edit().putString(KEY_RESPONSAVEL_EMAIL, email.trim().lowercase()).apply()
    }

    fun getResponsavelEmail(): String? =
        if (isLoggedIn()) null else prefs.getString(KEY_RESPONSAVEL_EMAIL, null)

    /** Reservas feitas sem conta não aparecem em /me/reservas: guardamos só os códigos para consultar a API. */
    fun addGuestBooking(codigo: String) {
        val atual = getGuestBookings().toMutableList()
        atual.remove(codigo)
        atual.add(0, codigo)
        prefs.edit().putString(KEY_GUEST_BOOKINGS, gson.toJson(atual)).apply()
    }

    fun getGuestBookings(): List<String> {
        val json = prefs.getString(KEY_GUEST_BOOKINGS, null) ?: return emptyList()
        return try {
            gson.fromJson(json, object : TypeToken<List<String>>() {}.type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    // ---------- preferências locais ----------

    fun saveLanguage(lang: String) = prefs.edit().putString(KEY_LANGUAGE, lang).apply()

    fun getLanguage(): String {
        prefs.getString(KEY_LANGUAGE, null)?.let { return it }
        val sysLang = java.util.Locale.getDefault().language.lowercase(java.util.Locale.ROOT)
        return if (sysLang.startsWith("en")) "en" else "pt"
    }

    fun saveBaseUrl(url: String) = prefs.edit().putString(KEY_BASE_URL, url).apply()
    fun getBaseUrl(): String? = prefs.getString(KEY_BASE_URL, null)

    fun saveDefaultTribeCount(adults: Int, children: Int) {
        prefs.edit().putInt("default_adults", adults).putInt("default_children", children).apply()
    }

    fun getDefaultAdults(): Int = prefs.getInt("default_adults", -1)
    fun getDefaultChildren(): Int = prefs.getInt("default_children", -1)

    // ---------- notificações locais ----------

    fun saveNotification(notif: NotificacaoDto) {
        val current = getSavedNotifications().toMutableList()
        current.removeAll { it.titulo == notif.titulo && it.descricao == notif.descricao }
        current.add(0, notif)
        prefs.edit().putString(KEY_NOTIFICATIONS, gson.toJson(current.take(50))).apply()
    }

    fun getSavedNotifications(): List<NotificacaoDto> {
        val json = prefs.getString(KEY_NOTIFICATIONS, null) ?: return emptyList()
        return try {
            gson.fromJson(json, object : TypeToken<List<NotificacaoDto>>() {}.type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }
}
