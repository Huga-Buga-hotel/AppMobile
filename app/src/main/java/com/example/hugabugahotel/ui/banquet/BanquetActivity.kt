package com.example.hugabugahotel.ui.banquet

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.hugabugahotel.R
import com.example.hugabugahotel.data.api.ApiClient
import com.example.hugabugahotel.databinding.ActivityBanquetBinding
import com.example.hugabugahotel.ui.account.MyBookingsActivity
import com.example.hugabugahotel.ui.account.ProfileActivity
import com.example.hugabugahotel.ui.caverns.CavernsListActivity
import com.example.hugabugahotel.ui.experiences.ExperiencesActivity
import com.example.hugabugahotel.ui.home.HomeActivity
import com.example.hugabugahotel.util.LanguageManager
import kotlinx.coroutines.launch

class BanquetActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBanquetBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBanquetBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigation()
        fetchStatus()
    }

    override fun onResume() {
        super.onResume()
        LanguageManager.updateBottomNavLanguage(this, binding.root.findViewById(R.id.bottomNav))
    }

    private fun setupNavigation() {
        binding.root.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNav)?.let { nav ->
            nav.selectedItemId = R.id.nav_caverns
            nav.setOnItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.nav_home -> {
                        startActivity(Intent(this, HomeActivity::class.java))
                        true
                    }
                    R.id.nav_caverns -> {
                        startActivity(Intent(this, CavernsListActivity::class.java))
                        true
                    }
                    R.id.nav_reservations -> {
                        startActivity(Intent(this, MyBookingsActivity::class.java))
                        true
                    }
                    R.id.nav_profile -> {
                        startActivity(Intent(this, ProfileActivity::class.java))
                        true
                    }
                    else -> false
                }
            }
        }
    }

    private fun fetchStatus() {
        lifecycleScope.launch {
            try {
                val api = ApiClient.getApi(this@BanquetActivity)
                val response = api.getRestauranteStatus()
                if (response.isSuccessful) {
                    val status = response.body()
                    // Restaurante status carregado
                }
            } catch (e: Exception) {
                // Modo offline
            }
        }
    }
}
