package com.example.hugabugahotel.ui.checkout

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.hugabugahotel.R
import com.example.hugabugahotel.data.api.ApiClient
import com.example.hugabugahotel.data.api.SessionManager
import com.example.hugabugahotel.data.model.HospedeDto
import com.example.hugabugahotel.data.model.HospedesRequest
import com.example.hugabugahotel.databinding.ActivityCheckoutStep1Binding
import kotlinx.coroutines.launch
import java.util.Locale

class CheckoutStep1Activity : AppCompatActivity() {

    private lateinit var binding: ActivityCheckoutStep1Binding
    private var reservaCodigo: String = "HB-4F7K"
    private var checkInDisplay: String = "12 out"
    private var checkOutDisplay: String = "15 out"
    private var adultsCount: Int = 2
    private var childrenCount: Int = 2
    private var totalCentavos: Long = 156000L
    private var noitesCount: Int = 3
    private var cavernName: String = "Toca do Mamute"
    private var kid1Age: Int = 6
    private var kid2Age: Int = 9

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCheckoutStep1Binding.inflate(layoutInflater)
        setContentView(binding.root)

        reservaCodigo = intent.getStringExtra("RESERVA_CODIGO") ?: "HB-4F7K"
        checkInDisplay = intent.getStringExtra("CHECK_IN") ?: "12 out"
        checkOutDisplay = intent.getStringExtra("CHECK_OUT") ?: "15 out"
        adultsCount = intent.getIntExtra("ADULTS", 2)
        childrenCount = intent.getIntExtra("CHILDREN", 2)
        totalCentavos = intent.getLongExtra("TOTAL_CENTAVOS", 156000L)
        noitesCount = intent.getIntExtra("NOITES", 3)
        cavernName = intent.getStringExtra("CAVERN_NOME") ?: "Toca do Mamute"

        val sessionManager = SessionManager(this)
        val savedName = sessionManager.getUserName()
        val savedEmail = sessionManager.getResponsavelEmail() ?: sessionManager.getUserEmail()

        if (binding.etFullName.text.isNullOrEmpty() && !savedName.isNullOrEmpty()) {
            binding.etFullName.setText(savedName)
        }
        if (binding.etEmail.text.isNullOrEmpty() && !savedEmail.isNullOrEmpty()) {
            binding.etEmail.setText(savedEmail)
        }

        binding.btnBack.setOnClickListener {
            finish()
        }

        setupStepper(1)
        bindBookingSummary()

        val totalPessoas = adultsCount + childrenCount
        binding.tvStepTitle.text = "Reservar $cavernName"
        binding.tvStepSubtitle.text = "$checkInDisplay – $checkOutDisplay · $totalPessoas pessoas"

        if (totalCentavos > 0) {
            val totalFormatted = "R$ ${String.format(Locale.getDefault(), "%,d", totalCentavos / 100)}"
            binding.tvStepTotal.text = totalFormatted
        }

        if (childrenCount > 0) {
            binding.chipKid1.visibility = View.VISIBLE
            binding.chipKid1.text = "Criança 1: $kid1Age anos"
            if (childrenCount >= 2) {
                binding.chipKid2.visibility = View.VISIBLE
                binding.chipKid2.text = "Criança 2: $kid2Age anos"
            } else {
                binding.chipKid2.visibility = View.GONE
            }
        } else {
            binding.chipKid1.visibility = View.GONE
            binding.chipKid2.visibility = View.GONE
        }

        binding.chipKid1.setOnClickListener {
            showAgePicker(1) { age ->
                kid1Age = age
                binding.chipKid1.text = "Criança 1: $kid1Age anos"
            }
        }

        binding.chipKid2.setOnClickListener {
            showAgePicker(2) { age ->
                kid2Age = age
                binding.chipKid2.text = "Criança 2: $kid2Age anos"
            }
        }

        binding.bottomBar.setOnClickListener {
            salvarHospedesEContinuar()
        }
    }

    private fun setupStepper(stepIndex: Int) {
        val step1 = findViewById<TextView>(R.id.step1Indicator)
        val step2 = findViewById<TextView>(R.id.step2Indicator)
        val step3 = findViewById<TextView>(R.id.step3Indicator)

        if (step1 == null || step2 == null || step3 == null) return

        val darkColor = resources.getColor(R.color.white, theme)
        val inkColor = resources.getColor(R.color.hb_ink, theme)

        when (stepIndex) {
            1 -> {
                step1.setBackgroundResource(R.drawable.bg_stepper_active)
                step1.setTextColor(darkColor)
                step1.text = "1. Quem vem"

                step2.setBackgroundResource(R.drawable.bg_chip)
                step2.setTextColor(inkColor)
                step2.text = "2. Extras"

                step3.setBackgroundResource(R.drawable.bg_chip)
                step3.setTextColor(inkColor)
                step3.text = "3. Pagamento"
            }
            2 -> {
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
            3 -> {
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
    }

    private fun bindBookingSummary() {
        binding.root.findViewById<TextView>(R.id.tvSummaryCavernName)?.text = cavernName
        binding.root.findViewById<TextView>(R.id.tvSummaryCheckIn)?.text = checkInDisplay
        binding.root.findViewById<TextView>(R.id.tvSummaryCheckOut)?.text = checkOutDisplay

        val totalPessoas = adultsCount + childrenCount
        binding.root.findViewById<TextView>(R.id.tvSummaryTribe)?.text = "TRIBO: $totalPessoas pessoas ($adultsCount ad., $childrenCount cri.)"

        val noitesVal = if (noitesCount > 0) noitesCount else 1
        val precoDiariaCentavos = totalCentavos / noitesVal
        val noitesStr = if (noitesVal == 1) "noite" else "noites"
        val breakdownText = "R$ ${String.format(Locale.getDefault(), "%,d", precoDiariaCentavos / 100)} × $noitesVal $noitesStr"
        val totalFormatted = "R$ ${String.format(Locale.getDefault(), "%,d", totalCentavos / 100)}"

        binding.root.findViewById<TextView>(R.id.tvSummaryNightsBreakdown)?.text = breakdownText
        binding.root.findViewById<TextView>(R.id.tvSummaryNightsTotal)?.text = totalFormatted
        binding.root.findViewById<TextView>(R.id.tvSummaryTotal)?.text = totalFormatted
    }

    private fun showAgePicker(kidNumber: Int, onAgeSelected: (Int) -> Unit) {
        val ages = (0..12).map { "$it anos" }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Selecione a idade da Criança $kidNumber")
            .setItems(ages) { _, which ->
                onAgeSelected(which)
            }
            .show()
    }

    private fun salvarHospedesEContinuar() {
        val nomeInput = binding.etFullName.text.toString().trim()
        val emailInput = binding.etEmail.text.toString().trim()
        val phoneInput = binding.etPhone.text.toString().trim()

        if (nomeInput.isEmpty()) {
            Toast.makeText(this, "Por favor, preencha o nome do responsável", Toast.LENGTH_SHORT).show()
            binding.etFullName.requestFocus()
            return
        }

        if (emailInput.isEmpty()) {
            Toast.makeText(this, "Por favor, preencha o e-mail do responsável", Toast.LENGTH_SHORT).show()
            binding.etEmail.requestFocus()
            return
        }

        val sessionManager = SessionManager(this)
        sessionManager.saveResponsavelEmail(emailInput)

        lifecycleScope.launch {
            val hospedesList = mutableListOf<HospedeDto>()
            val nomePartes = nomeInput.split(" ")
            val primeiroNome = nomePartes.firstOrNull() ?: "Hóspede"
            val sobrenome = if (nomePartes.size > 1) nomePartes.drop(1).joinToString(" ") else "Tribo"

            for (i in 1..adultsCount) {
                if (i == 1) {
                    hospedesList.add(HospedeDto(primeiroNome, sobrenome, "ADULTO", 30))
                } else {
                    hospedesList.add(HospedeDto("Adulto $i", sobrenome, "ADULTO", 30))
                }
            }
            for (i in 1..childrenCount) {
                val age = if (i == 1) kid1Age else if (i == 2) kid2Age else 6
                hospedesList.add(HospedeDto("Criança $i", sobrenome, "CRIANCA", age))
            }

            try {
                val api = ApiClient.getApi(this@CheckoutStep1Activity)
                val request = HospedesRequest(
                    responsavelNome = primeiroNome,
                    responsavelSobrenome = sobrenome,
                    responsavelEmail = emailInput,
                    responsavelCelular = phoneInput,
                    hospedes = hospedesList
                )
                api.atualizarHospedes(reservaCodigo, request)
            } catch (_: Exception) {}

            val nextIntent = Intent(this@CheckoutStep1Activity, CheckoutStep2Activity::class.java).apply {
                putExtra("RESERVA_CODIGO", reservaCodigo)
                putExtra("CHECK_IN", checkInDisplay)
                putExtra("CHECK_OUT", checkOutDisplay)
                putExtra("ADULTS", adultsCount)
                putExtra("CHILDREN", childrenCount)
                putExtra("TOTAL_CENTAVOS", totalCentavos)
                putExtra("NOITES", noitesCount)
                putExtra("CAVERN_NOME", cavernName)
                putExtra("RESPONSAVEL_NOME", nomeInput)
                putExtra("RESPONSAVEL_EMAIL", emailInput)
                putExtra("RESPONSAVEL_PHONE", phoneInput)
            }
            startActivity(nextIntent)
        }
    }
}
