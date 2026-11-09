package com.example.hugabugahotel.ui.account

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.hugabugahotel.R
import com.example.hugabugahotel.data.api.ApiClient
import com.example.hugabugahotel.data.api.SessionManager
import com.example.hugabugahotel.data.model.ReservaResponse
import com.example.hugabugahotel.databinding.ActivityMyBookingsBinding
import com.example.hugabugahotel.util.LanguageManager
import com.example.hugabugahotel.util.setupMainNavigation
import kotlinx.coroutines.launch
import java.util.Locale

class MyBookingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMyBookingsBinding
    private lateinit var adapter: BookingsAdapter
    private var currentTab = "proximas"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyBookingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigation()
        setupRecyclerView()
        setupTabs()
        fetchReservas(currentTab)
    }

    override fun onResume() {
        super.onResume()
        binding.bottomNav.selectedItemId = R.id.nav_reservations
        applyLanguage()
        fetchReservas(currentTab)
    }

    private fun applyLanguage() {
        binding.tvTitleBookings.text = LanguageManager.get(this, "bookings_title")
        binding.chipUpcoming.text = LanguageManager.get(this, "tab_upcoming")
        binding.chipPast.text = LanguageManager.get(this, "tab_past")
        binding.chipCanceled.text = LanguageManager.get(this, "tab_canceled")
        binding.tvEmptyState.text = LanguageManager.get(this, "empty_bookings")

        LanguageManager.updateBottomNavLanguage(this, binding.bottomNav)
    }

    private fun setupRecyclerView() {
        adapter = BookingsAdapter(
            items = emptyList(),
            currentTab = currentTab,
            onCancelClick = { estadia ->
                cancelarReserva(estadia)
            }
        )
        binding.rvBookings.layoutManager = LinearLayoutManager(this)
        binding.rvBookings.adapter = adapter
    }

    private fun setupTabs() {
        binding.chipUpcoming.setOnClickListener {
            currentTab = "proximas"
            updateTabStyles()
            setupRecyclerView()
            fetchReservas(currentTab)
        }
        binding.chipPast.setOnClickListener {
            currentTab = "passadas"
            updateTabStyles()
            setupRecyclerView()
            fetchReservas(currentTab)
        }
        binding.chipCanceled.setOnClickListener {
            currentTab = "canceladas"
            updateTabStyles()
            setupRecyclerView()
            fetchReservas(currentTab)
        }
    }

    private fun updateTabStyles() {
        binding.chipUpcoming.setBackgroundResource(if (currentTab == "proximas") R.drawable.bg_chip_selected else R.drawable.bg_chip)
        binding.chipUpcoming.setTextColor(resources.getColor(if (currentTab == "proximas") android.R.color.white else R.color.hb_ink, theme))

        binding.chipPast.setBackgroundResource(if (currentTab == "passadas") R.drawable.bg_chip_selected else R.drawable.bg_chip)
        binding.chipPast.setTextColor(resources.getColor(if (currentTab == "passadas") android.R.color.white else R.color.hb_ink, theme))

        binding.chipCanceled.setBackgroundResource(if (currentTab == "canceladas") R.drawable.bg_chip_selected else R.drawable.bg_chip)
        binding.chipCanceled.setTextColor(resources.getColor(if (currentTab == "canceladas") android.R.color.white else R.color.hb_ink, theme))
    }

    private fun setupNavigation() {
        binding.bottomNav.setupMainNavigation(this, R.id.nav_reservations)
    }

    private fun cancelarReserva(reserva: ReservaResponse) {
        AlertDialog.Builder(this)
            .setTitle("Cancelar Reserva")
            .setMessage("Tem certeza de que deseja cancelar a reserva ${reserva.codigo} (${reserva.cavernaNome})?")
            .setPositiveButton("Sim, cancelar") { _, _ ->
                lifecycleScope.launch {
                    try {
                        val api = ApiClient.getApi(this@MyBookingsActivity)
                        val response = api.cancelarReserva(reserva.codigo)
                        if (response.isSuccessful) {
                            SessionManager(this@MyBookingsActivity).removeCompletedBooking(reserva.codigo)
                            Toast.makeText(this@MyBookingsActivity, "Reserva cancelada com sucesso! 🪨", Toast.LENGTH_SHORT).show()
                            fetchReservas(currentTab)
                        } else {
                            Toast.makeText(this@MyBookingsActivity, "Erro ao cancelar reserva: ${response.code()}", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this@MyBookingsActivity, "Erro: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Não", null)
            .show()
    }

    private fun fetchReservas(aba: String) {
        lifecycleScope.launch {
            val apiList = try {
                val api = ApiClient.getApi(this@MyBookingsActivity)
                val response = api.getMinhasReservas(aba)
                if (response.isSuccessful && response.body() != null) {
                    response.body()!!.estadias
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                emptyList()
            }

            val filteredList = when (aba) {
                "proximas" -> apiList.filter {
                    val st = it.status.uppercase(Locale.ROOT)
                    (st.contains("PENDENTE") || st.contains("AGUARDANDO") || st.contains("CONFIRMADA")) && !st.contains("CANCELADA")
                }
                "passadas" -> apiList.filter {
                    val st = it.status.uppercase(Locale.ROOT)
                    (st.contains("CONCLUIDA") || st.contains("EXPIRADA")) && !st.contains("CANCELADA")
                }
                "canceladas" -> apiList.filter {
                    val st = it.status.uppercase(Locale.ROOT)
                    st.contains("CANCELADA")
                }
                else -> apiList
            }

            adapter.updateData(filteredList)
            if (filteredList.isEmpty()) {
                binding.tvEmptyState.visibility = View.VISIBLE
                binding.rvBookings.visibility = View.GONE
            } else {
                binding.tvEmptyState.visibility = View.GONE
                binding.rvBookings.visibility = View.VISIBLE
            }
        }
    }
}
