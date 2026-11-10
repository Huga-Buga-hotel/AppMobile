package com.example.hugabugahotel.ui.experiences

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.hugabugahotel.R
import com.example.hugabugahotel.data.api.ApiClient
import com.example.hugabugahotel.data.api.ApiErrors
import com.example.hugabugahotel.data.model.AtividadeItem
import com.example.hugabugahotel.databinding.ActivityExperiencesBinding
import com.example.hugabugahotel.util.LanguageManager
import com.example.hugabugahotel.util.setupMainNavigation
import kotlinx.coroutines.launch

class ExperiencesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityExperiencesBinding
    private lateinit var adapter: ExperiencesAdapter
    private var currentCategory = "ALL"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExperiencesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigation()
        setupRecyclerView()
        setupFilters()

        fetchAtividades(currentCategory)
    }

    override fun onResume() {
        super.onResume()
        binding.bottomNav.selectedItemId = R.id.nav_experiences
        LanguageManager.updateBottomNavLanguage(this, binding.bottomNav)
    }

    private fun setupNavigation() {
        binding.bottomNav.setupMainNavigation(this, R.id.nav_experiences)
    }

    private fun setupRecyclerView() {
        adapter = ExperiencesAdapter(emptyList()) { item ->
            openSelectBookingScreen(item)
        }
        binding.experiencesList.layoutManager = LinearLayoutManager(this)
        binding.experiencesList.adapter = adapter
    }

    /** Vindo do voucher/gerenciar, a reserva já é conhecida; senão o hóspede escolhe uma reserva confirmada. */
    private fun openSelectBookingScreen(item: AtividadeItem) {
        val reserva = intent.getStringExtra("RESERVA_CODIGO")
        val destino = if (reserva != null) ExperienceScheduleActivity::class.java else SelectBookingForExperienceActivity::class.java
        startActivity(Intent(this, destino).apply {
            putExtra("EXPERIENCE_ID", item.id)
            putExtra("EXPERIENCE_NOME", item.displayNome)
            reserva?.let { putExtra("RESERVA_CODIGO", it) }
        })
    }

    private fun setupFilters() {
        binding.chipAllAtividades.setOnClickListener {
            currentCategory = "ALL"
            updateFilterStyles()
            fetchAtividades(currentCategory)
        }

        binding.chipKidsAtividades.setOnClickListener {
            currentCategory = "CRIANCAS"
            updateFilterStyles()
            fetchAtividades(currentCategory)
        }

        binding.chipFamilyAtividades.setOnClickListener {
            currentCategory = "FAMILIA"
            updateFilterStyles()
            fetchAtividades(currentCategory)
        }

        binding.chipAdventureAtividades.setOnClickListener {
            currentCategory = "AVENTURA"
            updateFilterStyles()
            fetchAtividades(currentCategory)
        }
    }

    private fun updateFilterStyles() {
        val whiteColor = resources.getColor(android.R.color.white, theme)
        val inkColor = resources.getColor(R.color.hb_ink, theme)

        binding.chipAllAtividades.setBackgroundResource(if (currentCategory == "ALL") R.drawable.bg_chip_selected else R.drawable.bg_chip)
        binding.chipAllAtividades.setTextColor(if (currentCategory == "ALL") whiteColor else inkColor)

        binding.chipKidsAtividades.setBackgroundResource(if (currentCategory == "CRIANCAS") R.drawable.bg_chip_selected else R.drawable.bg_chip)
        binding.chipKidsAtividades.setTextColor(if (currentCategory == "CRIANCAS") whiteColor else inkColor)

        binding.chipFamilyAtividades.setBackgroundResource(if (currentCategory == "FAMILIA") R.drawable.bg_chip_selected else R.drawable.bg_chip)
        binding.chipFamilyAtividades.setTextColor(if (currentCategory == "FAMILIA") whiteColor else inkColor)

        binding.chipAdventureAtividades.setBackgroundResource(if (currentCategory == "AVENTURA") R.drawable.bg_chip_selected else R.drawable.bg_chip)
        binding.chipAdventureAtividades.setTextColor(if (currentCategory == "AVENTURA") whiteColor else inkColor)
    }

    private fun fetchAtividades(categoria: String) {
        lifecycleScope.launch {
            val queryParam = if (categoria == "ALL") null else categoria
            try {
                val response = ApiClient.getApi(this@ExperiencesActivity).getAtividades(queryParam)
                if (response.isSuccessful) {
                    adapter.updateData(response.body()?.itens.orEmpty())
                } else {
                    Toast.makeText(this@ExperiencesActivity, ApiErrors.mensagem(response), Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ExperiencesActivity, ApiErrors.mensagem(e), Toast.LENGTH_LONG).show()
            }
        }
    }
}
