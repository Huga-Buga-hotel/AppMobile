package com.example.hugabugahotel.data.api

import android.content.Context
import android.content.SharedPreferences
import com.example.hugabugahotel.data.model.NotificacaoDto
import com.example.hugabugahotel.data.model.ReservaResponse
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences("hugabuga_session", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_RESPONSAVEL_EMAIL = "responsavel_email"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_BASE_URL = "base_url"
        private const val KEY_COMPLETED_BOOKINGS = "completed_bookings"
    }

    fun saveSession(accessToken: String, refreshToken: String?, userId: String?, userName: String?, userEmail: String?) {
        prefs.edit().apply {
            putString(KEY_ACCESS_TOKEN, accessToken)
            if (refreshToken != null) putString(KEY_REFRESH_TOKEN, refreshToken)
            if (userId != null) putString(KEY_USER_ID, userId)
            if (userName != null) putString(KEY_USER_NAME, userName)
            if (userEmail != null) putString(KEY_USER_EMAIL, userEmail)
            apply()
        }
    }

    fun saveResponsavelEmail(email: String) {
        prefs.edit().putString(KEY_RESPONSAVEL_EMAIL, email).apply()
    }

    fun saveLanguage(lang: String) {
        prefs.edit().putString(KEY_LANGUAGE, lang).apply()
    }

    fun saveBaseUrl(url: String) {
        prefs.edit().putString(KEY_BASE_URL, url).apply()
    }

    fun saveCompletedBooking(reserva: ReservaResponse) {
        val current = getCompletedBookings().toMutableList()
        current.removeAll { it.codigo == reserva.codigo }
        current.add(0, reserva)
        val json = Gson().toJson(current)
        prefs.edit().putString(KEY_COMPLETED_BOOKINGS, json).apply()
    }

    fun removeCompletedBooking(codigo: String) {
        val current = getCompletedBookings().toMutableList()
        current.removeAll { it.codigo == codigo }
        val json = Gson().toJson(current)
        prefs.edit().putString(KEY_COMPLETED_BOOKINGS, json).apply()
    }

    fun getCompletedBookings(): List<ReservaResponse> {
        val json = prefs.getString(KEY_COMPLETED_BOOKINGS, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<ReservaResponse>>() {}.type
            val list = Gson().fromJson<List<ReservaResponse>>(json, type) ?: emptyList()
            list.filter { 
                val st = it.status.uppercase(java.util.Locale.ROOT)
                st.contains("PENDENTE") || st.contains("AGUARDANDO") || st.contains("CONFIRMADA")
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun getBaseUrl(): String? = prefs.getString(KEY_BASE_URL, null)

    fun getAccessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)
    fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)
    fun getUserName(): String? = prefs.getString(KEY_USER_NAME, "Marina")
    fun getUserEmail(): String? = prefs.getString(KEY_USER_EMAIL, "marina@tribo.com")
    fun getResponsavelEmail(): String? = prefs.getString(KEY_RESPONSAVEL_EMAIL, getUserEmail())
    fun getLanguage(): String {
        val saved = prefs.getString(KEY_LANGUAGE, null)
        if (saved != null) return saved
        val sysLang = java.util.Locale.getDefault().language.lowercase(java.util.Locale.ROOT)
        return if (sysLang.startsWith("en")) "en" else "pt"
    }

    fun saveDefaultTribeCount(adults: Int, children: Int) {
        prefs.edit().putInt("default_adults", adults).putInt("default_children", children).apply()
    }

    fun getDefaultAdults(): Int = prefs.getInt("default_adults", -1)
    fun getDefaultChildren(): Int = prefs.getInt("default_children", -1)

    fun isLoggedIn(): Boolean = !getAccessToken().isNullOrEmpty()

    fun saveScheduledExperiences(reservaCodigo: String, agendaByDay: Map<String, Map<String, Pair<String, String>>>) {
        val json = Gson().toJson(agendaByDay)
        prefs.edit().putString("agenda_$reservaCodigo", json).apply()
    }

    fun getScheduledExperiences(reservaCodigo: String): MutableMap<String, MutableMap<String, Pair<String, String>>> {
        val json = prefs.getString("agenda_$reservaCodigo", null) ?: return mutableMapOf()
        return try {
            val type = object : TypeToken<MutableMap<String, MutableMap<String, Pair<String, String>>>>() {}.type
            Gson().fromJson(json, type) ?: mutableMapOf()
        } catch (_: Exception) {
            mutableMapOf()
        }
    }

    fun saveExtraExpenses(reservaCodigo: String, extraCentavos: Long) {
        prefs.edit().putLong("extra_cost_$reservaCodigo", extraCentavos).apply()
    }

    fun getExtraExpenses(reservaCodigo: String): Long {
        return prefs.getLong("extra_cost_$reservaCodigo", 0L)
    }

    fun addExtraExpenseToBooking(reservaCodigo: String, deltaCentavos: Long) {
        val currentExtra = getExtraExpenses(reservaCodigo)
        val newExtra = (currentExtra + deltaCentavos).coerceAtLeast(0L)
        saveExtraExpenses(reservaCodigo, newExtra)

        val bookings = getCompletedBookings().toMutableList()
        val index = bookings.indexOfFirst { it.codigo == reservaCodigo }
        if (index != -1) {
            val oldBooking = bookings[index]
            val updatedBooking = oldBooking.copy(
                totalCentavos = (oldBooking.totalCentavos + deltaCentavos).coerceAtLeast(0L)
            )
            bookings[index] = updatedBooking
            val json = Gson().toJson(bookings)
            prefs.edit().putString(KEY_COMPLETED_BOOKINGS, json).apply()
        }
    }

    fun savePaymentMethod(reservaCodigo: String, metodo: String) {
        prefs.edit().putString("pay_method_$reservaCodigo", metodo).apply()
    }

    fun getPaymentMethod(reservaCodigo: String): String {
        return prefs.getString("pay_method_$reservaCodigo", "PIX") ?: "PIX"
    }

    fun saveExperiencePaidInApp(reservaCodigo: String, paid: Boolean) {
        prefs.edit().putBoolean("exp_paid_in_app_$reservaCodigo", paid).apply()
    }

    fun isExperiencePaidInApp(reservaCodigo: String): Boolean {
        return prefs.getBoolean("exp_paid_in_app_$reservaCodigo", false)
    }

    fun saveNotification(notif: NotificacaoDto) {
        val current = getSavedNotifications().toMutableList()
        current.removeAll { it.titulo == notif.titulo && it.descricao == notif.descricao }
        current.add(0, notif)
        val json = Gson().toJson(current)
        prefs.edit().putString("saved_notifications", json).apply()
    }

    fun getSavedNotifications(): List<NotificacaoDto> {
        val json = prefs.getString("saved_notifications", null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<NotificacaoDto>>() {}.type
            Gson().fromJson(json, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }
}
