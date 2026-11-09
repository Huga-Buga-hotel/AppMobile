package com.example.hugabugahotel.ui.experiences

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
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
import com.example.hugabugahotel.data.api.SessionManager
import com.example.hugabugahotel.data.model.AgendamentoRequest
import com.example.hugabugahotel.data.model.AlertaVagaRequest
import com.example.hugabugahotel.data.model.NotificacaoDto
import com.example.hugabugahotel.data.model.SessaoAtividade
import com.example.hugabugahotel.databinding.ActivityExperienceScheduleBinding
import com.example.hugabugahotel.ui.checkout.CheckoutStep3Activity
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ExperienceScheduleActivity : AppCompatActivity() {

    private lateinit var binding: ActivityExperienceScheduleBinding
    private lateinit var daysAdapter: DaysAdapter
    private lateinit var sessionManager: SessionManager

    private var experienceId: String = ""
    private var experienceNome: String = ""
    private var reservaCodigo: String = ""
    private var cavernName: String = ""
    private var checkInDisplay: String = "7 out"
    private var checkOutDisplay: String = "14 out"
    private var selectedDayIso: String = "2026-10-07"
    private var isManagementMode: Boolean = false

    // Agenda separada individualmente por cada dia de estadia (ISO Date -> (Hora -> Título e Subtítulo))
    private val agendaByDayMap = mutableMapOf<String, MutableMap<String, Pair<String, String>>>()

    private val currentDayAgenda: MutableMap<String, Pair<String, String>>
        get() = agendaByDayMap.getOrPut(selectedDayIso) { mutableMapOf() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExperienceScheduleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        experienceId = intent.getStringExtra("EXPERIENCE_ID") ?: "1"
        experienceNome = intent.getStringExtra("EXPERIENCE_NOME") ?: "Experiência"
        reservaCodigo = intent.getStringExtra("RESERVA_CODIGO") ?: "HB-4F7K"
        cavernName = intent.getStringExtra("CAVERN_NOME") ?: "Toca do Mamute"
        checkInDisplay = intent.getStringExtra("CHECK_IN") ?: "7 out"
        checkOutDisplay = intent.getStringExtra("CHECK_OUT") ?: "14 out"
        isManagementMode = intent.getBooleanExtra("IS_MANAGEMENT_MODE", false)

        // Restaura agendamentos persistidos para esta reserva
        val savedAgenda = sessionManager.getScheduledExperiences(reservaCodigo)
        if (savedAgenda.isNotEmpty()) {
            agendaByDayMap.putAll(savedAgenda)
        }

        if (isManagementMode) {
            binding.tvExpScheduleTitle.text = "Gerenciar Experiências"
            binding.tvExpScheduleSub.text = "Reserva $reservaCodigo ($cavernName). Visualize os dias da estadia e as experiências agendadas da tribo."
            binding.tvHorariosDisponiveisTitle.visibility = View.GONE
            binding.layoutSessionsContainer.visibility = View.GONE
            binding.root.findViewById<View>(R.id.btnAddExperienceManagement)?.visibility = View.VISIBLE
            binding.root.findViewById<View>(R.id.btnAddExperienceManagement)?.setOnClickListener {
                val intent = Intent(this, ExperiencesActivity::class.java).apply {
                    putExtra("RESERVA_CODIGO", reservaCodigo)
                    putExtra("CAVERN_NOME", cavernName)
                    putExtra("CHECK_IN", checkInDisplay)
                    putExtra("CHECK_OUT", checkOutDisplay)
                }
                startActivity(intent)
            }
        } else {
            binding.tvExpScheduleTitle.text = "Agendar $experienceNome"
            binding.tvExpScheduleSub.text = "Reserva $reservaCodigo ($cavernName). Escolha o dia e horário da sua estadia para confirmar sua vaga na tribo."
        }

        binding.btnConfirmExperienceSchedule.setOnClickListener {
            val hasAnyBookings = agendaByDayMap.values.any { it.isNotEmpty() }
            if (hasAnyBookings) {
                sessionManager.saveScheduledExperiences(reservaCodigo, agendaByDayMap)
                Toast.makeText(this, "Reserva de experiências confirmada com sucesso! 🪨", Toast.LENGTH_LONG).show()
                finish()
            } else {
                Toast.makeText(this, "Nenhuma experiência foi reservada ainda.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.root.findViewById<TextView>(R.id.btnPayPendingInApp)?.setOnClickListener {
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

        resetAgendaUi()
        setupDaysList()
        updateConfirmButtonState()
    }

    override fun onResume() {
        super.onResume()
        val savedAgenda = sessionManager.getScheduledExperiences(reservaCodigo)
        if (savedAgenda.isNotEmpty()) {
            agendaByDayMap.clear()
            agendaByDayMap.putAll(savedAgenda)
        }
        updateAgendaUi()
        checkAndUpdatePendingBanner()
        updateConfirmButtonState()
        if (!isManagementMode) {
            fetchSessoesForDay()
        }
    }

    private fun updateConfirmButtonState() {
        val hasAnyBookings = agendaByDayMap.values.any { it.isNotEmpty() }
        val btnConfirm = binding.btnConfirmExperienceSchedule
        btnConfirm.isEnabled = hasAnyBookings
        btnConfirm.alpha = if (hasAnyBookings) 1.0f else 0.4f
    }

    private fun setupDaysList() {
        val days = generateDaysFromReservation(checkInDisplay, checkOutDisplay)

        selectedDayIso = days[0].isoDate
        binding.root.findViewById<TextView>(R.id.tvAgendaDateEyebrow)?.text = days[0].fullLabel

        daysAdapter = DaysAdapter(days) { day ->
            selectedDayIso = day.isoDate
            binding.root.findViewById<TextView>(R.id.tvAgendaDateEyebrow)?.text = day.fullLabel
            if (!isManagementMode) {
                fetchSessoesForDay()
            }
            fetchAgendaDoDia()
        }

        binding.daysList.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.daysList.adapter = daysAdapter

        if (!isManagementMode) {
            fetchSessoesForDay()
        }
        fetchAgendaDoDia()
    }

    private fun generateDaysFromReservation(checkInStr: String, checkOutStr: String): List<DayItem> {
        val isoSdf = SimpleDateFormat("yyyy-MM-dd", Locale("pt", "BR"))
        val displaySdf = SimpleDateFormat("d MMM", Locale("pt", "BR"))
        val shortDaySdf = SimpleDateFormat("EEE", Locale("pt", "BR"))
        val dayNumSdf = SimpleDateFormat("d", Locale("pt", "BR"))
        val fullLabelSdf = SimpleDateFormat("EEEE, d 'DE' MMMM", Locale("pt", "BR"))

        var startDate: Date? = try { isoSdf.parse(checkInStr) } catch (_: Exception) { null }
        if (startDate == null) {
            startDate = try {
                val parsed = displaySdf.parse(checkInStr.replace(".", ""))
                val cal = Calendar.getInstance().apply {
                    time = parsed!!
                    set(Calendar.YEAR, 2026)
                }
                cal.time
            } catch (_: Exception) { null }
        }

        var endDate: Date? = try { isoSdf.parse(checkOutStr) } catch (_: Exception) { null }
        if (endDate == null) {
            endDate = try {
                val parsed = displaySdf.parse(checkOutStr.replace(".", ""))
                val cal = Calendar.getInstance().apply {
                    time = parsed!!
                    set(Calendar.YEAR, 2026)
                }
                cal.time
            } catch (_: Exception) { null }
        }

        val startCal = Calendar.getInstance().apply {
            if (startDate != null) {
                time = startDate
            } else {
                set(2026, Calendar.OCTOBER, 7)
            }
        }
        val endCal = Calendar.getInstance().apply {
            if (endDate != null) {
                time = endDate
            } else {
                set(2026, Calendar.OCTOBER, 14)
            }
        }

        if (endCal.before(startCal)) {
            endCal.time = startCal.time
            endCal.add(Calendar.DAY_OF_MONTH, 7)
        }

        val days = mutableListOf<DayItem>()
        val currCal = startCal.clone() as Calendar

        while (!currCal.after(endCal)) {
            val date = currCal.time
            val name = shortDaySdf.format(date).replace(".", "").uppercase(Locale("pt", "BR"))
            val number = dayNumSdf.format(date)
            val fullLabel = fullLabelSdf.format(date).uppercase(Locale("pt", "BR"))
            val isoDate = isoSdf.format(date)
            val isFirst = days.isEmpty()

            days.add(DayItem(name, number, fullLabel, isoDate, isSelected = isFirst))
            currCal.add(Calendar.DAY_OF_MONTH, 1)
        }

        return if (days.isNotEmpty()) days else listOf(
            DayItem("QUA", "7", "QUARTA-FEIRA, 7 DE OUTUBRO", "2026-10-07", isSelected = true),
            DayItem("QUI", "8", "QUINTA-FEIRA, 8 DE OUTUBRO", "2026-10-08", isSelected = false),
            DayItem("SEX", "9", "SEXTA-FEIRA, 9 DE OUTUBRO", "2026-10-09", isSelected = false),
            DayItem("SÁB", "10", "SÁBADO, 10 DE OUTUBRO", "2026-10-10", isSelected = false),
            DayItem("DOM", "11", "DOMINGO, 11 DE OUTUBRO", "2026-10-11", isSelected = false),
            DayItem("SEG", "12", "SEGUNDA-FEIRA, 12 DE OUTUBRO", "2026-10-12", isSelected = false),
            DayItem("TER", "13", "TERÇA-FEIRA, 13 DE OUTUBRO", "2026-10-13", isSelected = false),
            DayItem("QUA", "14", "QUARTA-FEIRA, 14 DE OUTUBRO", "2026-10-14", isSelected = false)
        )
    }

    private fun fetchSessoesForDay() {
        if (isManagementMode) return
        lifecycleScope.launch {
            val sessoesList = try {
                val api = ApiClient.getApi(this@ExperienceScheduleActivity)
                val response = api.getSessoesAtividade(experienceId, selectedDayIso, selectedDayIso)
                if (response.isSuccessful && response.body() != null) {
                    response.body()!!
                } else {
                    emptyList()
                }
            } catch (_: Exception) {
                emptyList()
            }

            renderSessionsUi(sessoesList)
        }
    }

    private fun renderSessionsUi(sessoes: List<SessaoAtividade>) {
        if (isManagementMode) return
        val container = binding.layoutSessionsContainer
        container.removeAllViews()

        val sessoesDisponiveis = sessoes.filter { sessao ->
            val vagas = sessao.vagasLivres ?: sessao.vagasDisponiveis ?: 0
            vagas > 0 && !sessao.esgotada
        }

        if (sessoesDisponiveis.isEmpty()) {
            val emptyLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = android.view.Gravity.CENTER
                setPadding(32, 28, 32, 28)
                setBackgroundResource(R.drawable.bg_card)
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                layoutParams = params
            }

            val tvEmpty = TextView(this).apply {
                text = "Nenhum horário disponível para este dia."
                setTextAppearance(R.style.HB_Text_H3)
                gravity = android.view.Gravity.CENTER
            }

            val tvSub = TextView(this).apply {
                text = "Deseja ser avisado assim que houver vaga?"
                setTextAppearance(R.style.HB_Text_Muted)
                gravity = android.view.Gravity.CENTER
                setPadding(0, 4, 0, 16)
            }

            val btnAlert = Button(this).apply {
                text = "Criar alerta de vaga"
                isAllCaps = false
                typeface = Typeface.DEFAULT_BOLD
                setBackgroundResource(R.drawable.bg_button_primary)
                setTextColor(ContextCompat.getColor(this@ExperienceScheduleActivity, R.color.white))
                gravity = android.view.Gravity.CENTER
                setPadding(24, 16, 24, 16)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = 16
                }
                setOnClickListener {
                    criarAlertaVaga()
                }
            }

            emptyLayout.addView(tvEmpty)
            emptyLayout.addView(tvSub)
            emptyLayout.addView(btnAlert)
            container.addView(emptyLayout)
            return
        }

        for (sessao in sessoesDisponiveis) {
            val hora = if (!sessao.inicio.isNullOrEmpty()) sessao.inicio.substringAfter("T").take(5) else "10:00"
            val vagas = sessao.vagasLivres ?: sessao.vagasDisponiveis ?: 4

            val card = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setBackgroundResource(R.drawable.bg_card)
                setPadding(32, 24, 32, 24)
                gravity = android.view.Gravity.CENTER_VERTICAL
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.setMargins(0, 0, 0, 16)
                layoutParams = params
            }

            val infoLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val tvHora = TextView(this).apply {
                text = "Horário: $hora"
                setTextAppearance(R.style.HB_Text_H3)
            }

            val tvVagas = TextView(this).apply {
                text = "$vagas vagas disponíveis"
                setTextAppearance(R.style.HB_Text_Muted)
            }

            infoLayout.addView(tvHora)
            infoLayout.addView(tvVagas)

            val btnAgendar = Button(this).apply {
                text = "Reservar"
                isAllCaps = false
                typeface = Typeface.DEFAULT_BOLD
                setBackgroundResource(R.drawable.bg_chip_selected)
                setTextColor(ContextCompat.getColor(this@ExperienceScheduleActivity, R.color.white))
                setOnClickListener {
                    agendarSessao(sessao, hora)
                }
            }

            card.addView(infoLayout)
            card.addView(btnAgendar)
            container.addView(card)
        }
    }

    private fun criarAlertaVaga() {
        lifecycleScope.launch {
            try {
                val api = ApiClient.getApi(this@ExperienceScheduleActivity)
                api.criarAlertaVaga(AlertaVagaRequest(experienceId, selectedDayIso))
            } catch (_: Exception) {}

            val notif = NotificacaoDto(
                id = "alert_${System.currentTimeMillis()}",
                titulo = "Alerta de Vaga Criado",
                descricao = "Monitorando vagas para $experienceNome no dia $selectedDayIso. Avisaremos assim que abrir!",
                lida = false
            )
            sessionManager.saveNotification(notif)

            Toast.makeText(this@ExperienceScheduleActivity, "Alerta criado com sucesso! Você será avisado nas notificações.", Toast.LENGTH_LONG).show()

            // Simula abertura de vaga após 3 segundos
            kotlinx.coroutines.delay(3000L)
            val spotNotif = NotificacaoDto(
                id = "spot_${System.currentTimeMillis()}",
                titulo = "Vaga Disponível! 🪨",
                descricao = "Uma vaga abriu para $experienceNome no dia $selectedDayIso! Corra para reservar.",
                lida = false
            )
            sessionManager.saveNotification(spotNotif)
        }
    }

    private fun agendarSessao(sessao: SessaoAtividade, hora: String) {
        lifecycleScope.launch {
            try {
                val api = ApiClient.getApi(this@ExperienceScheduleActivity)
                val request = AgendamentoRequest(sessaoId = sessao.id, quantidade = 1)
                api.agendarAtividade(reservaCodigo, request)
            } catch (_: Exception) {}

            val payMethod = sessionManager.getPaymentMethod(reservaCodigo)
            val isPixOrCard = (payMethod == "PIX" || payMethod == "CARTAO" || payMethod == "CARD")
            val isPaidInApp = sessionManager.isExperiencePaidInApp(reservaCodigo)

            val statusDetail = if (isPixOrCard && !isPaidInApp) {
                "1 pessoa · Pendente (Pagar no app ou recepção)"
            } else if (isPixOrCard && isPaidInApp) {
                "1 pessoa · Confirmado (Pago no app)"
            } else {
                "1 pessoa · Confirmado (Na chegada)"
            }

            currentDayAgenda[hora] = Pair(experienceNome, statusDetail)
            sessionManager.saveScheduledExperiences(reservaCodigo, agendaByDayMap)

            // R$ 60,00 (6000 centavos) somados ao custo total da reserva
            sessionManager.addExtraExpenseToBooking(reservaCodigo, 6000L)

            updateAgendaUi()
            fetchSessoesForDay()
            updateConfirmButtonState()

            if (isPixOrCard && !isPaidInApp) {
                Toast.makeText(this@ExperienceScheduleActivity, "Reserva pendente de pagamento! Pague pelo app ou presencialmente na recepção. 🪨", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this@ExperienceScheduleActivity, "Reserva confirmada para $experienceNome às $hora! 🪨", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun cancelarReservaSessao(horaKey: String) {
        lifecycleScope.launch {
            try {
                val api = ApiClient.getApi(this@ExperienceScheduleActivity)
                api.cancelarAgendamento(reservaCodigo, "agendamento_$horaKey")
            } catch (_: Exception) {}

            currentDayAgenda.remove(horaKey)
            currentDayAgenda.remove("${horaKey.take(2)}h")
            sessionManager.saveScheduledExperiences(reservaCodigo, agendaByDayMap)

            // Remove o valor (R$ 60,00) do custo total
            sessionManager.addExtraExpenseToBooking(reservaCodigo, -6000L)

            updateAgendaUi()
            updateConfirmButtonState()
            if (!isManagementMode) {
                fetchSessoesForDay()
            }

            Toast.makeText(this@ExperienceScheduleActivity, "Reserva cancelada com sucesso!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setFreeSlotUi(boxId: Int, titleId: Int, subId: Int, btnActionId: Int) {
        val root = binding.root
        root.findViewById<View>(boxId)?.setBackgroundResource(R.drawable.bg_dashed)
        val tvTitle = root.findViewById<TextView>(titleId)
        tvTitle?.text = "Horário Livre"
        tvTitle?.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_plus, 0, 0, 0)
        tvTitle?.compoundDrawablePadding = 12
        root.findViewById<TextView>(subId)?.text = "LIVRE"

        val btnAction = root.findViewById<TextView>(btnActionId)
        btnAction?.visibility = View.GONE
    }

    private fun setBookedSlotUi(boxId: Int, titleId: Int, subId: Int, btnActionId: Int, horaKey: String, item: Pair<String, String>) {
        val root = binding.root
        root.findViewById<View>(boxId)?.setBackgroundResource(R.drawable.bg_card)
        val tvTitle = root.findViewById<TextView>(titleId)
        tvTitle?.text = item.first
        tvTitle?.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
        root.findViewById<TextView>(subId)?.text = item.second

        val btnAction = root.findViewById<TextView>(btnActionId)
        btnAction?.visibility = View.VISIBLE
        btnAction?.text = "Cancelar"
        btnAction?.setBackgroundResource(R.drawable.bg_badge_red)
        btnAction?.setTextColor(ContextCompat.getColor(this, R.color.white))
        btnAction?.setOnClickListener {
            cancelarReservaSessao(horaKey)
        }
    }

    private fun resetAgendaUi() {
        setFreeSlotUi(R.id.boxAgenda8h, R.id.tvAgenda8hTitle, R.id.tvAgenda8hSub, R.id.btnCancelAgenda8h)
        setFreeSlotUi(R.id.boxAgenda10h, R.id.tvAgenda10hTitle, R.id.tvAgenda10hSub, R.id.btnCancelAgenda10h)
        setFreeSlotUi(R.id.boxAgenda14h, R.id.tvAgenda14hTitle, R.id.tvAgenda14hSub, R.id.btnCancelAgenda14h)
        setFreeSlotUi(R.id.boxAgenda16h, R.id.tvAgenda16hTitle, R.id.tvAgenda16hSub, R.id.btnCancelAgenda16h)
        setFreeSlotUi(R.id.boxAgenda18h, R.id.tvAgenda18hTitle, R.id.tvAgenda18hSub, R.id.btnCancelAgenda18h)
        setFreeSlotUi(R.id.boxAgenda20h, R.id.tvAgenda20hTitle, R.id.tvAgenda20hSub, R.id.btnCancelAgenda20h)
    }

    private fun updateAgendaUi() {
        val dayAgenda = currentDayAgenda
        val slots = listOf(
            listOf("08:00", "08h") to Tuple4(R.id.boxAgenda8h, R.id.tvAgenda8hTitle, R.id.tvAgenda8hSub, R.id.btnCancelAgenda8h, "08:00"),
            listOf("10:00", "10h") to Tuple4(R.id.boxAgenda10h, R.id.tvAgenda10hTitle, R.id.tvAgenda10hSub, R.id.btnCancelAgenda10h, "10:00"),
            listOf("14:00", "14h") to Tuple4(R.id.boxAgenda14h, R.id.tvAgenda14hTitle, R.id.tvAgenda14hSub, R.id.btnCancelAgenda14h, "14:00"),
            listOf("16:00", "16h") to Tuple4(R.id.boxAgenda16h, R.id.tvAgenda16hTitle, R.id.tvAgenda16hSub, R.id.btnCancelAgenda16h, "16:00"),
            listOf("18:00", "18h") to Tuple4(R.id.boxAgenda18h, R.id.tvAgenda18hTitle, R.id.tvAgenda18hSub, R.id.btnCancelAgenda18h, "18:00"),
            listOf("20:00", "20h") to Tuple4(R.id.boxAgenda20h, R.id.tvAgenda20hTitle, R.id.tvAgenda20hSub, R.id.btnCancelAgenda20h, "20:00")
        )

        for ((keys, config) in slots) {
            val item = keys.firstNotNullOfOrNull { dayAgenda[it] }
            if (item != null) {
                setBookedSlotUi(config.boxId, config.titleId, config.subId, config.btnActionId, config.horaKey, item)
            } else {
                setFreeSlotUi(config.boxId, config.titleId, config.subId, config.btnActionId)
            }
        }

        checkAndUpdatePendingBanner()
        updateConfirmButtonState()
    }

    private fun checkAndUpdatePendingBanner() {
        val payMethod = sessionManager.getPaymentMethod(reservaCodigo)
        val isPixOrCard = (payMethod == "PIX" || payMethod == "CARTAO" || payMethod == "CARD")
        val hasBookings = agendaByDayMap.values.any { it.isNotEmpty() }
        val isPaidInApp = sessionManager.isExperiencePaidInApp(reservaCodigo)

        val banner = binding.root.findViewById<View>(R.id.layoutPendingPaymentBanner)
        val btnPayInApp = binding.root.findViewById<TextView>(R.id.btnPayPendingInApp)
        val tvText = binding.root.findViewById<TextView>(R.id.tvPendingPaymentText)

        if (isPixOrCard && hasBookings && !isPaidInApp) {
            val totalExtra = sessionManager.getExtraExpenses(reservaCodigo)
            val valorFormatted = String.format(Locale("pt", "BR"), "R$ %.2f", (if (totalExtra > 0) totalExtra else 6000L) / 100.0)
            tvText?.text = "Pendente: $valorFormatted (Pague no app ou recepção)"
            btnPayInApp?.visibility = View.VISIBLE
            banner?.visibility = View.VISIBLE
        } else if (isPixOrCard && hasBookings && isPaidInApp) {
            tvText?.text = "✓ Experiências pagas pelo app"
            btnPayInApp?.visibility = View.GONE
            banner?.visibility = View.VISIBLE
        } else {
            banner?.visibility = View.GONE
        }
    }

    private data class Tuple4(
        val boxId: Int,
        val titleId: Int,
        val subId: Int,
        val btnActionId: Int,
        val horaKey: String
    )

    private fun fetchAgendaDoDia() {
        lifecycleScope.launch {
            try {
                val api = ApiClient.getApi(this@ExperienceScheduleActivity)
                val response = api.getAgendaTribo(reservaCodigo, selectedDayIso)
                if (response.isSuccessful && response.body() != null) {
                    val agendaResponse = response.body()!!
                    val dayAgenda = currentDayAgenda
                    for (item in agendaResponse.itens) {
                        val horaNorm = item.hora.take(5)
                        dayAgenda[horaNorm] = Pair(item.titulo, item.detalhe ?: item.subtitulo ?: "Confirmado")
                    }
                    updateAgendaUi()
                } else {
                    updateAgendaUi()
                }
            } catch (_: Exception) {
                updateAgendaUi()
            }
        }
    }
}
