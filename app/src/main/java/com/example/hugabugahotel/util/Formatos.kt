package com.example.hugabugahotel.util

import java.text.NumberFormat
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Formatação única para o app inteiro: dinheiro vem da API em centavos e datas em ISO. */
object Formatos {
    private val BR = Locale.forLanguageTag("pt-BR")
    private val moeda = NumberFormat.getCurrencyInstance(BR)
    private val diaMes = DateTimeFormatter.ofPattern("d MMM", BR)
    private val diaMesAno = DateTimeFormatter.ofPattern("dd/MM/yyyy", BR)
    private val hora = DateTimeFormatter.ofPattern("HH:mm", BR)

    /** 156000 -> "R$ 1.560,00". */
    fun reais(centavos: Long): String = moeda.format(centavos / 100.0)

    /** "2026-12-10" -> "10 dez". Devolve o texto original se não for uma data ISO. */
    fun dataCurta(iso: String?): String = parseData(iso)?.format(diaMes)?.replace(".", "") ?: iso.orEmpty()

    fun dataCompleta(iso: String?): String = parseData(iso)?.format(diaMesAno) ?: iso.orEmpty()

    /** "2026-12-02T09:00:00-03:00" ou "09:00:00" -> "09:00". */
    fun hora(valor: String?): String {
        if (valor.isNullOrBlank()) return ""
        return try {
            OffsetDateTime.parse(valor).atZoneSameInstant(java.time.ZoneId.of("America/Sao_Paulo")).format(hora)
        } catch (_: Exception) {
            valor.substringAfter("T").take(5)
        }
    }

    fun parseData(iso: String?): LocalDate? = try {
        if (iso.isNullOrBlank()) null else LocalDate.parse(iso.take(10))
    } catch (_: Exception) {
        null
    }

    fun iso(data: LocalDate): String = data.toString()

    /** Status da API em texto amigável. */
    fun status(status: String): String = when (status.uppercase(Locale.ROOT)) {
        "PENDENTE" -> "Aguardando checkout"
        "AGUARDANDO_PAGAMENTO" -> "Aguardando pagamento"
        "CONFIRMADA" -> "Confirmada"
        "CONCLUIDA" -> "Concluída"
        "CANCELADA" -> "Cancelada"
        "EXPIRADA" -> "Expirada"
        "NAO_COMPARECEU" -> "Não compareceu"
        "APROVADO" -> "Aprovado"
        "RECUSADO" -> "Recusado"
        "EXPIRADO" -> "Expirado"
        else -> status
    }
}
