package com.example.hugabugahotel.ui.experiences

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.hugabugahotel.R
import com.example.hugabugahotel.data.api.ApiClient
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

    private fun openSelectBookingScreen(item: AtividadeItem) {
        val intent = Intent(this, SelectBookingForExperienceActivity::class.java).apply {
            putExtra("EXPERIENCE_ID", item.id)
            putExtra("EXPERIENCE_NOME", item.displayNome)
        }
        startActivity(intent)
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
            val list = try {
                val api = ApiClient.getApi(this@ExperiencesActivity)
                val response = api.getAtividades(queryParam)
                if (response.isSuccessful && response.body() != null) {
                    response.body()!!.itens
                } else {
                    getDefaultAtividades(categoria)
                }
            } catch (e: Exception) {
                getDefaultAtividades(categoria)
            }
            adapter.updateData(list)
        }
    }

    private fun getDefaultAtividades(categoria: String): List<AtividadeItem> {
        val all = listOf(
            AtividadeItem(id = "1", nome = "Escavação de ovo de dino", categoria = "CRIANCAS", descricao = "Procure fósseis na areia vulcânica", precoCentavos = 0L, duracaoMin = 60, idadeMin = 4, idadeMax = 12, cobranca = "POR_PESSOA", faixaEtaria = "4-12 anos"),
            AtividadeItem(id = "2", nome = "Pintura rupestre", categoria = "CRIANCAS", descricao = "Aprenda a desenhar nas paredes da caverna", precoCentavos = 0L, duracaoMin = 45, idadeMin = 3, idadeMax = 10, cobranca = "POR_PESSOA", faixaEtaria = "3-10 anos"),
            AtividadeItem(id = "3", nome = "Arco e flecha", categoria = "AVENTURA", descricao = "Aula de pontaria com monitor e equipamento", precoCentavos = 5000L, duracaoMin = 45, idadeMin = 8, cobranca = "POR_PESSOA", faixaEtaria = "8+ anos"),
            AtividadeItem(id = "4", nome = "Trilha dos mamutes", categoria = "FAMILIA", descricao = "Caminhada guiada pelo vale dos dinos", precoCentavos = 9000L, duracaoMin = 90, idadeMin = 0, cobranca = "POR_PESSOA", faixaEtaria = "Todas idades"),
            AtividadeItem(id = "5", nome = "Fogueira & histórias", categoria = "FAMILIA", descricao = "Contação de lendas sob as estrelas", precoCentavos = 0L, duracaoMin = 60, idadeMin = 0, cobranca = "POR_PESSOA", faixaEtaria = "Todas idades")
        )
        return if (categoria == "ALL") {
            all
        } else {
            all.filter { it.categoria.equals(categoria, ignoreCase = true) }
        }
    }
}
