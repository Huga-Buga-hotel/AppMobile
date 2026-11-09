package com.example.hugabugahotel.ui.checkout

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.hugabugahotel.R
import com.example.hugabugahotel.data.api.ApiClient
import com.example.hugabugahotel.data.model.AdicionarExtrasRequest
import com.example.hugabugahotel.data.model.ExtraItem
import com.example.hugabugahotel.data.model.SelecaoExtraDto
import com.example.hugabugahotel.databinding.ActivityCheckoutStep2Binding
import kotlinx.coroutines.launch
import java.util.Locale

class CheckoutStep2Activity : AppCompatActivity() {

    private lateinit var binding: ActivityCheckoutStep2Binding
    private var reservaCodigo: String = "HB-4F7K"
    private var checkInDisplay: String = "12 out"
    private var checkOutDisplay: String = "15 out"
    private var adultsCount: Int = 2
    private var childrenCount: Int = 2
    private var baseTotalCentavos: Long = 156000L
    private var currentTotalCentavos: Long = 156000L
    private var noitesCount: Int = 3
    private var cavernName: String = "Toca do Mamute"

    private val selectedExtrasMap = mutableMapOf<String, ExtraItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCheckoutStep2Binding.inflate(layoutInflater)
        setContentView(binding.root)

        reservaCodigo = intent.getStringExtra("RESERVA_CODIGO") ?: "HB-4F7K"
        checkInDisplay = intent.getStringExtra("CHECK_IN") ?: "12 out"
        checkOutDisplay = intent.getStringExtra("CHECK_OUT") ?: "15 out"
        adultsCount = intent.getIntExtra("ADULTS", 2)
        childrenCount = intent.getIntExtra("CHILDREN", 2)
        baseTotalCentavos = intent.getLongExtra("TOTAL_CENTAVOS", 156000L)
        currentTotalCentavos = baseTotalCentavos
        noitesCount = intent.getIntExtra("NOITES", 3)
        cavernName = intent.getStringExtra("CAVERN_NOME") ?: "Toca do Mamute"

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnSkipExtras.setOnClickListener {
            avancarParaPagamento()
        }

        binding.btnToPayment.setOnClickListener {
            avancarParaPagamento()
        }

        binding.bottomBar.setOnClickListener {
            avancarParaPagamento()
        }

        setupStepper()
        bindBookingSummary()
        fetchExtras()
    }

    private fun setupStepper() {
        val step1 = findViewById<TextView>(R.id.step1Indicator)
        val step2 = findViewById<TextView>(R.id.step2Indicator)
        val step3 = findViewById<TextView>(R.id.step3Indicator)

        if (step1 == null || step2 == null || step3 == null) return

        val darkColor = resources.getColor(R.color.white, theme)
        val inkColor = resources.getColor(R.color.hb_ink, theme)

        step1.setBackgroundResource(R.drawable.bg_stepper_done)
        step1.setTextColor(darkColor)
        step1.text = "✓ 1. Quem vem"

        step2.setBackgroundResource(R.drawable.bg_stepper_active)
        step2.setTextColor(darkColor)
        step2.text = "2. Extras"

        step3.setBackgroundResource(R.drawable.bg_chip)
        step3.setTextColor(inkColor)
        step3.text = "3. Pagamento"
    }

    private fun bindBookingSummary() {
        binding.root.findViewById<TextView>(R.id.tvSummaryCavernName)?.text = cavernName
        binding.root.findViewById<TextView>(R.id.tvSummaryCheckIn)?.text = checkInDisplay
        binding.root.findViewById<TextView>(R.id.tvSummaryCheckOut)?.text = checkOutDisplay

        val totalPessoas = adultsCount + childrenCount
        binding.root.findViewById<TextView>(R.id.tvSummaryTribe)?.text = "TRIBO: $totalPessoas pessoas ($adultsCount ad., $childrenCount cri.)"

        val noitesVal = if (noitesCount > 0) noitesCount else 1
        val precoDiariaCentavos = baseTotalCentavos / noitesVal
        val noitesStr = if (noitesVal == 1) "noite" else "noites"
        val breakdownText = "R$ ${String.format(Locale.getDefault(), "%,d", precoDiariaCentavos / 100)} × $noitesVal $noitesStr"

        binding.root.findViewById<TextView>(R.id.tvSummaryNightsBreakdown)?.text = breakdownText
        binding.root.findViewById<TextView>(R.id.tvSummaryNightsTotal)?.text = "R$ ${String.format(Locale.getDefault(), "%,d", baseTotalCentavos / 100)}"

        val containerSummary = binding.root.findViewById<LinearLayout>(R.id.containerSummaryExtras)
        if (containerSummary != null) {
            containerSummary.removeAllViews()
            for (extra in selectedExtrasMap.values) {
                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(0, 6, 0, 0)
                }
                val tvName = TextView(this).apply {
                    text = extra.nome
                    setTextAppearance(R.style.HB_Text)
                    textSize = 12f
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }
                val tvValue = TextView(this).apply {
                    text = "+R$ ${String.format(Locale.getDefault(), "%,d", extra.precoCentavos / 100)}"
                    setTextAppearance(R.style.HB_Text)
                    textSize = 12f
                    setTypeface(null, Typeface.BOLD)
                }
                row.addView(tvName)
                row.addView(tvValue)
                containerSummary.addView(row)
            }
        }

        val totalFormatted = "R$ ${String.format(Locale.getDefault(), "%,d", currentTotalCentavos / 100)}"
        binding.root.findViewById<TextView>(R.id.tvSummaryTotal)?.text = totalFormatted
    }

    private fun fetchExtras() {
        lifecycleScope.launch {
            val apiList = try {
                val api = ApiClient.getApi(this@CheckoutStep2Activity)
                val response = api.getExtras()
                if (response.isSuccessful && response.body()?.isNotEmpty() == true) {
                    response.body()!!.filter {
                        !it.nome.contains("café", ignoreCase = true) && !it.nome.contains("coffee", ignoreCase = true)
                    }
                } else {
                    getDefaultExtras()
                }
            } catch (_: Exception) {
                getDefaultExtras()
            }
            populateExtrasList(apiList)
        }
    }

    private fun getDefaultExtras(): List<ExtraItem> {
        return listOf(
            ExtraItem("extra_kit", "Kit mini-caçador", "Lança de espuma, colar e mapa jurássico", 7000L),
            ExtraItem("extra_late_checkout", "Late check-out 15h", "Mais 3 horas de descanso na caverna", 9000L),
            ExtraItem("extra_transfer", "Transfer carro de pedra", "Bonde pré-histórico Rodoviária ⇄ Hotel", 12000L),
            ExtraItem("extra_festa", "Festa na caverna", "Bolo-ovo + decoração rupestre completa", 18000L),
            ExtraItem("extra_fogueira", "Fogueira VIP no terraço", "S'mores, marshmallow e mantas de pele", 9500L)
        )
    }

    private fun populateExtrasList(extras: List<ExtraItem>) {
        binding.containerExtras.removeAllViews()

        val darkColor = resources.getColor(R.color.white, theme)
        val inkColor = resources.getColor(R.color.hb_ink, theme)

        for (extra in extras) {
            val extraView = layoutInflater.inflate(R.layout.item_extra_option, binding.containerExtras, false)
            val tvTitle = extraView.findViewById<TextView>(R.id.tvExtraTitle)
            val tvDesc = extraView.findViewById<TextView>(R.id.tvExtraDesc)
            val tvPrice = extraView.findViewById<TextView>(R.id.tvExtraPrice)
            val btnAdd = extraView.findViewById<TextView>(R.id.btnExtraAdd)

            tvTitle?.text = extra.nome
            tvDesc?.text = extra.descricao ?: ""
            tvPrice?.text = "R$ ${String.format(Locale.getDefault(), "%,d", extra.precoCentavos / 100)}"

            val isSelected = selectedExtrasMap.containsKey(extra.id)
            if (isSelected) {
                btnAdd?.text = "✓ Adicionado"
                btnAdd?.setBackgroundResource(R.drawable.bg_stepper_done)
                btnAdd?.setTextColor(darkColor)
            } else {
                btnAdd?.text = "Reservar"
                btnAdd?.setBackgroundResource(R.drawable.bg_chip)
                btnAdd?.setTextColor(inkColor)
            }

            btnAdd?.setOnClickListener {
                if (selectedExtrasMap.containsKey(extra.id)) {
                    selectedExtrasMap.remove(extra.id)
                    btnAdd.text = "Reservar"
                    btnAdd.setBackgroundResource(R.drawable.bg_chip)
                    btnAdd.setTextColor(inkColor)
                } else {
                    selectedExtrasMap[extra.id] = extra
                    btnAdd.text = "✓ Adicionado"
                    btnAdd.setBackgroundResource(R.drawable.bg_stepper_done)
                    btnAdd.setTextColor(darkColor)
                }

                val extrasSum = selectedExtrasMap.values.sumOf { it.precoCentavos }
                currentTotalCentavos = baseTotalCentavos + extrasSum
                bindBookingSummary()
            }

            binding.containerExtras.addView(extraView)
        }
    }

    private fun avancarParaPagamento() {
        lifecycleScope.launch {
            try {
                val api = ApiClient.getApi(this@CheckoutStep2Activity)
                val extrasRequest = selectedExtrasMap.map { SelecaoExtraDto(it.key, 1) }
                val request = AdicionarExtrasRequest(extras = extrasRequest)
                api.atualizarExtras(reservaCodigo, request)
            } catch (_: Exception) {}

            val extraNames = ArrayList(selectedExtrasMap.values.map { it.nome })
            val extraPrices = selectedExtrasMap.values.map { it.precoCentavos }.toLongArray()

            val nextIntent = Intent(this@CheckoutStep2Activity, CheckoutStep3Activity::class.java).apply {
                putExtras(intent)
                putExtra("TOTAL_CENTAVOS", currentTotalCentavos)
                putStringArrayListExtra("EXTRA_NAMES", extraNames)
                putExtra("EXTRA_PRICES", extraPrices)
            }
            startActivity(nextIntent)
        }
    }
}
