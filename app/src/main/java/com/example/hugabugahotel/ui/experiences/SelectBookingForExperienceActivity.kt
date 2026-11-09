package com.example.hugabugahotel.ui.experiences

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.hugabugahotel.data.api.ApiClient
import com.example.hugabugahotel.data.model.ReservaResponse
import com.example.hugabugahotel.databinding.ActivitySelectBookingBinding
import com.example.hugabugahotel.ui.account.BookingsAdapter
import kotlinx.coroutines.launch
import java.util.Locale

class SelectBookingForExperienceActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySelectBookingBinding
    private lateinit var adapter: BookingsAdapter
    private var experienceId: String = ""
    private var experienceNome: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySelectBookingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        experienceId = intent.getStringExtra("EXPERIENCE_ID") ?: "1"
        experienceNome = intent.getStringExtra("EXPERIENCE_NOME") ?: "Experiência"

        binding.tvSelectBookingTitle.text = "Escolha sua Reserva para $experienceNome"

        setupRecyclerView()
        fetchBookings()
    }

    private fun setupRecyclerView() {
        adapter = BookingsAdapter(
            items = emptyList(),
            currentTab = "proximas",
            isSelectionMode = true,
            selectionButtonText = "Reservar",
            onVoucherClick = { reserva ->
                openScheduleForBooking(reserva)
            },
            onCancelClick = null
        )
        binding.rvBookingsForExp.layoutManager = LinearLayoutManager(this)
        binding.rvBookingsForExp.adapter = adapter
    }

    private fun openScheduleForBooking(reserva: ReservaResponse) {
        val intent = Intent(this, ExperienceScheduleActivity::class.java).apply {
            putExtra("EXPERIENCE_ID", experienceId)
            putExtra("EXPERIENCE_NOME", experienceNome)
            putExtra("RESERVA_CODIGO", reserva.codigo)
            putExtra("CAVERN_NOME", reserva.cavernaNome)
            putExtra("CHECK_IN", reserva.checkIn)
            putExtra("CHECK_OUT", reserva.checkOut)
        }
        startActivity(intent)
    }

    private fun fetchBookings() {
        lifecycleScope.launch {
            val apiList = try {
                val api = ApiClient.getApi(this@SelectBookingForExperienceActivity)
                val response = api.getMinhasReservas("proximas")
                if (response.isSuccessful && response.body() != null) {
                    response.body()!!.estadias
                } else {
                    emptyList()
                }
            } catch (_: Exception) {
                emptyList()
            }

            // Filtra estadias ativas (não canceladas, não expiradas, não pendentes) estritamente do usuário logado
            val activeBookings = apiList.filter { reserva ->
                val status = reserva.status.uppercase(Locale.getDefault())
                status != "CANCELADA" && status != "EXPIRADA" && status != "PENDENTE"
            }

            if (activeBookings.isEmpty()) {
                adapter.updateData(emptyList())
                binding.tvEmptyBookingsForExp.text = "Você não possui reservas ativas nesta conta para agendar experiências. Faça uma reserva primeiro!"
                binding.tvEmptyBookingsForExp.visibility = View.VISIBLE
                binding.rvBookingsForExp.visibility = View.GONE
            } else {
                adapter.updateData(activeBookings)
                binding.tvEmptyBookingsForExp.visibility = View.GONE
                binding.rvBookingsForExp.visibility = View.VISIBLE
            }
        }
    }
}
