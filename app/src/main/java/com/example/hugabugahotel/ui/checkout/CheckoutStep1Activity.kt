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
import com.example.hugabugahotel.data.api.ApiErrors
import com.example.hugabugahotel.util.Formatos
import com.example.hugabugahotel.util.Mascaras
import com.example.hugabugahotel.util.Mascaras.mascaraCpf
import com.example.hugabugahotel.util.Mascaras.mascaraTelefone
import com.example.hugabugahotel.data.api.SessionManager
import com.example.hugabugahotel.data.model.HospedeDto
import com.example.hugabugahotel.data.model.HospedesRequest
import com.example.hugabugahotel.databinding.ActivityCheckoutStep1Binding
import kotlinx.coroutines.launch
import java.util.Locale

class CheckoutStep1Activity : AppCompatActivity() {

    private lateinit var binding: ActivityCheckoutStep1Binding
    private var reservaCodigo: String = ""
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
        binding.etPhone.mascaraTelefone()
        binding.etDocument.mascaraCpf()

        reservaCodigo = intent.getStringExtra("RESERVA_CODIGO") ?: run { finish(); return }
        checkInDisplay = intent.getStringExtra("CHECK_IN") ?: "12 out"
        checkOutDisplay = intent.getStringExtra("CHECK_OUT") ?: "15 out"
        adultsCount = intent.getIntExtra("ADULTS", 2)
        childrenCount = intent.getIntExtra("CHILDREN", 2)
        totalCentavos = intent.getLongExtra("TOTAL_CENTAVOS", 156000L)
        noitesCount = intent.getIntExtra("NOITES", 3)
        cavernName = intent.getStringExtra("CAVERN_NOME") ?: "Toca do Mamute"

        val sessionManager = SessionManager(this)
        val savedName = sessionManager.getUserName()
        val savedEmail = intent.getStringExtra("RESPONSAVEL_EMAIL")
            ?: sessionManager.getResponsavelEmail() ?: sessionManager.getUserEmail()
        if (sessionManager.isLoggedIn()) preencherComPerfil()

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

        binding.tvStepTotal.text = Formatos.reais(totalCentavos)

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
        val noitesStr = if (noitesVal == 1) "noite" else "noites"
        val breakdownText = "${Formatos.reais(totalCentavos / noitesVal)} × $noitesVal $noitesStr"
        val totalFormatted = Formatos.reais(totalCentavos)

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

    private fun preencherComPerfil() {
        lifecycleScope.launch {
            try {
                val r = ApiClient.getApi(this@CheckoutStep1Activity).getUserProfile()
                val perfil = r.body() ?: return@launch
                if (binding.etFullName.text.isNullOrBlank()) {
                    binding.etFullName.setText(listOfNotNull(perfil.nome, perfil.sobrenome).joinToString(" "))
                }
                if (binding.etEmail.text.isNullOrBlank()) binding.etEmail.setText(perfil.email)
                if (binding.etPhone.text.isNullOrBlank()) perfil.celular?.let { binding.etPhone.setText(it) }
            } catch (_: Exception) {
                // sem rede: o usuário digita
            }
        }
    }

    private fun salvarHospedesEContinuar() {
        val nomeInput = binding.etFullName.text.toString().trim()
        val emailInput = binding.etEmail.text.toString().trim()
        val phoneInput = binding.etPhone.text.toString().trim()
        val cpf = binding.etDocument.text.toString().filter { it.isDigit() }

        val erro = when {
            nomeInput.isEmpty() -> "Preencha o nome do responsável"
            !android.util.Patterns.EMAIL_ADDRESS.matcher(emailInput).matches() -> "Preencha um e-mail válido"
            Mascaras.digitos(phoneInput).length !in 10..11 -> "Preencha o celular com DDD: (00) 00000-0000"
            cpf.length != 11 -> "Preencha o CPF do responsável (11 dígitos)"
            else -> null
        }
        if (erro != null) {
            Toast.makeText(this, erro, Toast.LENGTH_SHORT).show()
            return
        }

        val sobrenome = nomeInput.split(" ").drop(1).joinToString(" ")
        val hospedes = buildList {
            add(HospedeDto(nomeInput, "ADULTO"))
            for (i in 2..adultsCount) add(HospedeDto("Adulto $i ${sobrenome}".trim(), "ADULTO"))
            for (i in 1..childrenCount) {
                val idade = when (i) { 1 -> kid1Age; 2 -> kid2Age; else -> 6 }
                add(HospedeDto("Criança $i ${sobrenome}".trim(), "CRIANCA", idade))
            }
        }

        binding.bottomBar.isEnabled = false
        lifecycleScope.launch {
            try {
                val api = ApiClient.getApi(this@CheckoutStep1Activity)
                val request = HospedesRequest(
                    responsavelNome = nomeInput,
                    responsavelEmail = emailInput,
                    celular = phoneInput,
                    documento = cpf,
                    hospedes = hospedes
                )
                val response = api.atualizarHospedes(reservaCodigo, request)
                val reserva = response.body()
                if (!response.isSuccessful || reserva == null) {
                    // 410 = o tempo de 15 min acabou; 400 = algum dado inválido
                    AlertDialog.Builder(this@CheckoutStep1Activity)
                        .setTitle("Confira os dados")
                        .setMessage(ApiErrors.mensagem(response))
                        .setPositiveButton("OK", null)
                        .show()
                    return@launch
                }
                // sem conta, o dono é reconhecido pelo e-mail do responsável
                if (!SessionManager(this@CheckoutStep1Activity).isLoggedIn()) {
                    SessionManager(this@CheckoutStep1Activity).saveResponsavelEmail(reserva.responsavelEmail ?: emailInput)
                }

                val nextIntent = Intent(this@CheckoutStep1Activity, CheckoutStep2Activity::class.java).apply {
                    putExtras(intent)
                    putExtra("TOTAL_CENTAVOS", reserva.total) // a idade das crianças pode mudar o total
                    putExtra("NOITES", reserva.noites)
                    putExtra("RESPONSAVEL_NOME", nomeInput)
                    putExtra("RESPONSAVEL_EMAIL", emailInput)
                    putExtra("RESPONSAVEL_PHONE", phoneInput)
                }
                startActivity(nextIntent)
            } catch (e: Exception) {
                Toast.makeText(this@CheckoutStep1Activity, ApiErrors.mensagem(e), Toast.LENGTH_LONG).show()
            } finally {
                binding.bottomBar.isEnabled = true
            }
        }
    }
}
