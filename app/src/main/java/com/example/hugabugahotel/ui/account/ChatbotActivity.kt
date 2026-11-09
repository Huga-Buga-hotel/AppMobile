package com.example.hugabugahotel.ui.account

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.hugabugahotel.data.api.ApiClient
import com.example.hugabugahotel.data.model.ChatbotRequest
import com.example.hugabugahotel.databinding.ActivityChatbotBinding
import kotlinx.coroutines.launch
import java.util.UUID

class ChatbotActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatbotBinding
    private val messages = mutableListOf<ChatMessage>()
    private lateinit var adapter: ChatAdapter
    private val sessionId: String = UUID.randomUUID().toString()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatbotBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.appBar.root.setOnClickListener {
            finish()
        }

        messages.add(ChatMessage("Ugh! Sou a Xamã, o oráculo da tribo. Como posso ajudar na sua estadia na caverna hoje?", false))

        adapter = ChatAdapter(messages)
        binding.rvChatMessages.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        binding.rvChatMessages.adapter = adapter

        binding.btnSendMessage.setOnClickListener {
            val text = binding.etMessageInput.text.toString().trim()
            if (text.isNotEmpty()) {
                sendMessage(text)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Apaga o histórico da conversa quando o usuário fecha o chat conforme especificação DELETE /chat/{id_sessao}
        lifecycleScope.launch {
            try {
                val api = ApiClient.getApi(this@ChatbotActivity)
                api.limparHistoricoChat(sessionId)
            } catch (_: Exception) {}
        }
    }

    private fun sendMessage(text: String) {
        messages.add(ChatMessage(text, true))
        adapter.notifyItemInserted(messages.size - 1)
        binding.rvChatMessages.scrollToPosition(messages.size - 1)
        binding.etMessageInput.setText("")

        lifecycleScope.launch {
            val responseText = try {
                val api = ApiClient.getApi(this@ChatbotActivity)
                val response = api.enviarMensagemChatbot(ChatbotRequest(text, sessionId))
                if (response.isSuccessful && response.body() != null) {
                    response.body()!!.displayResposta
                } else {
                    "Ugh! O bot está temporariamente indisponível (502). Por favor, aguarde até o seu retorno."
                }
            } catch (_: Exception) {
                "Ugh! O bot está temporariamente indisponível. Por favor, aguarde até o seu retorno."
            }

            messages.add(ChatMessage(responseText, false))
            adapter.notifyItemInserted(messages.size - 1)
            binding.rvChatMessages.scrollToPosition(messages.size - 1)
        }
    }
}
