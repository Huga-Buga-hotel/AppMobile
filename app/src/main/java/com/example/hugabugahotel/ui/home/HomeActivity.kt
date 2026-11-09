package com.example.hugabugahotel.ui.home

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
import com.example.hugabugahotel.R
import com.example.hugabugahotel.data.api.ApiClient
import com.example.hugabugahotel.data.api.SessionManager
import com.example.hugabugahotel.databinding.ActivityHomeBinding
import com.example.hugabugahotel.ui.caverns.CavernsListActivity
import com.example.hugabugahotel.util.LanguageManager
import com.example.hugabugahotel.util.setupMainNavigation
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private var selectedCheckIn = ""
    private var selectedCheckOut = ""
    private var adultCount = 2
    private var childCount = 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigation()
        setupSearchCard()
        fetchCaverns()
        fetchTribeMembersFromProfile()
    }

    override fun onResume() {
        super.onResume()
        binding.bottomNav.selectedItemId = R.id.nav_home
        applyLanguage()
        fetchTribeMembersFromProfile()
        LanguageManager.updateBottomNavLanguage(this, binding.bottomNav)
    }

    private fun applyLanguage() {
        binding.tvHomeEyebrow.text = LanguageManager.get(this, "home_eyebrow")
        binding.tvHomeTitle1.text = LanguageManager.get(this, "home_title_1")
        binding.tvHomeTitle2.text = LanguageManager.get(this, "home_title_2")
        binding.tvHomeDesc.text = LanguageManager.get(this, "home_desc")

        binding.root.findViewById<TextView>(R.id.tvPhotoAlt)?.text = LanguageManager.get(this, "home_photo_alt")
        binding.root.findViewById<TextView>(R.id.tvRatingText)?.text = LanguageManager.get(this, "home_rating")
        binding.root.findViewById<TextView>(R.id.tvBadgeText)?.text = LanguageManager.get(this, "home_badge")
        binding.root.findViewById<TextView>(R.id.tvMarqueeBanner)?.text = LanguageManager.get(this, "marquee_text")

        binding.root.findViewById<TextView>(R.id.tvDatesLabel)?.text = LanguageManager.get(this, "search_dates_label")
        binding.root.findViewById<TextView>(R.id.tvTribeLabel)?.text = LanguageManager.get(this, "search_tribe_label")
        binding.root.findViewById<TextView>(R.id.btnSearch)?.text = LanguageManager.get(this, "search_cta")

        if (selectedCheckIn.isEmpty() || selectedCheckOut.isEmpty()) {
            binding.root.findViewById<TextView>(R.id.tvDatesValue)?.text = "Selecione as datas"
        } else {
            binding.root.findViewById<TextView>(R.id.tvDatesValue)?.text = "$selectedCheckIn – $selectedCheckOut"
        }

        updateTribeSummaryUi()
    }

    private fun updateTribeSummaryUi() {
        val total = adultCount + childCount
        val lang = SessionManager(this).getLanguage()
        val tribeStr = when (lang) {
            "cave" -> "$total na toca ($adultCount caç., $childCount caverninhas)"
            "en" -> "$total in tribe ($adultCount ad., $childCount ch.)"
            else -> "$total na tribo ($adultCount ad., $childCount cri.)"
        }
        binding.root.findViewById<TextView>(R.id.tvTribeValue)?.text = tribeStr
    }

    private fun fetchTribeMembersFromProfile() {
        val sessionManager = SessionManager(this)
        if (sessionManager.isLoggedIn()) {
            val cachedAdults = sessionManager.getDefaultAdults()
            val cachedChildren = sessionManager.getDefaultChildren()
            if (cachedAdults >= 1 && cachedChildren >= 0) {
                adultCount = cachedAdults
                childCount = cachedChildren
                updateTribeSummaryUi()
            }

            lifecycleScope.launch {
                try {
                    val api = ApiClient.getApi(this@HomeActivity)
                    val response = api.getMembrosTribo()
                    if (response.isSuccessful && response.body() != null) {
                        val membros = response.body()!!
                        val adults = 1 + membros.count { it.tipo.contains("ADULTO", ignoreCase = true) }
                        val children = membros.count { it.tipo.contains("CRIANCA", ignoreCase = true) }
                        adultCount = adults
                        childCount = children
                        sessionManager.saveDefaultTribeCount(adults, children)
                        updateTribeSummaryUi()
                    }
                } catch (_: Exception) {}
            }
        }
    }

    private fun setupNavigation() {
        binding.bottomNav.setupMainNavigation(this, R.id.nav_home)
    }

    private fun setupSearchCard() {
        val layoutDates = binding.root.findViewById<View>(R.id.layoutDates)
        val layoutTribe = binding.root.findViewById<View>(R.id.layoutTribe)
        val btnSearch = binding.root.findViewById<View>(R.id.btnSearch)

        layoutDates?.setOnClickListener {
            showDatePicker()
        }

        layoutTribe?.setOnClickListener {
            showTribePicker()
        }

        btnSearch?.setOnClickListener {
            navigateToCaverns()
        }
    }

    private fun navigateToCaverns() {
        if (selectedCheckIn.isEmpty() || selectedCheckOut.isEmpty()) {
            Toast.makeText(this, "Por favor, selecione o período da estadia antes de procurar cavernas! 🪨", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(this, CavernsListActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            if (selectedCheckIn.isNotEmpty()) putExtra("CHECK_IN", selectedCheckIn)
            if (selectedCheckOut.isNotEmpty()) putExtra("CHECK_OUT", selectedCheckOut)
            putExtra("ADULTS", adultCount)
            putExtra("CHILDREN", childCount)
        }
        startActivity(intent)
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
                val sdf = SimpleDateFormat("dd MMM", Locale.forLanguageTag("pt-BR"))
                selectedCheckIn = sdf.format(Date(startDate))
                selectedCheckOut = sdf.format(Date(endDate))
                val dateStr = "$selectedCheckIn – $selectedCheckOut"
                binding.root.findViewById<TextView>(R.id.tvDatesValue)?.text = dateStr
                Toast.makeText(this, "Período: $dateStr", Toast.LENGTH_SHORT).show()
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
            text = "$adultCount"
            textSize = 18f
            setPadding(20, 0, 20, 0)
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        val btnMinusAdult = Button(this).apply {
            text = "-"
            setOnClickListener {
                if (adultCount > 1) {
                    adultCount--
                    tvAdultsVal.text = "$adultCount"
                }
            }
        }
        val btnPlusAdult = Button(this).apply {
            text = "+"
            setOnClickListener {
                adultCount++
                tvAdultsVal.text = "$adultCount"
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
            text = "$childCount"
            textSize = 18f
            setPadding(20, 0, 20, 0)
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        val btnMinusChild = Button(this).apply {
            text = "-"
            setOnClickListener {
                if (childCount > 0) {
                    childCount--
                    tvChildrenVal.text = "$childCount"
                }
            }
        }
        val btnPlusChild = Button(this).apply {
            text = "+"
            setOnClickListener {
                childCount++
                tvChildrenVal.text = "$childCount"
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
                updateTribeSummaryUi()
                Toast.makeText(this, "Tribo atualizada!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun fetchCaverns() {
        lifecycleScope.launch {
            try {
                val api = ApiClient.getApi(this@HomeActivity)
                val response = api.getCavernas(page = 0, size = 5)
                if (response.isSuccessful) {
                    val body = response.body()
                    val total = body?.total ?: 0
                }
            } catch (e: Exception) {
                // Modo offline / fallback
            }
        }
    }
}
