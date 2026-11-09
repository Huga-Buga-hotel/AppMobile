package com.example.hugabugahotel.ui.account

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.hugabugahotel.data.api.ApiClient
import com.example.hugabugahotel.data.model.CavernSummary
import com.example.hugabugahotel.databinding.ActivityFavoriteCavernsBinding
import com.example.hugabugahotel.util.LanguageManager
import kotlinx.coroutines.launch

class FavoriteCavernsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFavoriteCavernsBinding
    private lateinit var adapter: FavoriteCavernsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFavoriteCavernsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.appBar.root.setOnClickListener {
            finish()
        }

        setupRecyclerView()
    }

    override fun onResume() {
        super.onResume()
        binding.tvTitleFavoriteCaverns.text = LanguageManager.get(this, "favorite_caverns_title")
        binding.tvEmptyState.text = LanguageManager.get(this, "empty_favorites")
        fetchFavoriteCaverns()
    }

    private fun setupRecyclerView() {
        adapter = FavoriteCavernsAdapter(
            initialItems = emptyList(),
            onRemoveFavorite = { cavern ->
                removerDosFavoritos(cavern)
            }
        )
        binding.rvFavoriteCaverns.layoutManager = LinearLayoutManager(this)
        binding.rvFavoriteCaverns.adapter = adapter
    }

    private fun removerDosFavoritos(cavern: CavernSummary) {
        lifecycleScope.launch {
            try {
                val api = ApiClient.getApi(this@FavoriteCavernsActivity)
                val response = api.removerFavorito(cavern.id)
                if (response.isSuccessful) {
                    Toast.makeText(
                        this@FavoriteCavernsActivity,
                        LanguageManager.get(this@FavoriteCavernsActivity, "fav_removed"),
                        Toast.LENGTH_SHORT
                    ).show()
                    fetchFavoriteCaverns()
                } else {
                    Toast.makeText(
                        this@FavoriteCavernsActivity,
                        "Erro ao desfavoritar: ${response.code()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@FavoriteCavernsActivity,
                    "Erro: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun fetchFavoriteCaverns() {
        lifecycleScope.launch {
            try {
                val api = ApiClient.getApi(this@FavoriteCavernsActivity)
                val response = api.getCavernasFavoritas()
                if (response.isSuccessful && response.body() != null) {
                    val list = response.body()!!
                    adapter.updateData(list)
                    if (list.isEmpty()) {
                        binding.tvEmptyState.visibility = View.VISIBLE
                        binding.rvFavoriteCaverns.visibility = View.GONE
                    } else {
                        binding.tvEmptyState.visibility = View.GONE
                        binding.rvFavoriteCaverns.visibility = View.VISIBLE
                    }
                } else {
                    binding.tvEmptyState.visibility = View.VISIBLE
                    binding.rvFavoriteCaverns.visibility = View.GONE
                    adapter.updateData(emptyList())
                }
            } catch (e: Exception) {
                binding.tvEmptyState.visibility = View.VISIBLE
                binding.rvFavoriteCaverns.visibility = View.GONE
                adapter.updateData(emptyList())
            }
        }
    }
}
