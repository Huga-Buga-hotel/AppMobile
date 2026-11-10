package com.example.hugabugahotel.ui.checkout

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.hugabugahotel.R
import com.example.hugabugahotel.data.api.ApiClient
import com.example.hugabugahotel.data.api.ApiErrors
import com.example.hugabugahotel.data.model.AdicionarExtrasRequest
import com.example.hugabugahotel.data.model.ComboDto
import com.example.hugabugahotel.data.model.ComboRequest
import com.example.hugabugahotel.data.model.ExtraItem
import com.example.hugabugahotel.data.model.ReservaResponse
import com.example.hugabugahotel.data.model.SelecaoExtraDto
import com.example.hugabugahotel.databinding.ActivityCheckoutStep2Binding
import com.example.hugabugahotel.util.Formatos
import kotlinx.coroutines.launch

/**
 * Passo 2: extras e combo quarto + Banquete. Os preços mostrados são unitários; o total que vale é o
 * devolvido pela API depois de salvar (ela calcula por criança, por pessoa e por noite).
 */
class CheckoutStep2Activity : AppCompatActivity() {

    private lateinit var binding: ActivityCheckoutStep2Binding
    private var reservaCodigo: String = ""
    private var checkInDisplay: String = ""
    private var checkOutDisplay: String = ""
    private var adultsCount: Int = 2
    private var childrenCount: Int = 0
    private var diariasCentavos: Long = 0L
    private var noitesCount: Int = 1
    private var cavernName: String = ""

    private val selecionados = linkedMapOf<String, ExtraItem>()
    private var comboEscolhido: ComboDto? = null
    private var comboHorario: String? = null

    companion object {
        private val HORARIOS_JANTAR = listOf("19:00", "19:30", "20:00", "20:30", "21:00", "21:30")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCheckoutStep2Binding.inflate(layoutInflater)
        setContentView(binding.root)

        reservaCodigo = intent.getStringExtra("RESERVA_CODIGO") ?: run { finish(); return }
        checkInDisplay = intent.getStringExtra("CHECK_IN") ?: ""
        checkOutDisplay = intent.getStringExtra("CHECK_OUT") ?: ""
        adultsCount = intent.getIntExtra("ADULTS", 2)
        childrenCount = intent.getIntExtra("CHILDREN", 0)
        diariasCentavos = intent.getLongExtra("TOTAL_CENTAVOS", 0L)
        noitesCount = intent.getIntExtra("NOITES", 1).coerceAtLeast(1)
        cavernName = intent.getStringExtra("CAVERN_NOME") ?: ""

        binding.btnBack.setOnClickListener { finish() }
        binding.btnSkipExtras.setOnClickListener {
            selecionados.clear(); comboEscolhido = null; comboHorario = null
            salvarEAvancar()
        }
        binding.btnToPayment.setOnClickListener { salvarEAvancar() }
        binding.bottomBar.setOnClickListener { salvarEAvancar() }

        setupStepper()
        bindBookingSummary()
        carregarOpcoes()
    }

    private fun setupStepper() {
        val step1 = findViewById<TextView>(R.id.step1Indicator) ?: return
        val step2 = findViewById<TextView>(R.id.step2Indicator) ?: return
        val step3 = findViewById<TextView>(R.id.step3Indicator) ?: return
        val white = resources.getColor(R.color.white, theme)
        step1.setBackgroundResource(R.drawable.bg_stepper_done); step1.setTextColor(white); step1.text = "✓ 1. Quem vem"
        step2.setBackgroundResource(R.drawable.bg_stepper_active); step2.setTextColor(white); step2.text = "2. Extras"
        step3.setBackgroundResource(R.drawable.bg_chip); step3.setTextColor(resources.getColor(R.color.hb_ink, theme)); step3.text = "3. Pagamento"
    }

    /** Estimativa enquanto escolhe; o valor final vem da API ao avançar. */
    private fun precoEstimado(e: ExtraItem): Long = when (e.cobranca) {
        "POR_CRIANCA" -> e.precoCentavos * childrenCount
        "POR_PESSOA" -> e.precoCentavos * (adultsCount + childrenCount)
        else -> e.precoCentavos
    }

    private fun comboEstimado(c: ComboDto): Long =
        noitesCount * (adultsCount * c.precoAdultoCentavos + childrenCount * c.precoCriancaCentavos)

    private fun bindBookingSummary() {
        binding.root.findViewById<TextView>(R.id.tvSummaryCavernName)?.text = cavernName
        binding.root.findViewById<TextView>(R.id.tvSummaryCheckIn)?.text = checkInDisplay
        binding.root.findViewById<TextView>(R.id.tvSummaryCheckOut)?.text = checkOutDisplay
        binding.root.findViewById<TextView>(R.id.tvSummaryTribe)?.text =
            "TRIBO: ${adultsCount + childrenCount} pessoas ($adultsCount ad., $childrenCount cri.)"
        val noitesStr = if (noitesCount == 1) "noite" else "noites"
        binding.root.findViewById<TextView>(R.id.tvSummaryNightsBreakdown)?.text =
            "${Formatos.reais(diariasCentavos / noitesCount)} × $noitesCount $noitesStr"
        binding.root.findViewById<TextView>(R.id.tvSummaryNightsTotal)?.text = Formatos.reais(diariasCentavos)

        val linhas = selecionados.values.map { it.nome to precoEstimado(it) } +
            listOfNotNull(comboEscolhido?.let { "Combo ${it.nome} (${comboHorario})" to comboEstimado(it) })
        binding.root.findViewById<LinearLayout>(R.id.containerSummaryExtras)?.let { container ->
            container.removeAllViews()
            for ((nome, valor) in linhas) {
                val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, 6, 0, 0) }
                row.addView(TextView(this).apply {
                    text = nome; setTextAppearance(R.style.HB_Text); textSize = 12f
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                })
                row.addView(TextView(this).apply {
                    text = "+${Formatos.reais(valor)}"; setTextAppearance(R.style.HB_Text); textSize = 12f
                    setTypeface(null, Typeface.BOLD)
                })
                container.addView(row)
            }
        }
        val estimado = diariasCentavos + linhas.sumOf { it.second }
        binding.root.findViewById<TextView>(R.id.tvSummaryTotal)?.text = Formatos.reais(estimado)
    }

    private fun carregarOpcoes() {
        lifecycleScope.launch {
            try {
                val api = ApiClient.getApi(this@CheckoutStep2Activity)
                val extras = api.getExtras()
                val combos = api.getCombos()
                if (!extras.isSuccessful) {
                    Toast.makeText(this@CheckoutStep2Activity, ApiErrors.mensagem(extras), Toast.LENGTH_LONG).show()
                    return@launch
                }
                // o café jurássico é incluso em toda reserva: não é uma opção
                val opcoes = extras.body().orEmpty().filter { !it.incluso }
                    .filter { it.cobranca != "POR_CRIANCA" || childrenCount > 0 }
                renderOpcoes(opcoes, combos.body().orEmpty())
            } catch (e: Exception) {
                Toast.makeText(this@CheckoutStep2Activity, ApiErrors.mensagem(e), Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun renderOpcoes(extras: List<ExtraItem>, combos: List<ComboDto>) {
        binding.containerExtras.removeAllViews()
        for (extra in extras) {
            val sufixo = when (extra.cobranca) { "POR_CRIANCA" -> " / criança"; "POR_PESSOA" -> " / pessoa"; else -> "" }
            adicionarCard(extra.nome, extra.descricao.orEmpty(), Formatos.reais(extra.precoCentavos) + sufixo,
                selecionado = { selecionados.containsKey(extra.id) }) {
                if (selecionados.remove(extra.id) == null) selecionados[extra.id] = extra
                bindBookingSummary()
            }
        }
        for (combo in combos) {
            val preco = "${Formatos.reais(combo.precoAdultoCentavos)} adulto · ${Formatos.reais(combo.precoCriancaCentavos)} criança / noite"
            adicionarCard("🍖 Combo ${combo.nome}", combo.descricao.orEmpty(), preco,
                selecionado = { comboEscolhido?.id == combo.id }) { aoMudar ->
                if (comboEscolhido?.id == combo.id) {
                    comboEscolhido = null; comboHorario = null; bindBookingSummary(); aoMudar()
                } else {
                    escolherHorario { h ->
                        comboEscolhido = combo; comboHorario = h; bindBookingSummary(); renderOpcoes(extras, combos)
                    }
                }
            }
        }
    }

    /** Card de opção com botão alternável. onToggle recebe uma função para redesenhar o próprio botão. */
    private fun adicionarCard(titulo: String, descricao: String, preco: String,
                              selecionado: () -> Boolean, onToggle: (redesenhar: () -> Unit) -> Unit) {
        val view = layoutInflater.inflate(R.layout.item_extra_option, binding.containerExtras, false)
        view.findViewById<TextView>(R.id.tvExtraTitle)?.text = titulo
        view.findViewById<TextView>(R.id.tvExtraDesc)?.text = descricao
        view.findViewById<TextView>(R.id.tvExtraPrice)?.text = preco
        val btn = view.findViewById<TextView>(R.id.btnExtraAdd)
        val redesenhar: () -> Unit = {
            val on = selecionado()
            btn?.text = if (on) "✓ Adicionado" else "Adicionar"
            btn?.setBackgroundResource(if (on) R.drawable.bg_stepper_done else R.drawable.bg_chip)
            btn?.setTextColor(resources.getColor(if (on) R.color.white else R.color.hb_ink, theme))
        }
        redesenhar()
        btn?.setOnClickListener { onToggle(redesenhar); redesenhar() }
        binding.containerExtras.addView(view)
    }

    private fun escolherHorario(onEscolha: (String) -> Unit) {
        AlertDialog.Builder(this)
            .setTitle("Horário do jantar (todas as noites)")
            .setItems(HORARIOS_JANTAR.toTypedArray()) { _, i -> onEscolha(HORARIOS_JANTAR[i]) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun salvarEAvancar() {
        binding.btnToPayment.isEnabled = false
        lifecycleScope.launch {
            try {
                val api = ApiClient.getApi(this@CheckoutStep2Activity)
                var resp = api.atualizarExtras(reservaCodigo,
                    AdicionarExtrasRequest(selecionados.keys.map { SelecaoExtraDto(it) }))
                if (!resp.isSuccessful) return@launch erro(ApiErrors.mensagem(resp))

                val combo = comboEscolhido
                resp = if (combo != null) {
                    api.definirCombo(reservaCodigo, ComboRequest(combo.id, comboHorario ?: "20:00"))
                } else {
                    api.removerCombo(reservaCodigo)
                }
                // 409 VAGAS_ESGOTADAS: o salão (50 lugares) lotou em alguma noite nesse horário
                if (!resp.isSuccessful) return@launch erro(ApiErrors.mensagem(resp))

                irParaPagamento(resp.body() ?: return@launch erro("Resposta vazia da API"))
            } catch (e: Exception) {
                erro(ApiErrors.mensagem(e))
            } finally {
                binding.btnToPayment.isEnabled = true
            }
        }
    }

    private fun irParaPagamento(reserva: ReservaResponse) {
        val nomes = ArrayList(reserva.extras.map { it.nome } + listOfNotNull(reserva.combo?.let { "Combo ${it.nome} (${it.horario.take(5)})" }))
        val precos = (reserva.extras.map { it.subtotalCentavos } + listOfNotNull(reserva.combo?.valorCentavos)).toLongArray()
        startActivity(Intent(this, CheckoutStep3Activity::class.java).apply {
            putExtras(intent)
            putExtra("DIARIAS_CENTAVOS", reserva.valorDiarias)
            putExtra("TOTAL_CENTAVOS", reserva.total)
            putStringArrayListExtra("EXTRA_NAMES", nomes)
            putExtra("EXTRA_PRICES", precos)
        })
    }

    private fun erro(msg: String) {
        AlertDialog.Builder(this).setTitle("Ugh!").setMessage(msg).setPositiveButton("OK", null).show()
    }
}
