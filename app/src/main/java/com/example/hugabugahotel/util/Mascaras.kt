package com.example.hugabugahotel.util

import android.text.Editable
import android.text.Selection
import android.text.TextWatcher
import android.widget.EditText

/**
 * Máscaras de digitação: o usuário digita só números e o campo se formata sozinho.
 * Os separadores só aparecem depois do dígito seguinte, então o backspace funciona normalmente.
 */
object Mascaras {

    /** "12345678901" -> "123.456.789-01" */
    fun cpf(texto: String): String {
        val d = texto.filter { it.isDigit() }.take(11)
        return buildString {
            d.forEachIndexed { i, c ->
                when (i) {
                    3, 6 -> append('.')
                    9 -> append('-')
                }
                append(c)
            }
        }
    }

    /** "54999998888" -> "(54) 99999-8888"; com 10 dígitos (fixo) -> "(54) 3333-4444". */
    fun telefone(texto: String): String {
        var d = texto.filter { it.isDigit() }
        // Número salvo com DDI (+55 54 9...) vira só DDD + número
        if (d.length > 11 && d.startsWith("55")) d = d.drop(2)
        d = d.take(11)
        if (d.isEmpty()) return ""
        if (d.length <= 2) return "($d"
        val ddd = d.take(2)
        val numero = d.drop(2)
        val corte = if (d.length == 11) 5 else 4
        return if (numero.length <= corte) "($ddd) $numero"
        else "($ddd) ${numero.take(corte)}-${numero.drop(corte)}"
    }

    fun digitos(texto: CharSequence?): String = texto?.filter { it.isDigit() }?.toString().orEmpty()

    fun EditText.mascaraCpf() = addTextChangedListener(Formatador(::cpf))
    fun EditText.mascaraTelefone() = addTextChangedListener(Formatador(::telefone))

    private class Formatador(private val formatar: (String) -> String) : TextWatcher {
        private var editando = false

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

        override fun afterTextChanged(s: Editable) {
            if (editando) return
            val atual = s.toString()
            val formatado = formatar(atual)
            if (formatado == atual) return

            // Mantém o cursor depois do mesmo dígito em que estava
            val cursor = Selection.getSelectionEnd(s).coerceIn(0, atual.length)
            val digitosAntes = atual.take(cursor).count { it.isDigit() }
            var pos = 0
            var vistos = 0
            while (pos < formatado.length && vistos < digitosAntes) {
                if (formatado[pos].isDigit()) vistos++
                pos++
            }

            editando = true
            s.replace(0, s.length, formatado)
            editando = false
            Selection.setSelection(s, pos.coerceAtMost(s.length))
        }
    }
}
