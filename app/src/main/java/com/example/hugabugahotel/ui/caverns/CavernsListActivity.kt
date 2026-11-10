package com.example.hugabugahotel.ui.caverns

import android.app.AlertDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.hugabugahotel.R
import com.example.hugabugahotel.data.api.ApiClient
import com.example.hugabugahotel.data.api.ApiErrors
import com.example.hugabugahotel.util.Formatos
import com.example.hugabugahotel.data.api.SessionManager
import com.example.hugabugahotel.data.model.CavernSummary
import com.example.hugabugahotel.databinding.ActivityCavernsListBinding
import com.example.hugabugahotel.util.LanguageManager
import com.example.hugabugahotel.util.setupMainNavigation
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CavernsListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCavernsListBinding
    private lateinit var adapter: CavernsAdapter
    private var allCaverns: List<CavernSummary> = emptyList()
    private var currentFilter = "ALL"
    private var searchQueryName = ""

    var checkIn: String = ""
    var checkOut: String = ""
    /** yyyy-MM-dd, enviados para GET /cavernas (só cavernas com toca livre no período). */
    var checkInIso: String = ""
    var checkOutIso: String = ""
    var adults: Int = 2
    var children: Int = 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCavernsListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkIn = intent.getStringExtra("CHECK_IN") ?: ""
        checkOut = intent.getStringExtra("CHECK_OUT") ?: ""
        checkInIso = intent.getStringExtra("CHECK_IN_ISO") ?: ""
        checkOutIso = intent.getStringExtra("CHECK_OUT_ISO") ?: ""

        val sessionManager = SessionManager(this)
        if (sessionManager.isLoggedIn() && sessionManager.getDefaultAdults() >= 1) {
            adults = intent.getIntExtra("ADULTS", sessionManager.getDefaultAdults())
            children = intent.getIntExtra("CHILDREN", sessionManager.getDefaultChildren())
        } else {
            adults = intent.getIntExtra("ADULTS", 2)
            children = intent.getIntExtra("CHILDREN", 2)
        }

        updateSearchSummaryUi()

        binding.etSearchQueryName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchQueryName = s?.toString()?.trim() ?: ""
                filterAndPopulate()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.layoutChangeSearch.setOnClickListener {
            showEditSearchOptions()
        }

        setupNavigation()
        setupFilters()
        setupRecyclerView()
        fetchCaverns()
        fetchTribeMembersFromProfile()
    }

    /** Vindo da Home com FLAG_ACTIVITY_REORDER_TO_FRONT: a tela já existe, então relê a busca. */
    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        checkIn = intent.getStringExtra("CHECK_IN") ?: checkIn
        checkOut = intent.getStringExtra("CHECK_OUT") ?: checkOut
        checkInIso = intent.getStringExtra("CHECK_IN_ISO") ?: checkInIso
        checkOutIso = intent.getStringExtra("CHECK_OUT_ISO") ?: checkOutIso
        adults = intent.getIntExtra("ADULTS", adults)
        children = intent.getIntExtra("CHILDREN", children)
        updateSearchSummaryUi()
        fetchCaverns()
    }

    override fun onResume() {
        super.onResume()
        binding.bottomNav.selectedItemId = R.id.nav_caverns
        applyLanguage()
        fetchTribeMembersFromProfile()
        LanguageManager.updateBottomNavLanguage(this, binding.bottomNav)
    }

    private fun fetchTribeMembersFromProfile() {
        val sessionManager = SessionManager(this)
        if (sessionManager.isLoggedIn()) {
            lifecycleScope.launch {
                try {
                    val api = ApiClient.getApi(this@CavernsListActivity)
                    val response = api.getMembrosTribo()
                    if (response.isSuccessful && response.body() != null) {
                        val membros = response.body()!!
                        val ads = 1 + membros.count { it.tipo.contains("ADULTO", ignoreCase = true) }
                        val chis = membros.count { it.tipo.contains("CRIANCA", ignoreCase = true) }
                        sessionManager.saveDefaultTribeCount(ads, chis)
                        if (!intent.hasExtra("ADULTS")) {
                            adults = ads
                            children = chis
                            updateSearchSummaryUi()
                        }
                    }
                } catch (_: Exception) {}
            }
        }
    }

    private fun updateSearchSummaryUi() {
        val totalPessoas = adults + children
        val datesStr = if (checkIn.isNotEmpty() && checkOut.isNotEmpty()) "$checkIn – $checkOut" else "Selecione as datas"
        binding.tvSearchSummary.text = "$datesStr · $totalPessoas na tribo ($adults ad., $children cri.)"
    }

    private fun showEditSearchOptions() {
        val options = arrayOf("Alterar Período da Estadia", "Alterar Composição da Tribo")
        AlertDialog.Builder(this)
            .setTitle("Modificar Busca")
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
            .setTitleText("Selecione o Período da Estadia")
            .setCalendarConstraints(constraintsBuilder.build())
            .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            val startDate = selection.first
            val endDate = selection.second
            if (startDate != null && endDate != null) {
                // O date picker devolve meia-noite em UTC: converter em UTC evita mostrar o dia anterior
                val inicio = java.time.Instant.ofEpochMilli(startDate).atZone(java.time.ZoneOffset.UTC).toLocalDate()
                val fim = java.time.Instant.ofEpochMilli(endDate).atZone(java.time.ZoneOffset.UTC).toLocalDate()
                checkInIso = inicio.toString()
                checkOutIso = fim.toString()
                checkIn = Formatos.dataCurta(checkInIso)
                checkOut = Formatos.dataCurta(checkOutIso)
                updateSearchSummaryUi()
                fetchCaverns()
            }
        }
        datePicker.show(supportFragmentManager, "MATERIAL_DATE_PICKER")
    }

    private fun showTribePicker() {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 40, 60, 20)
        }

        // Adults
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
            text = "$adults"
            textSize = 18f
            setPadding(20, 0, 20, 0)
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        val btnMinusAdult = Button(this).apply {
            text = "-"
            setOnClickListener {
                if (adults > 1) {
                    adults--
                    tvAdultsVal.text = "$adults"
                }
            }
        }
        val btnPlusAdult = Button(this).apply {
            text = "+"
            setOnClickListener {
                adults++
                tvAdultsVal.text = "$adults"
            }
        }
        rowAdults.addView(tvAdultsLabel)
        rowAdults.addView(btnMinusAdult)
        rowAdults.addView(tvAdultsVal)
        rowAdults.addView(btnPlusAdult)
        container.addView(rowAdults)

        // Children
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
            text = "$children"
            textSize = 18f
            setPadding(20, 0, 20, 0)
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        val btnMinusChild = Button(this).apply {
            text = "-"
            setOnClickListener {
                if (children > 0) {
                    children--
                    tvChildrenVal.text = "$children"
                }
            }
        }
        val btnPlusChild = Button(this).apply {
            text = "+"
            setOnClickListener {
                children++
                tvChildrenVal.text = "$children"
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
                updateSearchSummaryUi()
                fetchCaverns()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun applyLanguage() {
        binding.tvCavernsListTitle.text = LanguageManager.get(this, "caverns_list_title")
        binding.tvCavernsListSubtitle.text = LanguageManager.get(this, "caverns_list_subtitle")
        binding.chipAll.text = LanguageManager.get(this, "chip_all")
        binding.chipToca.text = LanguageManager.get(this, "chip_toca")
        binding.chipFogueira.text = LanguageManager.get(this, "chip_fogueira")
        binding.chipPrice.text = LanguageManager.get(this, "chip_price")
    }

    private fun setupRecyclerView() {
        adapter = CavernsAdapter(
            initialItems = emptyList(),
            onFavoriteClicked = { cavern, isFav ->
                handleFavoriteToggle(cavern, isFav)
            }
        )
        binding.rvCaverns.layoutManager = LinearLayoutManager(this)
        binding.rvCaverns.adapter = adapter
    }

    private fun handleFavoriteToggle(cavern: CavernSummary, isFavorite: Boolean) {
        if (!SessionManager(this).isLoggedIn()) {
            Toast.makeText(this, "Entre na sua conta para favoritar cavernas 🪨", Toast.LENGTH_SHORT).show()
            return
        }
        lifecycleScope.launch {
            try {
                val api = ApiClient.getApi(this@CavernsListActivity)
                val response = if (isFavorite) api.adicionarFavorito(cavern.id) else api.removerFavorito(cavern.id)
                val msg = if (!response.isSuccessful) ApiErrors.mensagem(response)
                    else LanguageManager.get(this@CavernsListActivity, if (isFavorite) "fav_added" else "fav_removed")
                Toast.makeText(this@CavernsListActivity, msg, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@CavernsListActivity, ApiErrors.mensagem(e), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupNavigation() {
        binding.bottomNav.setupMainNavigation(this, R.id.nav_caverns)
    }

    private fun setupFilters() {
        binding.chipAll.setOnClickListener {
            currentFilter = "ALL"
            updateChipStyles()
            filterAndPopulate()
        }
        binding.chipToca.setOnClickListener {
            currentFilter = "TOCA"
            updateChipStyles()
            filterAndPopulate()
        }
        binding.chipFogueira.setOnClickListener {
            currentFilter = "FOGUEIRA"
            updateChipStyles()
            filterAndPopulate()
        }
        binding.chipPrice.setOnClickListener {
            currentFilter = "PRICE"
            updateChipStyles()
            filterAndPopulate()
        }
    }

    private fun updateChipStyles() {
        binding.chipAll.setBackgroundResource(if (currentFilter == "ALL") R.drawable.bg_chip_selected else R.drawable.bg_chip)
        binding.chipAll.setTextColor(resources.getColor(if (currentFilter == "ALL") android.R.color.white else R.color.hb_ink, theme))

        binding.chipToca.setBackgroundResource(if (currentFilter == "TOCA") R.drawable.bg_chip_selected else R.drawable.bg_chip)
        binding.chipToca.setTextColor(resources.getColor(if (currentFilter == "TOCA") android.R.color.white else R.color.hb_ink, theme))

        binding.chipFogueira.setBackgroundResource(if (currentFilter == "FOGUEIRA") R.drawable.bg_chip_selected else R.drawable.bg_chip)
        binding.chipFogueira.setTextColor(resources.getColor(if (currentFilter == "FOGUEIRA") android.R.color.white else R.color.hb_ink, theme))

        binding.chipPrice.setBackgroundResource(if (currentFilter == "PRICE") R.drawable.bg_chip_selected else R.drawable.bg_chip)
        binding.chipPrice.setTextColor(resources.getColor(if (currentFilter == "PRICE") android.R.color.white else R.color.hb_ink, theme))
    }

    private fun fetchCaverns() {
        lifecycleScope.launch {
            val api = ApiClient.getApi(this@CavernsListActivity)
            if (SessionManager(this@CavernsListActivity).isLoggedIn()) try {
                val favResponse = api.getCavernasFavoritas()
                if (favResponse.isSuccessful && favResponse.body() != null) {
                    val ids = favResponse.body()!!.map { it.id }.toSet()
                    adapter.setFavoriteIds(ids)
                }
            } catch (_: Exception) {}

            allCaverns = try {
                val comDatas = checkInIso.isNotEmpty() && checkOutIso.isNotEmpty()
                val response = api.getCavernas(
                    checkIn = checkInIso.takeIf { comDatas }, checkOut = checkOutIso.takeIf { comDatas },
                    adultos = adults, idadesCriancas = List(children) { 6 }.ifEmpty { null },
                    page = 0, size = 50)
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    if (body.itens.isEmpty() && body.sugestoes.isNotEmpty()) {
                        Toast.makeText(this@CavernsListActivity, "Tudo ocupado nessas datas. Que tal: " +
                            body.sugestoes.joinToString(" ou ") { "${Formatos.dataCurta(it.checkIn)}–${Formatos.dataCurta(it.checkOut)}" },
                            Toast.LENGTH_LONG).show()
                    }
                    body.itens
                } else {
                    Toast.makeText(this@CavernsListActivity, ApiErrors.mensagem(response), Toast.LENGTH_LONG).show()
                    emptyList()
                }
            } catch (e: Exception) {
                Toast.makeText(this@CavernsListActivity, ApiErrors.mensagem(e), Toast.LENGTH_LONG).show()
                emptyList()
            }
            filterAndPopulate()
        }
    }

    private fun filterAndPopulate() {
        var list = when (currentFilter) {
            "TOCA" -> allCaverns.filter { it.slug.contains("toca", ignoreCase = true) || it.categoria == "FAMILIA" }
            "FOGUEIRA" -> allCaverns.filter { it.comodidades.any { c -> c.contains("fogueira", ignoreCase = true) } }
            "PRICE" -> allCaverns.sortedBy { it.totalEstadiaCentavos ?: it.precoBaseCentavos }
            else -> allCaverns
        }

        if (searchQueryName.isNotEmpty()) {
            list = list.filter { it.nome.contains(searchQueryName, ignoreCase = true) || it.categoria.contains(searchQueryName, ignoreCase = true) }
        }

        populateCaverns(list)
    }

    private fun populateCaverns(list: List<CavernSummary>) {
        adapter.updateData(list)
    }
}
