package com.example.hugabugahotel.ui.experiences

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.hugabugahotel.data.api.ApiClient
import com.example.hugabugahotel.data.api.ApiErrors
import com.example.hugabugahotel.data.api.SessionManager
import com.example.hugabugahotel.databinding.ActivitySelectBookingBinding
import com.example.hugabugahotel.ui.account.BookingItem
import com.example.hugabugahotel.ui.account.BookingsAdapter
import kotlinx.coroutines.launch

/** Escolhe em qual reserva CONFIRMADA a atividade será agendada (a API só agenda para hóspede confirmado). */
class SelectBookingForExperienceActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySelectBookingBinding
    private lateinit var adapter: BookingsAdapter
    private var experienceId: String = ""
    private var experienceNome: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySelectBookingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        experienceId = intent.getStringExtra("EXPERIENCE_ID") ?: run { finish(); return }
        experienceNome = intent.getStringExtra("EXPERIENCE_NOME") ?: "Experiência"
        binding.tvSelectBookingTitle.text = "Escolha sua reserva para $experienceNome"

        adapter = BookingsAdapter(emptyList(), isSelectionMode = true, selectionButtonText = "Escolher",
            onSelect = { reserva ->
                startActivity(Intent(this, ExperienceScheduleActivity::class.java).apply {
                    putExtra("EXPERIENCE_ID", experienceId)
                    putExtra("EXPERIENCE_NOME", experienceNome)
                    putExtra("RESERVA_CODIGO", reserva.codigo)
                })
            })
        binding.rvBookingsForExp.layoutManager = LinearLayoutManager(this)
        binding.rvBookingsForExp.adapter = adapter
        fetchBookings()
    }

    private fun fetchBookings() {
        lifecycleScope.launch {
            val session = SessionManager(this@SelectBookingForExperienceActivity)
            val api = ApiClient.getApi(this@SelectBookingForExperienceActivity)
            val confirmadas = try {
                if (session.isLoggedIn()) {
                    api.getMinhasReservas("proximas").body()?.estadias.orEmpty().map { BookingItem.de(it) }
                } else {
                    session.getGuestBookings().mapNotNull { runCatching { api.getVoucherReserva(it).body() }.getOrNull() }
                        .map { BookingItem.de(it) }
                }.filter { it.status == "CONFIRMADA" }
            } catch (e: Exception) {
                binding.tvEmptyBookingsForExp.text = ApiErrors.mensagem(e)
                emptyList()
            }

            adapter.updateData(confirmadas)
            if (confirmadas.isEmpty()) {
                if (binding.tvEmptyBookingsForExp.text.isNullOrBlank() || session.isLoggedIn() || session.getGuestBookings().isEmpty()) {
                    binding.tvEmptyBookingsForExp.text =
                        "Você não tem reservas confirmadas para agendar atividades. Reserve uma caverna primeiro!"
                }
                binding.tvEmptyBookingsForExp.visibility = View.VISIBLE
                binding.rvBookingsForExp.visibility = View.GONE
            } else {
                binding.tvEmptyBookingsForExp.visibility = View.GONE
                binding.rvBookingsForExp.visibility = View.VISIBLE
            }
        }
    }
}
