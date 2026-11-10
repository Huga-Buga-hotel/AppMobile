package com.example.hugabugahotel.ui.experiences

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.hugabugahotel.R
import com.example.hugabugahotel.data.api.ApiClient
import com.example.hugabugahotel.data.api.ApiErrors
import com.example.hugabugahotel.data.api.SessionManager
import com.example.hugabugahotel.data.model.*
import com.example.hugabugahotel.databinding.ActivityExperienceScheduleBinding
import com.example.hugabugahotel.util.Formatos
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Agendar uma atividade dentro da estadia. Os dias vêm do check-in/check-out da reserva, os horários e
 * vagas de GET /atividades/{id}/sessoes e a agenda do dia de GET /reservas/{codigo}/agenda.
 * O valor vai para a conta da toca (pago no hotel). Sem EXPERIENCE_ID, só mostra a agenda (modo gerenciar).
 */
class ExperienceScheduleActivity : AppCompatActivity() {

    private lateinit var binding: ActivityExperienceScheduleBinding
    private var experienceId: String? = null
    private var experienceNome: String = ""
    private var reservaCodigo: String = ""
    private var reserva: ReservaResponse? = null
    private var diaSelecionado: String = ""
    private var agendamentos: List<AgendamentoDto> = emptyList()

    private val BR = Locale.forLanguageTag("pt-BR")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExperienceScheduleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        reservaCodigo = intent.getStringExtra("RESERVA_CODIGO") ?: run { finish(); return }
        experienceId = intent.getStringExtra("EXPERIENCE_ID")
        experienceNome = intent.getStringExtra("EXPERIENCE_NOME") ?: "Experiência"
        val gerenciar = experienceId == null

        binding.tvExpScheduleTitle.text = if (gerenciar) "Agenda da tribo" else "Agendar $experienceNome"
        binding.tvHorariosDisponiveisTitle.visibility = if (gerenciar) View.GONE else View.VISIBLE
        binding.layoutSessionsContainer.visibility = if (gerenciar) View.GONE else View.VISIBLE
        binding.root.findViewById<View>(R.id.btnAddExperienceManagement)?.apply {
            visibility = if (gerenciar) View.VISIBLE else View.GONE
            setOnClickListener {
                startActivity(Intent(this@ExperienceScheduleActivity, ExperiencesActivity::class.java)
                    .putExtra("RESERVA_CODIGO", reservaCodigo))
            }
        }
        // As atividades são pagas no hotel: não há pagamento no app
        binding.root.findViewById<View>(R.id.layoutPendingPaymentBanner)?.visibility = View.GONE
        binding.btnConfirmExperienceSchedule.text = "Concluir"
        binding.btnConfirmExperienceSchedule.setOnClickListener { finish() }

        carregarReserva()
    }

    private fun carregarReserva() {
        lifecycleScope.launch {
            try {
                val response = ApiClient.getApi(this@ExperienceScheduleActivity).getVoucherReserva(reservaCodigo)
                val r = response.body() ?: return@launch erro(ApiErrors.mensagem(response))
                reserva = r
                binding.tvExpScheduleSub.text = "Reserva ${r.codigo} (${r.cavernaNome}). " +
                    if (r.status == "CONFIRMADA") "Escolha o dia e o horário." else "Só reservas confirmadas podem agendar atividades."
                montarDias(r)
            } catch (e: Exception) {
                erro(ApiErrors.mensagem(e))
            }
        }
    }

    private fun montarDias(r: ReservaResponse) {
        val inicio = Formatos.parseData(r.checkIn) ?: LocalDate.now()
        val fim = Formatos.parseData(r.checkOut) ?: inicio
        val hoje = LocalDate.now()
        val nome = DateTimeFormatter.ofPattern("EEE", BR)
        val completo = DateTimeFormatter.ofPattern("EEEE, d 'DE' MMMM", BR)
        val dias = inicio.datesUntil(fim.plusDays(1)).toList()
            .filter { !it.isBefore(hoje) }
            .ifEmpty { listOf(inicio) }
            .mapIndexed { i, d ->
                DayItem(nome.format(d).replace(".", "").uppercase(BR), d.dayOfMonth.toString(),
                    completo.format(d).uppercase(BR), d.toString(), isSelected = i == 0)
            }
        diaSelecionado = dias.first().isoDate
        binding.root.findViewById<TextView>(R.id.tvAgendaDateEyebrow)?.text = dias.first().fullLabel
        binding.daysList.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.daysList.adapter = DaysAdapter(dias) { dia ->
            diaSelecionado = dia.isoDate
            binding.root.findViewById<TextView>(R.id.tvAgendaDateEyebrow)?.text = dia.fullLabel
            atualizarDia()
        }
        atualizarDia()
    }

    private fun atualizarDia() {
        carregarAgenda()
        if (experienceId != null) carregarSessoes()
    }

    // ---------- sessões disponíveis ----------

    private fun carregarSessoes() {
        val id = experienceId ?: return
        lifecycleScope.launch {
            try {
                val r = ApiClient.getApi(this@ExperienceScheduleActivity).getSessoesAtividade(id, diaSelecionado, diaSelecionado)
                if (r.isSuccessful) renderSessoes(r.body().orEmpty()) else erro(ApiErrors.mensagem(r))
            } catch (e: Exception) {
                erro(ApiErrors.mensagem(e))
            }
        }
    }

    private fun renderSessoes(sessoes: List<SessaoAtividade>) {
        val container = binding.layoutSessionsContainer
        container.removeAllViews()
        val futuras = sessoes.filter { s -> runCatching { OffsetDateTime.parse(s.inicio).isAfter(OffsetDateTime.now()) }.getOrDefault(true) }
        if (futuras.isEmpty()) {
            container.addView(TextView(this).apply {
                text = "Nenhum horário para este dia."
                setTextAppearance(R.style.HB_Text_Muted)
                setPadding(8, 16, 8, 16)
            })
            return
        }
        for (s in futuras) {
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setBackgroundResource(R.drawable.bg_card)
                setPadding(32, 24, 32, 24)
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, 16) }
            }
            card.addView(LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                addView(TextView(context).apply { text = "Horário: ${Formatos.hora(s.inicio)}"; setTextAppearance(R.style.HB_Text_H3) })
                addView(TextView(context).apply {
                    text = if (s.esgotada) "Esgotada" else "${s.vagasLivres} de ${s.vagasTotais} vagas livres"
                    setTextAppearance(R.style.HB_Text_Muted)
                })
            })
            card.addView(Button(this).apply {
                text = if (s.esgotada) "Avisar" else "Reservar"
                isAllCaps = false
                typeface = Typeface.DEFAULT_BOLD
                setBackgroundResource(if (s.esgotada) R.drawable.bg_chip else R.drawable.bg_chip_selected)
                setTextColor(ContextCompat.getColor(context, if (s.esgotada) R.color.hb_ink else R.color.white))
                setOnClickListener { if (s.esgotada) avisarVaga(s) else escolherQuantidade(s) }
            })
            container.addView(card)
        }
    }

    private fun escolherQuantidade(s: SessaoAtividade) {
        val r = reserva ?: return
        val max = minOf(s.vagasLivres, (r.adultos + r.criancas).coerceAtLeast(1))
        val opcoes = (1..max).map { if (it == 1) "1 pessoa" else "$it pessoas" }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Quantas pessoas da tribo?")
            .setItems(opcoes) { _, i -> agendar(s, i + 1) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun agendar(s: SessaoAtividade, quantidade: Int) {
        lifecycleScope.launch {
            try {
                val r = ApiClient.getApi(this@ExperienceScheduleActivity)
                    .agendarAtividade(reservaCodigo, AgendamentoRequest(s.id, quantidade))
                val a = r.body()
                if (r.isSuccessful && a != null) {
                    val valor = if (a.valorCentavos == 0L) "grátis" else "${Formatos.reais(a.valorCentavos)} na conta da toca"
                    Toast.makeText(this@ExperienceScheduleActivity,
                        "$experienceNome às ${Formatos.hora(a.inicio)} agendada ($valor) 🪨", Toast.LENGTH_LONG).show()
                    atualizarDia()
                } else {
                    // 409 VAGAS_ESGOTADAS, 400 faixa de idade, 422 reserva não confirmada
                    erro(ApiErrors.mensagem(r))
                }
            } catch (e: Exception) {
                erro(ApiErrors.mensagem(e))
            }
        }
    }

    /** Botão "Avisar": grava o pedido de aviso de vaga na API. */
    private fun avisarVaga(s: SessaoAtividade) {
        val session = SessionManager(this)
        val email = session.getUserEmail() ?: session.getResponsavelEmail() ?: reserva?.responsavelEmail
            ?: return erro("Informe um e-mail para receber o aviso.")
        lifecycleScope.launch {
            try {
                val r = ApiClient.getApi(this@ExperienceScheduleActivity)
                    .criarAlertaVaga(AlertaVagaRequest(email = email, tipo = "ATIVIDADE", sessaoId = s.id))
                if (r.isSuccessful) {
                    session.saveNotification(NotificacaoDto("alerta_${s.id}", "Alerta de vaga criado",
                        "Avisaremos em $email se abrir vaga em $experienceNome às ${Formatos.hora(s.inicio)}."))
                    Toast.makeText(this@ExperienceScheduleActivity, "Pronto! Avisaremos em $email.", Toast.LENGTH_LONG).show()
                } else {
                    erro(ApiErrors.mensagem(r))
                }
            } catch (e: Exception) {
                erro(ApiErrors.mensagem(e))
            }
        }
    }

    // ---------- agenda do dia ----------

    private fun carregarAgenda() {
        lifecycleScope.launch {
            try {
                val api = ApiClient.getApi(this@ExperienceScheduleActivity)
                agendamentos = api.getAgendamentos(reservaCodigo).body().orEmpty().filter { it.status == "ATIVO" }
                val r = api.getAgendaTribo(reservaCodigo, diaSelecionado)
                renderAgenda(r.body()?.itens.orEmpty())
            } catch (e: Exception) {
                renderAgenda(emptyList())
            }
        }
    }

    private fun renderAgenda(itens: List<AgendaItemDto>) {
        val container = binding.root.findViewById<LinearLayout>(R.id.layoutAgendaItems) ?: return
        container.removeAllViews()
        binding.root.findViewById<View>(R.id.tvAgendaEmpty)?.visibility = if (itens.isEmpty()) View.VISIBLE else View.GONE
        for (item in itens) {
            val linha = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = 24 }
            }
            linha.addView(TextView(this).apply { text = item.hora; setTextAppearance(R.style.HB_Text_H3); minWidth = 140 })
            val caixa = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setBackgroundResource(R.drawable.bg_card)
                setPadding(20, 16, 20, 16)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            caixa.addView(LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                addView(TextView(context).apply { text = item.titulo; setTextAppearance(R.style.HB_Text); setTypeface(null, Typeface.BOLD) })
                item.detalhe?.let { d -> addView(TextView(context).apply { text = d; setTextAppearance(R.style.HB_Text_Muted) }) }
            })
            // a agenda não traz id: o cancelamento usa o agendamento do mesmo horário e atividade
            val agendamento = if (item.tipo == "ATIVIDADE") agendamentos.firstOrNull {
                it.atividade == item.titulo && Formatos.hora(it.inicio) == item.hora && it.inicio.startsWith(diaSelecionado)
            } else null
            if (agendamento != null) {
                caixa.addView(TextView(this).apply {
                    text = "Cancelar"
                    setBackgroundResource(R.drawable.bg_badge_red)
                    setTextColor(ContextCompat.getColor(context, R.color.white))
                    setPadding(24, 12, 24, 12)
                    textSize = 12f
                    setTypeface(null, Typeface.BOLD)
                    setOnClickListener { cancelar(agendamento) }
                })
            }
            linha.addView(caixa)
            container.addView(linha)
        }
    }

    private fun cancelar(a: AgendamentoDto) {
        lifecycleScope.launch {
            try {
                val r = ApiClient.getApi(this@ExperienceScheduleActivity).cancelarAgendamento(reservaCodigo, a.id)
                if (r.isSuccessful) {
                    Toast.makeText(this@ExperienceScheduleActivity, "Agendamento cancelado. A vaga voltou.", Toast.LENGTH_SHORT).show()
                    atualizarDia()
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
