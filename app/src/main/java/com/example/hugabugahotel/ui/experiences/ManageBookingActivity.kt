package com.example.hugabugahotel.ui.experiences

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.hugabugahotel.R
import com.example.hugabugahotel.data.api.ApiClient
import com.example.hugabugahotel.data.api.SessionManager
import com.example.hugabugahotel.databinding.ActivityManageBookingBinding
import com.example.hugabugahotel.ui.checkout.CheckoutStep3Activity
import kotlinx.coroutines.launch
import java.util.Locale

class ManageBookingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManageBookingBinding
    private lateinit var sessionManager: SessionManager

    private var reservaCodigo: String = "HB-4F7K"
    private var cavernName: String = "Toca do Mamute"
    private var checkInDisplay: String = "7 out"
    private var checkOutDisplay: String = "14 out"
    private var baseTotalCentavos: Long = 156000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageBookingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        reservaCodigo = intent.getStringExtra("RESERVA_CODIGO") ?: "HB-4F7K"
        cavernName = intent.getStringExtra("CAVERN_NOME") ?: "Toca do Mamute"
        checkInDisplay = intent.getStringExtra("CHECK_IN") ?: "7 out"
        checkOutDisplay = intent.getStringExtra("CHECK_OUT") ?: "14 out"
        baseTotalCentavos = intent.getLongExtra("TOTAL_CENTAVOS", 156000L)

        binding.tvManageTitle.text = "Gerenciar Reserva $reservaCodigo"
        binding.tvManageCavern.text = cavernName
        binding.tvManageDates.text = "📅 $checkInDisplay – $checkOutDisplay"

        binding.btnAddExperienceFromManage.setOnClickListener {
            val intent = Intent(this, ExperiencesActivity::class.java)
            startActivity(intent)
        }

        binding.btnManagePayInApp.setOnClickListener {
            val extraCentavos = sessionManager.getExtraExpenses(reservaCodigo)
            val amountToPay = if (extraCentavos > 0) extraCentavos else 6000L
            val intent = Intent(this, CheckoutStep3Activity::class.java).apply {
                putExtra("RESERVA_CODIGO", reservaCodigo)
                putExtra("CAVERN_NOME", cavernName)
                putExtra("CHECK_IN", checkInDisplay)
                putExtra("CHECK_OUT", checkOutDisplay)
                putExtra("TOTAL_CENTAVOS", amountToPay)
                putExtra("IS_EXPERIENCES_PAYMENT", true)
            }
            startActivity(intent)
        }

        loadDataAndRender()
    }

    override fun onResume() {
        super.onResume()
        loadDataAndRender()
    }

    private fun loadDataAndRender() {
        val extraCentavos = sessionManager.getExtraExpenses(reservaCodigo)
        val base = if (baseTotalCentavos > 0) baseTotalCentavos else 156000L
        val finalTotal = base + extraCentavos
        val totalFormatted = String.format(Locale("pt", "BR"), "R$ %.2f", finalTotal / 100.0)
        binding.tvManageTotal.text = "💰 Total atualizado: $totalFormatted"

        // Render pending payment banner
        val payMethod = sessionManager.getPaymentMethod(reservaCodigo)
        val isPixOrCard = (payMethod == "PIX" || payMethod == "CARTAO" || payMethod == "CARD")
        val isPaidInApp = sessionManager.isExperiencePaidInApp(reservaCodigo)
        val hasExtras = extraCentavos > 0

        if (isPixOrCard && hasExtras && !isPaidInApp) {
            val extraFormatted = String.format(Locale("pt", "BR"), "R$ %.2f", extraCentavos / 100.0)
            binding.tvManagePendingText.text = "Despesas pendentes: $extraFormatted (Pagar no app ou recepção)"
            binding.layoutManagePendingPayment.visibility = View.VISIBLE
            binding.btnManagePayInApp.visibility = View.VISIBLE
        } else if (isPixOrCard && hasExtras && isPaidInApp) {
            binding.tvManagePendingText.text = "✓ Despesas adicionais quitadas pelo app"
            binding.layoutManagePendingPayment.visibility = View.VISIBLE
            binding.btnManagePayInApp.visibility = View.GONE
        } else {
            binding.layoutManagePendingPayment.visibility = View.GONE
        }

        // Render booked experiences list
        val container = binding.layoutManageBookedExperiences
        container.removeAllViews()

        val scheduledAgenda = sessionManager.getScheduledExperiences(reservaCodigo)
        var totalBookedCount = 0

        for ((dateIso, dayMap) in scheduledAgenda) {
            for ((hora, pair) in dayMap) {
                totalBookedCount++
                val title = pair.first
                val detail = pair.second

                val card = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setBackgroundResource(R.drawable.bg_card)
                    setPadding(28, 20, 28, 20)
                    gravity = android.view.Gravity.CENTER_VERTICAL
                    val params = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    params.setMargins(0, 0, 0, 12)
                    layoutParams = params
                }

                val infoLayout = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }

                val tvTitle = TextView(this).apply {
                    text = "$title ($hora)"
                    setTextAppearance(R.style.HB_Text_H3)
                }

                val tvDetail = TextView(this).apply {
                    text = "Data: $dateIso · $detail"
                    setTextAppearance(R.style.HB_Text_Muted)
                }

                infoLayout.addView(tvTitle)
                infoLayout.addView(tvDetail)

                val btnCancel = TextView(this).apply {
                    text = "Cancelar"
                    background = ContextCompat.getDrawable(this@ManageBookingActivity, R.drawable.bg_badge_red)
                    setTextColor(ContextCompat.getColor(this@ManageBookingActivity, R.color.white))
                    setPadding(24, 12, 24, 12)
                    textSize = 12f
                    setTypeface(null, Typeface.BOLD)
                    setOnClickListener {
                        cancelarExperiencia(dateIso, hora)
                    }
                }

                card.addView(infoLayout)
                card.addView(btnCancel)
                container.addView(card)
            }
        }

        if (totalBookedCount == 0) {
            binding.tvManageNoExperiences.visibility = View.VISIBLE
        } else {
            binding.tvManageNoExperiences.visibility = View.GONE
        }
    }

    private fun cancelarExperiencia(dateIso: String, hora: String) {
        lifecycleScope.launch {
            try {
                val api = ApiClient.getApi(this@ManageBookingActivity)
                api.cancelarAgendamento(reservaCodigo, "agendamento_$hora")
            } catch (_: Exception) {}

            val scheduledAgenda = sessionManager.getScheduledExperiences(reservaCodigo)
            scheduledAgenda[dateIso]?.remove(hora)
            scheduledAgenda[dateIso]?.remove("${hora.take(2)}h")
            if (scheduledAgenda[dateIso]?.isEmpty() == true) {
                scheduledAgenda.remove(dateIso)
            }
            sessionManager.saveScheduledExperiences(reservaCodigo, scheduledAgenda)

            // Subtrai R$ 60,00 (6000 centavos)
            sessionManager.addExtraExpenseToBooking(reservaCodigo, -6000L)

            loadDataAndRender()
            Toast.makeText(this@ManageBookingActivity, "Experiência cancelada com sucesso!", Toast.LENGTH_SHORT).show()
        }
    }
}
