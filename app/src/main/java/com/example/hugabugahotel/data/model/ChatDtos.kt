package com.example.hugabugahotel.data.model

// Chatbot (Bugão). O app fala só com o backend (POST /chat, POST /chat/audio, DELETE /chat/{idSessao});
// o backend repassa para a API do chatbot (repositório chatbot-huga-buga-hotel), que fica numa rede privada.
// Espelha backend dto/ChatDtos.java.

/** idSessao: UUID gerado pelo app a cada conversa aberta (o backend separa por conta/visitante). */
data class ChatbotRequest(
    val mensagem: String,
    val idSessao: String
)

data class ChatbotResponse(
    val resposta: String
)

/** Mensagem de voz: o texto reconhecido vira a bolha do usuário. */
data class ChatbotAudioResponse(
    val transcricao: String,
    val resposta: String
)
