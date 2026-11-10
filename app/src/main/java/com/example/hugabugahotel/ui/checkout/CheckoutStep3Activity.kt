package com.example.hugabugahotel.ui.checkout

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.hugabugahotel.R
import com.example.hugabugahotel.data.api.ApiClient
import com.example.hugabugahotel.data.api.ApiErrors
import com.example.hugabugahotel.data.model.PagamentoRequest
import com.example.hugabugahotel.data.model.PagamentoResponse
import com.example.hugabugahotel.databinding.ActivityCheckoutStep3Binding
import com.example.hugabugahotel.util.Formatos
import com.example.hugabugahotel.util.QrCodeUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.OffsetDateTime

/**
 * Passo 3: pagamento simulado pela API.
 *  - Cartão: aprova ou recusa na hora (cartão de teste 4111 1111 1111 1111; finais 0002 e 0069 recusam).
 *  - Pix: a API gera o copia e cola (vale 10 min, 5% de desconto); o app consulta o status a cada 3 s.
 *  - Na chegada: confirma a reserva sem cobrar.
 * Só vai para o voucher quando a API confirma.
 */
class CheckoutStep3Activity : AppCompatActivity() {

    private lateinit var binding: ActivityCheckoutStep3Binding
    private var reservaCodigo: String = ""
    private var checkInDisplay: String = ""
    private var checkOutDisplay: String = ""
    private var adultsCount: Int = 2
    private var childrenCount: Int = 0
    private var totalCentavos: Long = 0L
    private var diariasCentavos: Long = 0L
    private var noitesCount: Int = 1
    private var cavernName: String = ""

    private var metodo: String = "PIX" // CARTAO, PIX, NA_CHEGADA
    private var pixTimer: CountDownTimer? = null
    private var pixPolling: Job? = null
    private var pixAtual: PagamentoResponse? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCheckoutStep3Binding.inflate(layoutInflater)
        setContentView(binding.root)

        reservaCodigo = intent.getStringExtra("RESERVA_CODIGO") ?: run { finish(); return }
        checkInDisplay = intent.getStringExtra("CHECK_IN") ?: ""
        checkOutDisplay = intent.getStringExtra("CHECK_OUT") ?: ""
        adultsCount = intent.getIntExtra("ADULTS", 2)
        childrenCount = intent.getIntExtra("CHILDREN", 0)
        totalCentavos = intent.getLongExtra("TOTAL_CENTAVOS", 0L)
        diariasCentavos = intent.getLongExtra("DIARIAS_CENTAVOS", totalCentavos)
        noitesCount = intent.getIntExtra("NOITES", 1).coerceAtLeast(1)
        cavernName = intent.getStringExtra("CAVERN_NOME") ?: ""

        binding.btnBack.setOnClickListener { finish() }
        binding.tabCard.setOnClickListener { metodo = "CARTAO"; atualizarAbas() }
        binding.tabPix.setOnClickListener { metodo = "PIX"; atualizarAbas() }
        binding.tabArrival.setOnClickListener { metodo = "NA_CHEGADA"; atualizarAbas() }
        binding.layoutPixCopyContainer.setOnClickListener { copiarPix() }
        binding.btnCopyPixIcon.setOnClickListener { copiarPix() }
        binding.btnActionPay.setOnClickListener { pagar() }
        binding.bottomBar.setOnClickListener { pagar() }

        setupStepper()
        bindBookingSummary()
        atualizarAbas()
    }

    override fun onDestroy() {
        super.onDestroy()
        pixTimer?.cancel()
        pixPolling?.cancel()
    }

    private fun setupStepper() {
        val white = resources.getColor(R.color.white, theme)
        findViewById<TextView>(R.id.step1Indicator)?.apply { setBackgroundResource(R.drawable.bg_stepper_done); setTextColor(white); text = "✓ 1. Quem vem" }
        findViewById<TextView>(R.id.step2Indicator)?.apply { setBackgroundResource(R.drawable.bg_stepper_done); setTextColor(white); text = "✓ 2. Extras" }
        findViewById<TextView>(R.id.step3Indicator)?.apply { setBackgroundResource(R.drawable.bg_stepper_active); setTextColor(white); text = "3. Pagamento" }
    }

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

        val nomes = intent.getStringArrayListExtra("EXTRA_NAMES") ?: arrayListOf()
        val precos = intent.getLongArrayExtra("EXTRA_PRICES") ?: longArrayOf()
        binding.root.findViewById<LinearLayout>(R.id.containerSummaryExtras)?.let { container ->
            container.removeAllViews()
            nomes.forEachIndexed { i, nome ->
                val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, 6, 0, 0) }
                row.addView(TextView(this).apply {
                    text = nome; setTextAppearance(R.style.HB_Text); textSize = 12f
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                })
                row.addView(TextView(this).apply {
                    text = "+${Formatos.reais(precos.getOrElse(i) { 0L })}"
                    setTextAppearance(R.style.HB_Text); textSize = 12f; setTypeface(null, Typeface.BOLD)
                })
                container.addView(row)
            }
        }
        binding.root.findViewById<TextView>(R.id.tvSummaryTotal)?.text = Formatos.reais(totalCentavos)
    }

    private fun atualizarAbas() {
        val white = resources.getColor(R.color.white, theme)
        val ink = resources.getColor(R.color.hb_ink, theme)
        fun aba(view: TextView, ativa: Boolean) {
            view.setBackgroundResource(if (ativa) R.drawable.bg_chip_selected else R.drawable.bg_chip)
            view.setTextColor(if (ativa) white else ink)
        }
        aba(binding.tabCard, metodo == "CARTAO")
        aba(binding.tabPix, metodo == "PIX")
        aba(binding.tabArrival, metodo == "NA_CHEGADA")
        binding.layoutCardForm.visibility = if (metodo == "CARTAO") View.VISIBLE else View.GONE
        binding.layoutPixForm.visibility = if (metodo == "PIX") View.VISIBLE else View.GONE
        binding.layoutArrivalForm.visibility = if (metodo == "NA_CHEGADA") View.VISIBLE else View.GONE

        binding.btnActionPay.text = when (metodo) {
            "CARTAO" -> "Pagar com cartão (${Formatos.reais(totalCentavos)})"
            "NA_CHEGADA" -> "Confirmar reserva (pagar no check-in)"
            else -> {
                // estimativa; o valor exato (total − 5%) vem da API ao gerar o Pix
                val comDesconto = totalCentavos - (totalCentavos * 5 + 50) / 100
                binding.tvPixSubtext.text = "Total com desconto Pix (−5%): ${Formatos.reais(pixAtual?.valorCentavos ?: comDesconto)}"
                if (pixAtual == null) "Gerar código Pix" else "Copiar código Pix"
            }
        }
        if (metodo == "PIX" && pixAtual == null) {
            binding.tvPixCode.text = "Toque em \"Gerar código Pix\""
            binding.tvPixTimer.text = "⏰ O código vale 10 minutos"
        }
    }

    private fun pagar() {
        if (metodo == "PIX" && pixAtual != null) return copiarPix()
        val request = when (metodo) {
            "CARTAO" -> montarCartao() ?: return
            "NA_CHEGADA" -> PagamentoRequest(metodo = "NA_CHEGADA")
            else -> PagamentoRequest(metodo = "PIX")
        }
        binding.btnActionPay.isEnabled = false
        lifecycleScope.launch {
            try {
                val response = ApiClient.getApi(this@CheckoutStep3Activity).realizarPagamento(reservaCodigo, request)
                val pagamento = response.body()
                if (!response.isSuccessful || pagamento == null) {
                    // 422 PAGAMENTO_RECUSADO (a toca continua segurada), 410 RESERVA_EXPIRADA, 400 dados inválidos
                    return@launch erro(ApiErrors.mensagem(response))
                }
                when {
                    pagamento.metodo == "PIX" && pagamento.status == "PENDENTE" -> mostrarPix(pagamento)
                    else -> irParaVoucher() // cartão aprovado ou "na chegada" (reserva já CONFIRMADA)
                }
            } catch (e: Exception) {
                erro(ApiErrors.mensagem(e))
            } finally {
                binding.btnActionPay.isEnabled = true
            }
        }
    }

    private fun montarCartao(): PagamentoRequest? {
        val numero = binding.etCardNumber.text.toString().filter { it.isDigit() }
        val validade = binding.etCardExpiry.text.toString().trim()
        val cvv = binding.etCardCvv.text.toString().trim()
        val erroCampo = when {
            numero.length !in 13..19 -> "Número do cartão inválido"
            !Regex("""\d{2}/\d{2}""").matches(validade) -> "Validade no formato MM/AA"
            !Regex("""\d{3,4}""").matches(cvv) -> "CVV inválido"
            binding.etCardName.text.isNullOrBlank() -> "Informe o nome impresso no cartão"
            else -> null
        }
        if (erroCampo != null) {
            Toast.makeText(this, erroCampo, Toast.LENGTH_SHORT).show()
            return null
        }
        // Ao confirmar, o hóspede aceita as regras da estadia (cancelamento grátis até 2 dias antes)
        return PagamentoRequest(metodo = "CARTAO", parcelas = 1, numeroCartao = numero, validade = validade,
            cvv = cvv, aceitoRegras = true)
    }

    private fun mostrarPix(p: PagamentoResponse) {
        pixAtual = p
        val codigo = p.pixCopiaCola.orEmpty()
        binding.tvPixCode.text = codigo
        QrCodeUtils.generateQrCodeBitmap(codigo, 512, 512)?.let {
            binding.root.findViewById<ImageView>(R.id.ivPixQrCode)?.setImageBitmap(it)
        }
        atualizarAbas()
        iniciarTimer(p.pixExpiraEm)
        acompanharPix(p.id)
    }

    private fun iniciarTimer(expiraEm: String?) {
        pixTimer?.cancel()
        val restante = try {
            Duration.between(OffsetDateTime.now(), OffsetDateTime.parse(expiraEm)).toMillis().coerceAtLeast(0)
        } catch (_: Exception) {
            10 * 60 * 1000L
        }
        pixTimer = object : CountDownTimer(restante, 1000L) {
            override fun onTick(ms: Long) {
                binding.tvPixTimer.text = String.format("⏰ Código expira em %02d:%02d", ms / 60000, (ms / 1000) % 60)
            }

            override fun onFinish() {
                binding.tvPixTimer.text = "⏰ Código Pix expirado. A reserva foi liberada."
            }
        }.start()
    }

    /** O front consulta GET /pagamentos/{id} a cada 3 s (no perfil dev o Pix aprova sozinho após 20 s). */
    private fun acompanharPix(pagamentoId: String) {
        pixPolling?.cancel()
        pixPolling = lifecycleScope.launch {
            while (isActive) {
                delay(3000)
                try {
                    val r = ApiClient.getApi(this@CheckoutStep3Activity).getStatusPagamento(pagamentoId)
                    when (r.body()?.status) {
                        "APROVADO" -> { pixTimer?.cancel(); irParaVoucher(); return@launch }
                        "EXPIRADO", "RECUSADO" -> {
                            pixTimer?.cancel()
                            erro("O Pix expirou e a reserva foi liberada. Faça uma nova reserva.")
                            return@launch
                        }
                    }
                } catch (_: Exception) {
                    // rede instável: tenta de novo no próximo ciclo
                }
            }
        }
    }

    private fun copiarPix() {
        val codigo = pixAtual?.pixCopiaCola ?: return
        (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager)
            .setPrimaryClip(ClipData.newPlainText("Código Pix Huga Buga", codigo))
        Toast.makeText(this, "Código Pix copiado! Pague no app do seu banco 🪨", Toast.LENGTH_SHORT).show()
    }

    private fun irParaVoucher() {
        startActivity(Intent(this, VoucherActivity::class.java).apply {
            putExtra("RESERVA_CODIGO", reservaCodigo)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        })
        finish()
    }

    private fun erro(msg: String) {
        AlertDialog.Builder(this).setTitle("Ugh!").setMessage(msg).setPositiveButton("OK", null).show()
    }
}
