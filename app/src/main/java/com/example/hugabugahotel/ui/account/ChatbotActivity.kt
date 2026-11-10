package com.example.hugabugahotel.ui.account

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.hugabugahotel.R
import com.example.hugabugahotel.data.api.ApiClient
import com.example.hugabugahotel.data.api.ApiErrors
import com.example.hugabugahotel.data.model.ChatbotRequest
import com.example.hugabugahotel.databinding.ActivityChatbotBinding
import com.example.hugabugahotel.util.AppScope
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.util.Locale
import java.util.UUID

/**
 * Conversa com o Bugão. O app fala só com o backend (/chat), que repassa ao chatbot.
 * Cada abertura da tela é uma conversa nova (idSessao = UUID); ao sair, o histórico é apagado.
 */
class ChatbotActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatbotBinding
    private val messages = mutableListOf<ChatMessage>()
    private lateinit var adapter: ChatAdapter
    private val sessionId: String = UUID.randomUUID().toString()

    private var gravador: MediaRecorder? = null
    private var arquivoAudio: File? = null
    private var aguardando = false

    private val pedirMicrofone = registerForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) iniciarGravacao()
        else Toast.makeText(this, "Sem o microfone, escreva sua pergunta 🪨", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatbotBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.appBar.root.setOnClickListener { finish() }

        adapter = ChatAdapter(messages)
        binding.rvChatMessages.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        binding.rvChatMessages.adapter = adapter
        adicionar(ChatMessage("Uga! Eu sou o Bugão, o assistente da tribo. Pergunte sobre cavernas, preços, " +
                "atividades, o Banquete ou a sua reserva.", false))

        binding.btnSendMessage.setOnClickListener { enviarTexto() }
        binding.etMessageInput.setOnEditorActionListener { _, acao, _ ->
            if (acao == EditorInfo.IME_ACTION_SEND) { enviarTexto(); true } else false
        }
        binding.btnMic.setOnClickListener {
            when {
                gravador != null -> pararEEnviarAudio()
                aguardando -> Unit
                ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
                        PackageManager.PERMISSION_GRANTED -> iniciarGravacao()
                else -> pedirMicrofone.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        descartarGravacao() // app em segundo plano: não deixa o microfone ligado
    }

    override fun onDestroy() {
        super.onDestroy()
        // lifecycleScope já foi cancelado aqui: o AppScope garante que o DELETE saia mesmo com a tela fechada
        if (isFinishing) {
            val api = ApiClient.getApi(applicationContext)
            AppScope.launch { runCatching { api.limparHistoricoChat(sessionId) } }
        }
    }

    private fun enviarTexto() {
        val texto = binding.etMessageInput.text.toString().trim()
        if (texto.isEmpty() || aguardando) return
        binding.etMessageInput.setText("")
        adicionar(ChatMessage(texto, true))
        conversar {
            val r = ApiClient.getApi(this).enviarMensagemChatbot(ChatbotRequest(texto, sessionId))
            val corpo = r.body()
            if (r.isSuccessful && corpo != null) corpo.resposta else ApiErrors.mensagem(r)
        }
    }

    private fun iniciarGravacao() {
        val arquivo = File(cacheDir, "mensagem-chat.m4a")
        try {
            // AAC 16 kHz mono: arquivo pequeno e no formato que o Whisper entende bem
            gravador = (if (Build.VERSION.SDK_INT >= 31) MediaRecorder(this) else @Suppress("DEPRECATION") MediaRecorder()).apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(16_000)
                setAudioChannels(1)
                setMaxDuration(120_000)
                setOutputFile(arquivo.absolutePath)
                setOnInfoListener { _, what, _ ->
                    if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) pararEEnviarAudio()
                }
                prepare()
                start()
            }
            arquivoAudio = arquivo
            binding.btnMic.setImageResource(R.drawable.ic_stop)
            binding.btnMic.setBackgroundResource(R.drawable.bg_chip_selected)
            binding.etMessageInput.hint = "Gravando... toque em ■ para enviar"
        } catch (e: Exception) {
            descartarGravacao()
            Toast.makeText(this, "Não deu para usar o microfone agora.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun pararEEnviarAudio() {
        val arquivo = arquivoAudio
        val ok = runCatching { gravador?.stop() }.isSuccess // stop() falha se a gravação foi curta demais
        liberarGravador()
        if (!ok || arquivo == null || !arquivo.exists() || arquivo.length() == 0L) {
            Toast.makeText(this, "Áudio muito curto. Segure um pouco mais 🎙️", Toast.LENGTH_SHORT).show()
            return
        }

        val bolha = ChatMessage("🎙️ Mensagem de voz", true)
        adicionar(bolha)
        conversar(aoTranscrever = { texto ->
            val i = messages.indexOf(bolha)
            if (i >= 0) { messages[i] = ChatMessage("🎙️ $texto", true); adapter.notifyItemChanged(i) }
        }) {
            val parte = MultipartBody.Part.createFormData("audio", "mensagem.m4a",
                arquivo.asRequestBody("audio/mp4".toMediaType()))
            val texto = "text/plain".toMediaType()
            val idioma = Locale.getDefault().language.takeIf { it.length == 2 }?.toRequestBody(texto)
            val r = ApiClient.getApi(this).enviarAudioChatbot(parte, sessionId.toRequestBody(texto), idioma)
            val corpo = r.body()
            if (r.isSuccessful && corpo != null) {
                transcricao = corpo.transcricao
                corpo.resposta
            } else ApiErrors.mensagem(r)
        }
    }

    // A transcrição chega junto com a resposta; guardada aqui para atualizar a bolha do usuário
    private var transcricao: String? = null

    /** Mostra "pensando...", faz a chamada e troca pelo resultado (resposta ou erro da API). */
    private fun conversar(aoTranscrever: ((String) -> Unit)? = null, chamada: suspend () -> String) {
        aguardando = true
        binding.btnSendMessage.isEnabled = false
        val pensando = ChatMessage("Bugão está pensando... 🔥", false)
        adicionar(pensando)

        lifecycleScope.launch {
            transcricao = null
            val resposta = try {
                chamada()
            } catch (e: Exception) {
                ApiErrors.mensagem(e)
            } finally {
                arquivoAudio?.delete()
                arquivoAudio = null
            }
            transcricao?.let { aoTranscrever?.invoke(it) }

            val i = messages.indexOf(pensando)
            if (i >= 0) { messages[i] = ChatMessage(resposta, false); adapter.notifyItemChanged(i) }
            else adicionar(ChatMessage(resposta, false))
            binding.rvChatMessages.scrollToPosition(messages.size - 1)

            aguardando = false
            binding.btnSendMessage.isEnabled = true
        }
    }

    private fun adicionar(msg: ChatMessage) {
        messages.add(msg)
        adapter.notifyItemInserted(messages.size - 1)
        binding.rvChatMessages.scrollToPosition(messages.size - 1)
    }

    private fun descartarGravacao() {
        if (gravador == null) return
        runCatching { gravador?.stop() }
        liberarGravador()
        arquivoAudio?.delete()
        arquivoAudio = null
    }

    private fun liberarGravador() {
        gravador?.release()
        gravador = null
        binding.btnMic.setImageResource(R.drawable.ic_mic)
        binding.btnMic.setBackgroundResource(R.drawable.bg_chip)
        binding.etMessageInput.hint = "Pergunte ao Bugão..."
    }
}
