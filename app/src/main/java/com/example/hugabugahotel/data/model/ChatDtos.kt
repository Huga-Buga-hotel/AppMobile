package com.example.hugabugahotel.data.model

import com.google.gson.annotations.SerializedName

// Contrato provisório do chatbot (Xamã). Será alinhado ao repositório do chatbot.

data class ChatbotRequest(
    val mensagem: String,
    @SerializedName("id_sessao")
    val idSessao: String
)

data class ChatbotResponse(
    val resposta: String? = null,
    val mensagem: String? = null
) {
    val displayResposta: String
        get() = resposta ?: mensagem ?: "Ugh! A Xamã ouviu sua mensagem."
}
