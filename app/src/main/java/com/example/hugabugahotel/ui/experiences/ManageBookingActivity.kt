package com.example.hugabugahotel.ui.experiences

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.hugabugahotel.R
import com.example.hugabugahotel.data.api.ApiClient
import com.example.hugabugahotel.data.api.ApiErrors
import com.example.hugabugahotel.data.model.AgendamentoDto
import com.example.hugabugahotel.databinding.ActivityManageBookingBinding
import com.example.hugabugahotel.util.Formatos
import kotlinx.coroutines.launch
import java.time.OffsetDateTime

/**
 * Gerenciar a estadia: resumo da reserva e atividades agendadas (GET /reservas/{codigo}/agendamentos).
 * As atividades vão para a conta da toca e são pagas no hotel; cancelar é possível até 4 h antes.
 */
class ManageBookingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManageBookingBinding
    private var reservaCodigo: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageBookingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        reservaCodigo = intent.getStringExtra("RESERVA_CODIGO") ?: run { finish(); return }
        binding.tvManageTitle.text = "Gerenciar reserva $reservaCodigo"
        binding.layoutManagePendingPayment.visibility = View.GONE
        binding.btnManagePayInApp.visibility = View.GONE

        binding.btnAddExperienceFromManage.setOnClickListener {
            startActivity(Intent(this, ExperiencesActivity::class.java).putExtra("RESERVA_CODIGO", reservaCodigo))
        }
    }

    override fun onResume() {
        super.onResume()
        carregar()
    }

    private fun carregar() {
        lifecycleScope.launch {
            try {
                val api = ApiClient.getApi(this@ManageBookingActivity)
                val reserva = api.getVoucherReserva(reservaCodigo)
                val r = reserva.body() ?: return@launch erro(ApiErrors.mensagem(reserva))
                binding.tvManageCavern.text = r.cavernaNome
                binding.tvManageDates.text = "📅 ${Formatos.dataCompleta(r.checkIn)} – ${Formatos.dataCompleta(r.checkOut)}"
                binding.tvManageTotal.text = "💰 Total da estadia: ${Formatos.reais(r.total)}"

                val lista = api.getAgendamentos(reservaCodigo)
                renderAgendamentos(lista.body().orEmpty().filter { it.status == "ATIVO" })
            } catch (e: Exception) {
                erro(ApiErrors.mensagem(e))
            }
        }
    }

    private fun renderAgendamentos(itens: List<AgendamentoDto>) {
        val container = binding.layoutManageBookedExperiences
        container.removeAllViews()
        binding.tvManageNoExperiences.visibility = if (itens.isEmpty()) View.VISIBLE else View.GONE
        val contaAtividades = itens.sumOf { it.valorCentavos }
        if (contaAtividades > 0) {
            binding.layoutManagePendingPayment.visibility = View.VISIBLE
            binding.tvManagePendingText.text = "Atividades na conta da toca: ${Formatos.reais(contaAtividades)} (pago no hotel)"
        }
        for (a in itens) {
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setBackgroundResource(R.drawable.bg_card)
                setPadding(28, 20, 28, 20)
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, 12) }
            }
            val info = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                addView(TextView(context).apply { text = "${a.atividade} (${Formatos.hora(a.inicio)})"; setTextAppearance(R.style.HB_Text_H3) })
                addView(TextView(context).apply {
                    text = "${Formatos.dataCompleta(a.inicio)} · ${a.quantidade} pessoa(s) · " +
                        if (a.valorCentavos == 0L) "grátis" else Formatos.reais(a.valorCentavos)
                    setTextAppearance(R.style.HB_Text_Muted)
                })
            }
            card.addView(info)
            val futura = try { OffsetDateTime.parse(a.inicio).isAfter(OffsetDateTime.now()) } catch (_: Exception) { true }
            if (futura) {
                card.addView(TextView(this).apply {
                    text = "Cancelar"
                    background = ContextCompat.getDrawable(context, R.drawable.bg_badge_red)
                    setTextColor(ContextCompat.getColor(context, R.color.white))
                    setPadding(24, 12, 24, 12)
                    textSize = 12f
                    setTypeface(null, Typeface.BOLD)
                    setOnClickListener { cancelar(a) }
                })
            }
            container.addView(card)
        }
    }

    private fun cancelar(a: AgendamentoDto) {
        lifecycleScope.launch {
            try {
                val r = ApiClient.getApi(this@ManageBookingActivity).cancelarAgendamento(reservaCodigo, a.id)
                if (r.isSuccessful) {
                    Toast.makeText(this@ManageBookingActivity, "${a.atividade} cancelada. A vaga voltou para a tribo.", Toast.LENGTH_SHORT).show()
                    carregar()
                } else {
                    erro(ApiErrors.mensagem(r)) // 422 FORA_DO_PRAZO a menos de 4 h
                }
            } catch (e: Exception) {
                erro(ApiErrors.mensagem(e))
            }
        }
    }

    private fun erro(msg: String) {
        AlertDialog.Builder(this).setTitle("Ugh!").setMessage(msg).setPositiveButton("OK", null).show()
    }
}
