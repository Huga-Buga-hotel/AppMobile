package com.example.hugabugahotel.ui.account

import android.app.AlertDialog
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.hugabugahotel.R
import com.example.hugabugahotel.data.api.ApiClient
import com.example.hugabugahotel.data.api.ApiErrors
import com.example.hugabugahotel.data.api.SessionManager
import com.example.hugabugahotel.databinding.ActivityMyBookingsBinding
import com.example.hugabugahotel.util.Formatos
import com.example.hugabugahotel.util.LanguageManager
import com.example.hugabugahotel.util.Uploads
import com.example.hugabugahotel.util.setupMainNavigation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Abas Próximas / Passadas / Canceladas.
 *  - Com conta: GET /me/reservas?aba= (o backend decide a aba e devolve a contagem de cada uma).
 *  - Sem conta: consulta cada código guardado no aparelho em GET /reservas/{codigo} (header X-Responsavel-Email).
 */
class MyBookingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMyBookingsBinding
    private lateinit var adapter: BookingsAdapter
    private var currentTab = "proximas"

    // Avaliação em andamento: fotos escolhidas na galeria
    private var reviewItem: BookingItem? = null
    private var reviewFotos: List<Uri> = emptyList()
    private var reviewFotosLabel: TextView? = null
    private val escolherFotos = registerForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(Uploads.MAX_FOTOS)
    ) { uris ->
        reviewFotos = uris
        reviewFotosLabel?.text = if (uris.isEmpty()) "Nenhuma foto" else "${uris.size} foto(s) escolhida(s)"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyBookingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.bottomNav.setupMainNavigation(this, R.id.nav_reservations)
        setupRecyclerView()
        setupTabs()
    }

    override fun onResume() {
        super.onResume()
        binding.bottomNav.selectedItemId = R.id.nav_reservations
        applyLanguage()
        fetchReservas()
    }

    private fun applyLanguage() {
        binding.tvTitleBookings.text = LanguageManager.get(this, "bookings_title")
        binding.tvEmptyState.text = LanguageManager.get(this, "empty_bookings")
        atualizarRotulos(emptyMap())
        LanguageManager.updateBottomNavLanguage(this, binding.bottomNav)
    }

    private fun atualizarRotulos(contagem: Map<String, Int>) {
        fun rotulo(chave: String, aba: String) =
            LanguageManager.get(this, chave) + (contagem[aba]?.let { " $it" } ?: "")
        binding.chipUpcoming.text = rotulo("tab_upcoming", "proximas")
        binding.chipPast.text = rotulo("tab_past", "passadas")
        binding.chipCanceled.text = rotulo("tab_canceled", "canceladas")
    }

    private fun setupRecyclerView() {
        adapter = BookingsAdapter(
            items = emptyList(),
            currentTab = currentTab,
            onCancelClick = { cancelarReserva(it) },
            onReviewClick = { abrirAvaliacao(it) }
        )
        binding.rvBookings.layoutManager = LinearLayoutManager(this)
        binding.rvBookings.adapter = adapter
    }

    private fun setupTabs() {
        val abas = mapOf(binding.chipUpcoming to "proximas", binding.chipPast to "passadas", binding.chipCanceled to "canceladas")
        abas.forEach { (chip, aba) ->
            chip.setOnClickListener {
                currentTab = aba
                abas.forEach { (c, a) ->
                    c.setBackgroundResource(if (a == aba) R.drawable.bg_chip_selected else R.drawable.bg_chip)
                    c.setTextColor(resources.getColor(if (a == aba) android.R.color.white else R.color.hb_ink, theme))
                }
                setupRecyclerView()
                fetchReservas()
            }
        }
    }

    private fun fetchReservas() {
        lifecycleScope.launch {
            val itens = try {
                if (SessionManager(this@MyBookingsActivity).isLoggedIn()) doBackend() else semConta()
            } catch (e: Exception) {
                Toast.makeText(this@MyBookingsActivity, ApiErrors.mensagem(e), Toast.LENGTH_LONG).show()
                emptyList()
            }
            adapter.updateData(itens)
            binding.tvEmptyState.visibility = if (itens.isEmpty()) View.VISIBLE else View.GONE
            binding.rvBookings.visibility = if (itens.isEmpty()) View.GONE else View.VISIBLE
        }
    }

    private suspend fun doBackend(): List<BookingItem> {
        val response = ApiClient.getApi(this).getMinhasReservas(currentTab)
        val body = response.body()
        if (!response.isSuccessful || body == null) {
            Toast.makeText(this, ApiErrors.mensagem(response), Toast.LENGTH_LONG).show()
            return emptyList()
        }
        atualizarRotulos(body.contagem)
        return body.estadias.map { BookingItem.de(it) }
    }

    /** Mesmas regras de aba do backend (ContaService.abaDa). */
    private suspend fun semConta(): List<BookingItem> {
        val api = ApiClient.getApi(this)
        val hoje = java.time.LocalDate.now()
        val reservas = coroutineScopeMap(SessionManager(this).getGuestBookings()) { codigo ->
            api.getVoucherReserva(codigo).body()
        }.filterNotNull()
        val porAba = reservas.groupBy { r ->
            val saida = Formatos.parseData(r.checkOut) ?: hoje
            when (r.status) {
                "CONFIRMADA" -> if (saida.isBefore(hoje)) "passadas" else "proximas"
                "AGUARDANDO_PAGAMENTO" -> if (saida.isBefore(hoje)) "" else "proximas"
                "CONCLUIDA" -> "passadas"
                "CANCELADA" -> "canceladas"
                else -> ""
            }
        }
        atualizarRotulos(listOf("proximas", "passadas", "canceladas").associateWith { porAba[it]?.size ?: 0 })
        return porAba[currentTab].orEmpty().sortedBy { it.checkIn }.map { BookingItem.de(it) }
    }

    /** Consulta em paralelo; um código que falhar (ex.: reserva de outro e-mail) só fica de fora. */
    private suspend fun <T, R> coroutineScopeMap(itens: List<T>, f: suspend (T) -> R?): List<R?> =
        kotlinx.coroutines.coroutineScope { itens.map { async { runCatching { f(it) }.getOrNull() } }.awaitAll() }

    private fun cancelarReserva(item: BookingItem) {
        AlertDialog.Builder(this)
            .setTitle("Cancelar reserva")
            .setMessage("Cancelar a reserva ${item.codigo} (${item.cavernaNome})?\n\n" +
                "Com 2 dias ou mais de antecedência o reembolso é integral. Depois disso não há reembolso " +
                "e, se o pagamento seria na chegada, há multa de 1 diária.")
            .setPositiveButton("Sim, cancelar") { _, _ ->
                lifecycleScope.launch {
                    try {
                        val response = ApiClient.getApi(this@MyBookingsActivity).cancelarReserva(item.codigo)
                        val body = response.body()
                        if (response.isSuccessful && body != null) {
                            val detalhe = when {
                                body.reembolsoCentavos > 0 -> "Reembolso: ${Formatos.reais(body.reembolsoCentavos)}"
                                body.multaCentavos > 0 -> "Multa a pagar na chegada: ${Formatos.reais(body.multaCentavos)}"
                                else -> "Nada foi cobrado."
                            }
                            AlertDialog.Builder(this@MyBookingsActivity).setTitle("Reserva cancelada")
                                .setMessage(detalhe).setPositiveButton("OK", null).show()
                            fetchReservas()
                        } else {
                            Toast.makeText(this@MyBookingsActivity, ApiErrors.mensagem(response), Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this@MyBookingsActivity, ApiErrors.mensagem(e), Toast.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton("Não", null)
            .show()
    }

    // ---------- avaliação (nota, comentário e até 5 fotos de 25 MB) ----------

    private fun abrirAvaliacao(item: BookingItem) {
        reviewItem = item
        reviewFotos = emptyList()
        val pad = (16 * resources.displayMetrics.density).toInt()
        val nota = RatingBar(this).apply { numStars = 5; stepSize = 1f; rating = 5f }
        val texto = EditText(this).apply { hint = "Conte como foi a estadia (opcional)"; minLines = 3 }
        val fotosLabel = TextView(this).apply { text = "Nenhuma foto"; setPadding(0, pad / 2, 0, 0) }
        reviewFotosLabel = fotosLabel
        val escolher = TextView(this).apply {
            text = "Escolher fotos (até 5, 25 MB cada)"
            setPadding(0, pad, 0, 0)
            setTextColor(resources.getColor(R.color.hb_orange, theme))
            setOnClickListener {
                escolherFotos.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
        }
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad / 2, pad, 0)
            addView(nota, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT))
            addView(texto); addView(escolher); addView(fotosLabel)
        }
        AlertDialog.Builder(this)
            .setTitle("Avaliar ${item.cavernaNome}")
            .setView(layout)
            .setPositiveButton("Enviar") { _, _ -> enviarAvaliacao(item.codigo, nota.rating.toInt(), texto.text.toString()) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun enviarAvaliacao(codigo: String, nota: Int, texto: String) {
        lifecycleScope.launch {
            try {
                val partes = withContext(Dispatchers.IO) { Uploads.partesDeFotos(this@MyBookingsActivity, reviewFotos) }
                val response = ApiClient.getApi(this@MyBookingsActivity).enviarAvaliacao(
                    codigo, Uploads.texto(nota.coerceIn(1, 5).toString()),
                    texto.takeIf { it.isNotBlank() }?.let { Uploads.texto(it) }, partes)
                if (response.isSuccessful) {
                    Toast.makeText(this@MyBookingsActivity, "Avaliação publicada! Obrigado, tribo", Toast.LENGTH_LONG).show()
                    fetchReservas()
                } else {
                    // 413 ARQUIVO_GRANDE, 400 tipo de imagem inválido, 422 já avaliada
                    Toast.makeText(this@MyBookingsActivity, ApiErrors.mensagem(response), Toast.LENGTH_LONG).show()
                }
            } catch (e: IllegalArgumentException) {
                Toast.makeText(this@MyBookingsActivity, e.message, Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(this@MyBookingsActivity, ApiErrors.mensagem(e), Toast.LENGTH_LONG).show()
            }
        }
    }
}
