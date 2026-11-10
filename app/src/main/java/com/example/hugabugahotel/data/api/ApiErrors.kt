package com.example.hugabugahotel.data.api

import com.example.hugabugahotel.data.model.ErroResposta
import com.google.gson.Gson
import retrofit2.Response
import java.io.IOException

/**
 * Converte as respostas de erro do backend ({status, codigo, titulo, mensagem, ref}) em texto para o usuário.
 * As mensagens do backend já dizem o que aconteceu, o que NÃO aconteceu e o próximo passo.
 */
object ApiErrors {
    private val gson = Gson()
    // errorBody() só pode ser lido uma vez: guarda o que já foi lido para mensagem() e codigo() funcionarem juntos
    private val lidos = java.util.Collections.synchronizedMap(java.util.WeakHashMap<Response<*>, ErroResposta?>())

    fun parse(response: Response<*>): ErroResposta? {
        if (lidos.containsKey(response)) return lidos[response]
        val erro = try {
            response.errorBody()?.charStream()?.use { gson.fromJson(it, ErroResposta::class.java) }
        } catch (_: Exception) {
            null
        }
        lidos[response] = erro
        return erro
    }

    /** Ex.: "Ugh! O pagamento não passou. O banco recusou o cartão final 0002 (...)". */
    fun mensagem(response: Response<*>): String {
        val erro = parse(response) ?: return "Erro ${response.code()} ao falar com a caverna central. Tente de novo."
        val campos = erro.campos.orEmpty().joinToString("\n") { "• ${it.campo}: ${it.erro}" }
        return listOf(erro.titulo, erro.mensagem, campos).filter { it.isNotBlank() }.joinToString("\n")
    }

    /** Código do erro (VALIDACAO, SEM_DISPONIBILIDADE, PAGAMENTO_RECUSADO...) ou null. */
    fun codigo(response: Response<*>): String? = parse(response)?.codigo

    fun mensagem(e: Throwable): String = when (e) {
        is IOException -> "Não conseguimos falar com a caverna central. Confira sua internet e tente de novo."
        else -> "Algo deu errado: ${e.message ?: e.javaClass.simpleName}"
    }
}
