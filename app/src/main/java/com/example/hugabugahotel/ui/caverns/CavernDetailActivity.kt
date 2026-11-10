package com.example.hugabugahotel.ui.caverns

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import coil.load
import com.example.hugabugahotel.R
import android.widget.EditText
import com.example.hugabugahotel.data.api.ApiClient
import com.example.hugabugahotel.data.api.ApiErrors
import com.example.hugabugahotel.data.api.SessionManager
import com.example.hugabugahotel.data.model.CavernDetailResponse
import com.example.hugabugahotel.data.model.CriarReservaRequest
import com.example.hugabugahotel.databinding.ActivityCavernDetailBinding
import com.example.hugabugahotel.ui.checkout.CheckoutStep1Activity
import com.example.hugabugahotel.util.LanguageManager
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CavernDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCavernDetailBinding
    private var cavernSlug: String = "toca-do-mamute"
    private var cavernId: String = ""
    private var isFavorited = false

    private var checkInDisplay: String = ""
    private var checkOutDisplay: String = ""
    private var checkInIso: String = ""
    private var checkOutIso: String = ""
    private var adultsCount: Int = 2
    private var childrenCount: Int = 2
    private var totalEstadiaCentavos: Long = 0L
    private var noitesEstadia: Int = 3

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCavernDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cavernSlug = intent.getStringExtra("CAVERN_SLUG") ?: "toca-do-mamute"
        checkInDisplay = intent.getStringExtra("CHECK_IN") ?: ""
        checkOutDisplay = intent.getStringExtra("CHECK_OUT") ?: ""
        checkInIso = intent.getStringExtra("CHECK_IN_ISO") ?: ""
        checkOutIso = intent.getStringExtra("CHECK_OUT_ISO") ?: ""
        val sessionManager = SessionManager(this)
        if (sessionManager.isLoggedIn() && sessionManager.getDefaultAdults() >= 1) {
            adultsCount = intent.getIntExtra("ADULTS", sessionManager.getDefaultAdults())
            childrenCount = intent.getIntExtra("CHILDREN", sessionManager.getDefaultChildren())
        } else {
            adultsCount = intent.getIntExtra("ADULTS", 2)
            childrenCount = intent.getIntExtra("CHILDREN", 2)
        }

        updateBookingSummaryUi()
        fetchTribeMembersFromProfile()

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnShare.setOnClickListener {
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, "Venha conhecer esta caverna incrível no Huga Buga Hotel! Reserve já.")
                type = "text/plain"
            }
            startActivity(Intent.createChooser(shareIntent, "Compartilhar Caverna"))
        }

        binding.btnFavorite.setOnClickListener {
            if (!SessionManager(this).isLoggedIn()) {
                Toast.makeText(this, "Entre na sua conta para favoritar cavernas", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val targetFavorited = !isFavorited
            lifecycleScope.launch {
                try {
                    val api = ApiClient.getApi(this@CavernDetailActivity)
                    val response = if (targetFavorited) api.adicionarFavorito(cavernId) else api.removerFavorito(cavernId)
                    if (!response.isSuccessful) {
                        Toast.makeText(this@CavernDetailActivity, ApiErrors.mensagem(response), Toast.LENGTH_LONG).show()
                        return@launch
                    }
                    isFavorited = targetFavorited
                    binding.btnFavorite.setImageResource(if (isFavorited) R.drawable.ic_heart else R.drawable.ic_heart_outline)
                    val chave = if (isFavorited) "fav_added" else "fav_removed"
                    Toast.makeText(this@CavernDetailActivity, LanguageManager.get(this@CavernDetailActivity, chave), Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this@CavernDetailActivity, ApiErrors.mensagem(e), Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.layoutBookingSummary.setOnClickListener {
            showEditBookingOptions()
        }

        binding.bookingBar.setOnClickListener {
            iniciarReserva()
        }

        binding.btnReserve.setOnClickListener {
            iniciarReserva()
        }

        fetchDetail()
    }

    private fun fetchTribeMembersFromProfile() {
        val sessionManager = SessionManager(this)
        if (sessionManager.isLoggedIn()) {
            lifecycleScope.launch {
                try {
                    val api = ApiClient.getApi(this@CavernDetailActivity)
                    val response = api.getMembrosTribo()
                    if (response.isSuccessful && response.body() != null) {
                        val membros = response.body()!!
                        val ads = 1 + membros.count { it.tipo.contains("ADULTO", ignoreCase = true) }
                        val chis = membros.count { it.tipo.contains("CRIANCA", ignoreCase = true) }
                        sessionManager.saveDefaultTribeCount(ads, chis)
                        if (!intent.hasExtra("ADULTS")) {
                            adultsCount = ads
                            childrenCount = chis
                            updateBookingSummaryUi()
                            fetchCotacao()
                        }
                    }
                } catch (_: Exception) {}
            }
        }
    }

    private fun updateBookingSummaryUi() {
        val totalPessoas = adultsCount + childrenCount
        val totalStr = if (totalEstadiaCentavos > 0) " · Total: R$ ${String.format(Locale.getDefault(), "%,d", totalEstadiaCentavos / 100)}" else ""
        val noitesStr = if (noitesEstadia > 1) " ($noitesEstadia noites)" else " (1 noite)"
        val datesText = if (checkInDisplay.isNotEmpty() && checkOutDisplay.isNotEmpty()) "$checkInDisplay – $checkOutDisplay$noitesStr" else "Selecione as datas"
        binding.tvBookingSummary.text = "$datesText · $totalPessoas pessoas ($adultsCount ad., $childrenCount cri.)$totalStr"
    }

    private fun showEditBookingOptions() {
        val options = arrayOf("Alterar Datas da Estadia", "Alterar Quantidade de Hóspedes")
        AlertDialog.Builder(this)
            .setTitle("Editar Reserva")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showDatePicker()
                    1 -> showTribePicker()
                }
            }
            .show()
    }

    private fun showDatePicker() {
        val constraintsBuilder = com.google.android.material.datepicker.CalendarConstraints.Builder()
            .setValidator(com.google.android.material.datepicker.DateValidatorPointForward.from(System.currentTimeMillis()))

        val datePicker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText("Selecione as Datas da Estadia")
            .setCalendarConstraints(constraintsBuilder.build())
            .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            val startDate = selection.first
            val endDate = selection.second
            if (startDate != null && endDate != null) {
                // O date picker devolve meia-noite em UTC: formatar em UTC evita reservar um dia antes
                val utc = java.util.TimeZone.getTimeZone("UTC")
                val displaySdf = SimpleDateFormat("dd MMM", Locale.forLanguageTag("pt-BR")).apply { timeZone = utc }
                val isoSdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = utc }

                checkInDisplay = displaySdf.format(Date(startDate))
                checkOutDisplay = displaySdf.format(Date(endDate))
                checkInIso = isoSdf.format(Date(startDate))
                checkOutIso = isoSdf.format(Date(endDate))

                updateBookingSummaryUi()
                fetchCotacao()
            }
        }
        datePicker.show(supportFragmentManager, "MATERIAL_DATE_PICKER")
    }

    private fun showTribePicker() {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 40, 60, 20)
        }

        // Adultos
        val rowAdults = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 10, 0, 20)
        }
        val tvAdultsLabel = TextView(this).apply {
            text = "Adultos (>12 anos):"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            textSize = 15f
        }
        val tvAdultsVal = TextView(this).apply {
            text = "$adultsCount"
            textSize = 18f
            setPadding(20, 0, 20, 0)
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        val btnMinusAdult = Button(this).apply {
            text = "-"
            setOnClickListener {
                if (adultsCount > 1) {
                    adultsCount--
                    tvAdultsVal.text = "$adultsCount"
                }
            }
        }
        val btnPlusAdult = Button(this).apply {
            text = "+"
            setOnClickListener {
                adultsCount++
                tvAdultsVal.text = "$adultsCount"
            }
        }
        rowAdults.addView(tvAdultsLabel)
        rowAdults.addView(btnMinusAdult)
        rowAdults.addView(tvAdultsVal)
        rowAdults.addView(btnPlusAdult)
        container.addView(rowAdults)

        // Crianças
        val rowChildren = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 10, 0, 10)
        }
        val tvChildrenLabel = TextView(this).apply {
            text = "Crianças (caverninhas):"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            textSize = 15f
        }
        val tvChildrenVal = TextView(this).apply {
            text = "$childrenCount"
            textSize = 18f
            setPadding(20, 0, 20, 0)
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        val btnMinusChild = Button(this).apply {
            text = "-"
            setOnClickListener {
                if (childrenCount > 0) {
                    childrenCount--
                    tvChildrenVal.text = "$childrenCount"
                }
            }
        }
        val btnPlusChild = Button(this).apply {
            text = "+"
            setOnClickListener {
                childrenCount++
                tvChildrenVal.text = "$childrenCount"
            }
        }
        rowChildren.addView(tvChildrenLabel)
        rowChildren.addView(btnMinusChild)
        rowChildren.addView(tvChildrenVal)
        rowChildren.addView(btnPlusChild)
        container.addView(rowChildren)

        AlertDialog.Builder(this)
            .setTitle("Composição da Tribo")
            .setView(container)
            .setPositiveButton("Confirmar") { _, _ ->
                updateBookingSummaryUi()
                fetchCotacao()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun fetchCotacao() {
        if (checkInIso.isEmpty() || checkOutIso.isEmpty()) {
            binding.layoutOccupiedWarning.visibility = View.GONE
            binding.btnReserve.isEnabled = true
            binding.btnReserve.alpha = 1.0f
            return
        }

        lifecycleScope.launch {
            try {
                val api = ApiClient.getApi(this@CavernDetailActivity)
                val idades = List(childrenCount) { 6 }
                val response = api.getCotacao(
                    cavernaId = cavernId,
                    checkIn = checkInIso,
                    checkOut = checkOutIso,
                    adultos = adultsCount,
                    idadesCriancas = idades
                )
                if (response.isSuccessful && response.body() != null) {
                    val cotacao = response.body()!!
                    noitesEstadia = if (cotacao.noites > 0) cotacao.noites else 1
                    if (cotacao.valorDiariasCentavos > 0) {
                        totalEstadiaCentavos = cotacao.valorDiariasCentavos
                        val precoDiaria = totalEstadiaCentavos / noitesEstadia
                        binding.tvPrice.text = "R$ ${String.format(Locale.getDefault(), "%,d", precoDiaria / 100)}"
                    }
                    binding.layoutOccupiedWarning.visibility = View.GONE
                    binding.btnReserve.isEnabled = true
                    binding.btnReserve.alpha = 1.0f
                    updateBookingSummaryUi()
                } else {
                    // ex.: acima da capacidade, estadia acima de 30 noites, datas no passado
                    binding.layoutOccupiedWarning.visibility = View.VISIBLE
                    binding.btnReserve.isEnabled = false
                    binding.btnReserve.alpha = 0.5f
                    Toast.makeText(this@CavernDetailActivity, ApiErrors.mensagem(response), Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@CavernDetailActivity, ApiErrors.mensagem(e), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchDetail() {
        lifecycleScope.launch {
            try {
                val api = ApiClient.getApi(this@CavernDetailActivity)

                // Favoritos só existem para quem tem conta
                if (SessionManager(this@CavernDetailActivity).isLoggedIn()) try {
                    val favResponse = api.getCavernasFavoritas()
                    if (favResponse.isSuccessful && favResponse.body() != null) {
                        val favs = favResponse.body()!!
                        isFavorited = favs.any { it.slug == cavernSlug }
                        binding.btnFavorite.setImageResource(if (isFavorited) R.drawable.ic_heart else R.drawable.ic_heart_outline)
                    }
                } catch (_: Exception) {}

                val response = api.getCavernaDetail(cavernSlug)
                if (response.isSuccessful && response.body() != null) {
                    val detail = response.body()!!
                    cavernId = detail.caverna.id
                    binding.btnFavorite.setImageResource(if (isFavorited) R.drawable.ic_heart else R.drawable.ic_heart_outline)
                    bindDetail(detail)
                    fetchCotacao()
                } else {
                    Toast.makeText(this@CavernDetailActivity, "Erro ao carregar detalhes da caverna (${response.code()})", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@CavernDetailActivity, "Erro de conexão: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun bindDetail(detail: CavernDetailResponse) {
        val cavern = detail.caverna
        binding.tvCategory.text = cavern.categoria.uppercase(Locale.getDefault())
        binding.tvCavernName.text = cavern.nome
        binding.tvRating.text = String.format(Locale.getDefault(), "%.1f (%d relatos)", cavern.notaMedia, cavern.totalRelatos)
        binding.tvLocation.text = if (!cavern.ala.isNullOrEmpty()) "${cavern.ala} · Huga Buga Resort" else LanguageManager.get(this, "detail_location")
        binding.tvDescription.text = detail.descricao ?: cavern.resumo ?: ""
        binding.tvPrice.text = "R$ ${String.format(Locale.getDefault(), "%,d", cavern.precoBaseCentavos / 100)}"

        if (totalEstadiaCentavos == 0L) {
            totalEstadiaCentavos = cavern.precoBaseCentavos * noitesEstadia
        }

        val fotos = detail.fotos
        val fotoUrl = fotos.firstOrNull()?.url ?: cavern.fotoUrl
        carregarFoto(fotoUrl)
        binding.tvPhotoCount.text = if (fotos.isNotEmpty()) "1 / ${fotos.size}" else "1 / 1"

        // Comodidades / Destaques
        binding.gridComodidades.removeAllViews()
        val comodidadesList = if (detail.comodidades.isNotEmpty()) {
            detail.comodidades.map { it.nome }
        } else {
            cavern.comodidades
        }
        for (comodidade in comodidadesList) {
            val amenityView = layoutInflater.inflate(R.layout.item_amenity, binding.gridComodidades, false)
            val tvTitle = amenityView.findViewById<TextView>(R.id.tvAmenityTitle)
            val tvSubtitle = amenityView.findViewById<TextView>(R.id.tvAmenitySubtitle)
            tvTitle?.text = comodidade
            tvSubtitle?.text = getString(R.string.amenity_highlight)
            binding.gridComodidades.addView(amenityView)
        }

        // Regras / Detalhes da Tribo
        binding.tvRuleCheckin.text = "Check-in (${cavern.capacidade} hóspedes máx, ${cavern.camas} camas)"
        binding.tvRuleCheckout.text = "Check-out (Área: ${cavern.areaM2} m²)"
        binding.tvRulePets.text = if (cavern.aceitaPets) "🐾 Aceita animais de estimação" else "❌ Não aceita animais de estimação"

        updateBookingSummaryUi()
    }

    private fun carregarFoto(fotoUrl: String?) {
        if (!fotoUrl.isNullOrEmpty()) {
            binding.ivDetailPhoto.load(fotoUrl) {
                crossfade(true)
                listener(
                    onSuccess = { _, _ -> binding.ivDetailPlaceholder.visibility = View.GONE },
                    onError = { _, _ -> binding.ivDetailPlaceholder.visibility = View.VISIBLE }
                )
            }
        } else {
            binding.ivDetailPhoto.setImageDrawable(null)
            binding.ivDetailPlaceholder.visibility = View.VISIBLE
        }
    }

    private fun iniciarReserva() {
        if (checkInIso.isEmpty() || checkOutIso.isEmpty()) {
            Toast.makeText(this, "Por favor, selecione as datas da estadia antes de reservar!", Toast.LENGTH_SHORT).show()
            showDatePicker()
            return
        }
        val session = SessionManager(this)
        if (session.isLoggedIn()) {
            criarReserva(null)
            return
        }
        // Sem conta: a API exige o e-mail do responsável, que depois identifica o dono da reserva
        val campo = EditText(this).apply {
            hint = "seu@email.com"
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS or android.text.InputType.TYPE_CLASS_TEXT
            session.getResponsavelEmail()?.let { setText(it) }
        }
        AlertDialog.Builder(this)
            .setTitle("Reservar sem conta")
            .setMessage("Informe o e-mail do responsável. É com ele que você consulta e gerencia a reserva.")
            .setView(campo)
            .setPositiveButton("Continuar") { _, _ ->
                val email = campo.text.toString().trim()
                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    Toast.makeText(this, "E-mail inválido", Toast.LENGTH_SHORT).show()
                } else {
                    criarReserva(email)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun criarReserva(emailSemConta: String?) {
        binding.btnReserve.isEnabled = false
        lifecycleScope.launch {
            try {
                val api = ApiClient.getApi(this@CavernDetailActivity)
                // As idades reais são informadas no passo 1; aqui 6 anos já conta na capacidade sem somar adicional.
                val request = CriarReservaRequest(
                    cavernaId = cavernId,
                    checkIn = checkInIso,
                    checkOut = checkOutIso,
                    adultos = adultsCount,
                    idadesCriancas = List(childrenCount) { 6 },
                    email = emailSemConta
                )
                val response = api.criarReserva(request)
                val reserva = response.body()
                if (response.isSuccessful && reserva != null) {
                    if (emailSemConta != null) {
                        SessionManager(this@CavernDetailActivity).saveResponsavelEmail(emailSemConta)
                        SessionManager(this@CavernDetailActivity).addGuestBooking(reserva.codigo)
                    }
                    val intent = Intent(this@CavernDetailActivity, CheckoutStep1Activity::class.java).apply {
                        putExtra("RESERVA_CODIGO", reserva.codigo)
                        putExtra("CHECK_IN", checkInDisplay)
                        putExtra("CHECK_OUT", checkOutDisplay)
                        putExtra("CHECK_IN_ISO", reserva.checkIn)
                        putExtra("CHECK_OUT_ISO", reserva.checkOut)
                        putExtra("ADULTS", adultsCount)
                        putExtra("CHILDREN", childrenCount)
                        putExtra("TOTAL_CENTAVOS", reserva.total)
                        putExtra("NOITES", reserva.noites)
                        putExtra("CAVERN_NOME", reserva.cavernaNome)
                        putExtra("RESPONSAVEL_EMAIL", reserva.responsavelEmail)
                    }
                    startActivity(intent)
                } else {
                    // 409 SEM_DISPONIBILIDADE traz datas sugeridas; 400 traz o campo com problema
                    AlertDialog.Builder(this@CavernDetailActivity)
                        .setTitle("Não deu pra reservar")
                        .setMessage(ApiErrors.mensagem(response))
                        .setPositiveButton("OK", null)
                        .show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@CavernDetailActivity, ApiErrors.mensagem(e), Toast.LENGTH_LONG).show()
            } finally {
                binding.btnReserve.isEnabled = true
            }
        }
    }
}
