package com.example.hugabugahotel.ui.checkout

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
import com.example.hugabugahotel.data.api.SessionManager
import com.example.hugabugahotel.data.model.PagamentoRequest
import com.example.hugabugahotel.data.model.ReservaResponse
import com.example.hugabugahotel.databinding.ActivityCheckoutStep3Binding
import com.example.hugabugahotel.util.QrCodeUtils
import kotlinx.coroutines.launch
import java.util.Locale

class CheckoutStep3Activity : AppCompatActivity() {

    private lateinit var binding: ActivityCheckoutStep3Binding
    private var reservaCodigo: String = "HB-4F7K"
    private var checkInDisplay: String = "12 out"
    private var checkOutDisplay: String = "15 out"
    private var adultsCount: Int = 2
    private var childrenCount: Int = 2
    private var totalCentavos: Long = 156000L
    private var noitesCount: Int = 3
    private var cavernName: String = "Toca do Mamute"
    private var isExperiencesPayment: Boolean = false

    private var selectedPaymentMethod: String = "PIX" // "CARD", "PIX", "ARRIVAL"
    private var pixTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCheckoutStep3Binding.inflate(layoutInflater)
        setContentView(binding.root)

        reservaCodigo = intent.getStringExtra("RESERVA_CODIGO") ?: "HB-4F7K"
        checkInDisplay = intent.getStringExtra("CHECK_IN") ?: "12 out"
        checkOutDisplay = intent.getStringExtra("CHECK_OUT") ?: "15 out"
        adultsCount = intent.getIntExtra("ADULTS", 2)
        childrenCount = intent.getIntExtra("CHILDREN", 2)
        totalCentavos = intent.getLongExtra("TOTAL_CENTAVOS", 156000L)
        noitesCount = intent.getIntExtra("NOITES", 3)
        cavernName = intent.getStringExtra("CAVERN_NOME") ?: "Toca do Mamute"
        isExperiencesPayment = intent.getBooleanExtra("IS_EXPERIENCES_PAYMENT", false)

        binding.btnBack.setOnClickListener {
            finish()
        }

        setupTabs()
        setupStepper()
        bindBookingSummary()
        updatePaymentMethodUi()
        startPixTimer()
        updatePixQrCode()

        binding.layoutPixCopyContainer.setOnClickListener {
            copiarCodigoPix()
        }

        binding.btnCopyPixIcon.setOnClickListener {
            copiarCodigoPix()
        }

        binding.btnActionPay.setOnClickListener {
            processarPagamento()
        }

        binding.bottomBar.setOnClickListener {
            processarPagamento()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        pixTimer?.cancel()
    }

    private fun updatePixQrCode() {
        val code = binding.tvPixCode.text.toString()
        val qrBitmap = QrCodeUtils.generateQrCodeBitmap("PIX_PAYMENT_${reservaCodigo}_$code", 512, 512)
        if (qrBitmap != null) {
            binding.root.findViewById<ImageView>(R.id.ivPixQrCode)?.setImageBitmap(qrBitmap)
        }
    }

    private fun setupTabs() {
        binding.tabCard.setOnClickListener {
            selectedPaymentMethod = "CARD"
            updatePaymentMethodUi()
        }

        binding.tabPix.setOnClickListener {
            selectedPaymentMethod = "PIX"
            updatePaymentMethodUi()
        }

        binding.tabArrival.setOnClickListener {
            selectedPaymentMethod = "ARRIVAL"
            updatePaymentMethodUi()
        }
    }

    private fun updatePaymentMethodUi() {
        val darkColor = resources.getColor(R.color.white, theme)
        val inkColor = resources.getColor(R.color.hb_ink, theme)

        when (selectedPaymentMethod) {
            "CARD" -> {
                binding.tabCard.setBackgroundResource(R.drawable.bg_chip_selected)
                binding.tabCard.setTextColor(darkColor)

                binding.tabPix.setBackgroundResource(R.drawable.bg_chip)
                binding.tabPix.setTextColor(inkColor)

                binding.tabArrival.setBackgroundResource(R.drawable.bg_chip)
                binding.tabArrival.setTextColor(inkColor)

                binding.layoutCardForm.visibility = View.VISIBLE
                binding.layoutPixForm.visibility = View.GONE
                binding.layoutArrivalForm.visibility = View.GONE

                val totalFormatted = "R$ ${String.format(Locale.getDefault(), "%,d", totalCentavos / 100)}"
                binding.btnActionPay.text = "Pagar com Cartão ($totalFormatted)"
            }
            "ARRIVAL" -> {
                binding.tabArrival.setBackgroundResource(R.drawable.bg_chip_selected)
                binding.tabArrival.setTextColor(darkColor)

                binding.tabCard.setBackgroundResource(R.drawable.bg_chip)
                binding.tabCard.setTextColor(inkColor)

                binding.tabPix.setBackgroundResource(R.drawable.bg_chip)
                binding.tabPix.setTextColor(inkColor)

                binding.layoutArrivalForm.visibility = View.VISIBLE
                binding.layoutCardForm.visibility = View.GONE
                binding.layoutPixForm.visibility = View.GONE

                binding.btnActionPay.text = if (isExperiencesPayment) "Confirmar Acerto Presencial" else "Confirmar Reserva (Pagar no Check-in)"
            }
            else -> { // PIX
                binding.tabPix.setBackgroundResource(R.drawable.bg_chip_selected)
                binding.tabPix.setTextColor(darkColor)

                binding.tabCard.setBackgroundResource(R.drawable.bg_chip)
                binding.tabCard.setTextColor(inkColor)

                binding.tabArrival.setBackgroundResource(R.drawable.bg_chip)
                binding.tabArrival.setTextColor(inkColor)

                binding.layoutPixForm.visibility = View.VISIBLE
                binding.layoutCardForm.visibility = View.GONE
                binding.layoutArrivalForm.visibility = View.GONE

                val pixTotal = (totalCentavos * 0.95).toLong()
                val pixFormatted = "R$ ${String.format(Locale.getDefault(), "%,d", pixTotal / 100)}"
                binding.tvPixSubtext.text = "Total com desconto Pix (-5%): $pixFormatted"
                binding.btnActionPay.text = "Copiar Código Pix ($pixFormatted)"
            }
        }
    }

    private fun startPixTimer() {
        pixTimer?.cancel()
        pixTimer = object : CountDownTimer(10 * 60 * 1000L, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                val minutes = millisUntilFinished / 1000 / 60
                val seconds = (millisUntilFinished / 1000) % 60
                binding.tvPixTimer.text = String.format(Locale.getDefault(), "⏰ Código expira em %02d:%02d", minutes, seconds)
            }

            override fun onFinish() {
                binding.tvPixTimer.text = "⏰ Código Pix expirado"
            }
        }.start()
    }

    private fun copiarCodigoPix() {
        val codigo = binding.tvPixCode.text.toString()
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Código Pix Huga Buga", codigo)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Código Pix copiado com sucesso! 🪨", Toast.LENGTH_SHORT).show()
    }

    private fun setupStepper() {
        val step1 = findViewById<TextView>(R.id.step1Indicator)
        val step2 = findViewById<TextView>(R.id.step2Indicator)
        val step3 = findViewById<TextView>(R.id.step3Indicator)

        if (step1 == null || step2 == null || step3 == null) return

        val darkColor = resources.getColor(R.color.white, theme)

        if (isExperiencesPayment) {
            step1.visibility = View.GONE
            step2.visibility = View.GONE
            step3.text = "Pagamento de Experiências"
        } else {
            step1.setBackgroundResource(R.drawable.bg_stepper_done)
            step1.setTextColor(darkColor)
            step1.text = "✓ 1. Quem vem"

            step2.setBackgroundResource(R.drawable.bg_stepper_done)
            step2.setTextColor(darkColor)
            step2.text = "✓ 2. Extras"

            step3.setBackgroundResource(R.drawable.bg_stepper_active)
            step3.setTextColor(darkColor)
            step3.text = "3. Pagamento"
        }
    }

    private fun bindBookingSummary() {
        if (isExperiencesPayment) {
            binding.root.findViewById<TextView>(R.id.tvSummaryCavernName)?.text = "Experiências Adicionais"
            binding.root.findViewById<TextView>(R.id.tvSummaryCheckIn)?.text = reservaCodigo
            binding.root.findViewById<TextView>(R.id.tvSummaryCheckOut)?.text = "$checkInDisplay – $checkOutDisplay"
            binding.root.findViewById<TextView>(R.id.tvSummaryTribe)?.text = "PAGAMENTO DE DESPESAS DE EXPERIÊNCIAS"
            binding.root.findViewById<TextView>(R.id.tvSummaryNightsBreakdown)?.text = "Experiências adicionais agendadas"
            binding.root.findViewById<TextView>(R.id.tvSummaryNightsTotal)?.text = "R$ ${String.format(Locale.getDefault(), "%,d", totalCentavos / 100)}"
            binding.root.findViewById<LinearLayout>(R.id.containerSummaryExtras)?.removeAllViews()
            val totalFormatted = "R$ ${String.format(Locale.getDefault(), "%,d", totalCentavos / 100)}"
            binding.root.findViewById<TextView>(R.id.tvSummaryTotal)?.text = totalFormatted
            return
        }

        binding.root.findViewById<TextView>(R.id.tvSummaryCavernName)?.text = cavernName
        binding.root.findViewById<TextView>(R.id.tvSummaryCheckIn)?.text = checkInDisplay
        binding.root.findViewById<TextView>(R.id.tvSummaryCheckOut)?.text = checkOutDisplay

        val totalPessoas = adultsCount + childrenCount
        binding.root.findViewById<TextView>(R.id.tvSummaryTribe)?.text = "TRIBO: $totalPessoas pessoas ($adultsCount ad., $childrenCount cri.)"

        val extraNames = intent.getStringArrayListExtra("EXTRA_NAMES") ?: arrayListOf()
        val extraPrices = intent.getLongArrayExtra("EXTRA_PRICES") ?: longArrayOf()

        val extrasSum = extraPrices.sum()
        val baseStayCentavos = if (totalCentavos > extrasSum) totalCentavos - extrasSum else totalCentavos

        val noitesVal = if (noitesCount > 0) noitesCount else 1
        val precoDiariaCentavos = baseStayCentavos / noitesVal
        val noitesStr = if (noitesVal == 1) "noite" else "noites"
        val breakdownText = "R$ ${String.format(Locale.getDefault(), "%,d", precoDiariaCentavos / 100)} × $noitesVal $noitesStr"

        binding.root.findViewById<TextView>(R.id.tvSummaryNightsBreakdown)?.text = breakdownText
        binding.root.findViewById<TextView>(R.id.tvSummaryNightsTotal)?.text = "R$ ${String.format(Locale.getDefault(), "%,d", baseStayCentavos / 100)}"

        val containerSummary = binding.root.findViewById<LinearLayout>(R.id.containerSummaryExtras)
        if (containerSummary != null) {
            containerSummary.removeAllViews()
            for (i in extraNames.indices) {
                val name = extraNames[i]
                val price = if (i < extraPrices.size) extraPrices[i] else 0L
                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(0, 6, 0, 0)
                }
                val tvName = TextView(this).apply {
                    text = name
                    setTextAppearance(R.style.HB_Text)
                    textSize = 12f
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }
                val tvValue = TextView(this).apply {
                    text = "+R$ ${String.format(Locale.getDefault(), "%,d", price / 100)}"
                    setTextAppearance(R.style.HB_Text)
                    textSize = 12f
                    setTypeface(null, Typeface.BOLD)
                }
                row.addView(tvName)
                row.addView(tvValue)
                containerSummary.addView(row)
            }
        }

        val totalFormatted = "R$ ${String.format(Locale.getDefault(), "%,d", totalCentavos / 100)}"
        binding.root.findViewById<TextView>(R.id.tvSummaryTotal)?.text = totalFormatted
    }

    private fun processarPagamento() {
        if (selectedPaymentMethod == "PIX") {
            copiarCodigoPix()
        }

        lifecycleScope.launch {
            val metodo = when (selectedPaymentMethod) {
                "CARD" -> "CARTAO"
                "ARRIVAL" -> "NA_CHEGADA"
                else -> "PIX"
            }
            SessionManager(this@CheckoutStep3Activity).savePaymentMethod(reservaCodigo, metodo)
            try {
                val api = ApiClient.getApi(this@CheckoutStep3Activity)
                val request = PagamentoRequest(metodo = metodo, aceitoRegras = true)
                api.realizarPagamento(reservaCodigo, request)
            } catch (_: Exception) {}

            if (isExperiencesPayment) {
                SessionManager(this@CheckoutStep3Activity).saveExperiencePaidInApp(reservaCodigo, true)
                Toast.makeText(this@CheckoutStep3Activity, "Pagamento das experiências adicionais realizado com sucesso! 🪨", Toast.LENGTH_LONG).show()
                finish()
            } else {
                val completedReserva = ReservaResponse(
                    codigo = reservaCodigo,
                    cavernaId = "",
                    cavernaNome = cavernName,
                    checkIn = checkInDisplay,
                    checkOut = checkOutDisplay,
                    adultos = adultsCount,
                    status = "CONFIRMADA",
                    totalCentavos = totalCentavos
                )
                SessionManager(this@CheckoutStep3Activity).saveCompletedBooking(completedReserva)

                val nextIntent = Intent(this@CheckoutStep3Activity, VoucherActivity::class.java).apply {
                    putExtras(intent)
                }
                startActivity(nextIntent)
            }
        }
    }
}
