package com.example.hugabugahotel.ui.banquet

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.hugabugahotel.R
import com.example.hugabugahotel.data.api.ApiClient
import com.example.hugabugahotel.data.api.ApiErrors
import com.example.hugabugahotel.data.api.SessionManager
import com.example.hugabugahotel.data.model.MesaHorario
import com.example.hugabugahotel.data.model.Prato
import com.example.hugabugahotel.data.model.ReservaMesaRequest
import com.example.hugabugahotel.databinding.ActivityBanquetBinding
import com.example.hugabugahotel.ui.account.MyBookingsActivity
import com.example.hugabugahotel.ui.account.ProfileActivity
import com.example.hugabugahotel.ui.caverns.CavernsListActivity
import com.example.hugabugahotel.ui.home.HomeActivity
import com.example.hugabugahotel.util.Formatos
import com.example.hugabugahotel.util.LanguageManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * O Banquete: turnos, cardápio por aba e reserva de mesa (salão de 50 lugares).
 * Com RESERVA_CODIGO (vindo do voucher) a mesa é de hóspede e ganha desconto; sem, é de visitante de fora.
 */
class BanquetActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBanquetBinding
    private val categorias = listOf("NA_FOGUEIRA", "ENTRADAS", "MENU_KIDS", "DA_HORTA", "SOBREMESAS", "POCOES")
    private var cardapio: List<Prato> = emptyList()
    private val pratosAdapter = PratosAdapter()
    private var reservaCodigo: String? = null
    private var descontoPct = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBanquetBinding.inflate(layoutInflater)
        setContentView(binding.root)

        reservaCodigo = intent.getStringExtra("RESERVA_CODIGO")
        setupNavigation()

        binding.menuList.layoutManager = LinearLayoutManager(this)
        binding.menuList.adapter = pratosAdapter
        if (binding.menuTabs.tabCount < categorias.size) binding.menuTabs.addTab(binding.menuTabs.newTab().setText("Poções"))
        binding.menuTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) = filtrar(tab.position)
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
        binding.btnOpenBooking.setOnClickListener { abrirReserva() }

        carregar()
    }

    override fun onResume() {
        super.onResume()
        LanguageManager.updateBottomNavLanguage(this, binding.root.findViewById(R.id.bottomNav))
    }

    private fun setupNavigation() {
        binding.root.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNav)?.let { nav ->
            nav.selectedItemId = R.id.nav_caverns
            nav.setOnItemSelectedListener { item ->
                val destino = when (item.itemId) {
                    R.id.nav_home -> HomeActivity::class.java
                    R.id.nav_caverns -> CavernsListActivity::class.java
                    R.id.nav_reservations -> MyBookingsActivity::class.java
                    R.id.nav_profile -> ProfileActivity::class.java
                    else -> return@setOnItemSelectedListener false
                }
                startActivity(Intent(this, destino))
                true
            }
        }
    }

    private fun carregar() {
        lifecycleScope.launch {
            try {
                val api = ApiClient.getApi(this@BanquetActivity)
                api.getRestauranteStatus().body()?.let { st ->
                    descontoPct = st.descontoHospedePct
                    val turnos = st.turnos.joinToString(" · ") { "${it.nome} ${it.abre.take(5)}–${it.fecha.take(5)}" }
                    binding.root.findViewById<TextView>(R.id.tvBanquetStatus)?.text =
                        (if (st.abertoAgora) "Aberto agora (${st.turnoAtual})" else "Fechado agora") +
                            "\n$turnos\nHóspede tem ${st.descontoHospedePct}% de desconto"
                }
                val r = api.getCardapio()
                if (r.isSuccessful) {
                    cardapio = r.body().orEmpty()
                    filtrar(binding.menuTabs.selectedTabPosition.coerceAtLeast(0))
                } else {
                    Toast.makeText(this@BanquetActivity, ApiErrors.mensagem(r), Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@BanquetActivity, ApiErrors.mensagem(e), Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun filtrar(aba: Int) {
        val categoria = categorias.getOrNull(aba)
        pratosAdapter.itens = cardapio.filter { categoria == null || it.categoria == categoria }
    }

    // ---------- reserva de mesa ----------

    private fun abrirReserva() {
        val sheet = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.view_book_table, null)
        sheet.setContentView(view)

        var data = LocalDate.now()
        var pessoas = 2
        var horario: String? = null
        val tvDia = view.findViewById<TextView>(R.id.bookDay)
        val tvPessoas = view.findViewById<TextView>(R.id.bookPeople)
        val grid = view.findViewById<GridLayout>(R.id.slotGrid)
        val btnBook = view.findViewById<Button>(R.id.btnBook)

        fun atualizarBotao() {
            btnBook.text = if (horario == null) "Escolha um horário" else "Uga! Reservar mesa · ${horario}"
            btnBook.isEnabled = horario != null
        }

        fun carregarHorarios() {
            tvDia.text = Formatos.dataCurta(data.toString())
            tvPessoas.text = pessoas.toString()
            horario = null
            atualizarBotao()
            lifecycleScope.launch {
                try {
                    val r = ApiClient.getApi(this@BanquetActivity).getHorariosMesa(data.toString(), pessoas)
                    if (!r.isSuccessful) return@launch Toast.makeText(this@BanquetActivity, ApiErrors.mensagem(r), Toast.LENGTH_LONG).show()
                    grid.removeAllViews()
                    for (h in r.body().orEmpty()) grid.addView(slot(grid, h) { escolhido ->
                        horario = escolhido
                        for (i in 0 until grid.childCount) grid.getChildAt(i).isSelected = (grid.getChildAt(i) as TextView).text == escolhido
                        atualizarBotao()
                    })
                } catch (e: Exception) {
                    Toast.makeText(this@BanquetActivity, ApiErrors.mensagem(e), Toast.LENGTH_LONG).show()
                }
            }
        }

        tvDia.setOnClickListener {
            val picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Dia da mesa")
                .setSelection(data.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli())
                .setCalendarConstraints(CalendarConstraints.Builder().setValidator(DateValidatorPointForward.now()).build())
                .build()
            picker.addOnPositiveButtonClickListener { ms ->
                data = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate()
                carregarHorarios()
            }
            picker.show(supportFragmentManager, "dia_mesa")
        }
        view.findViewById<View>(R.id.btnMinus).setOnClickListener { if (pessoas > 1) { pessoas--; carregarHorarios() } }
        view.findViewById<View>(R.id.btnPlus).setOnClickListener { if (pessoas < 12) { pessoas++; carregarHorarios() } }
        btnBook.setOnClickListener {
            val h = horario ?: return@setOnClickListener
            val base = ReservaMesaRequest(
                codigoReserva = reservaCodigo,
                data = data.toString(),
                horario = h,
                pessoas = pessoas,
                cadeirinhas = if (view.findViewById<SwitchCompat>(R.id.switchHighchair).isChecked) 1 else 0,
                pertoFogueira = view.findViewById<SwitchCompat>(R.id.switchFireSide).isChecked
            )
            if (reservaCodigo == null && !SessionManager(this).isLoggedIn()) {
                pedirContato { nome, email, celular ->
                    reservar(base.copy(nomeContato = nome, emailContato = email, celularContato = celular), sheet)
                }
            } else {
                reservar(base, sheet)
            }
        }

        carregarHorarios()
        sheet.show()
    }

    private fun slot(grid: GridLayout, h: MesaHorario, onClick: (String) -> Unit): View {
        val v = layoutInflater.inflate(R.layout.item_slot, grid, false) as TextView
        val hora = h.horario.take(5)
        v.text = hora
        v.isEnabled = h.status == "LIVRE"
        v.contentDescription = if (v.isEnabled) "$hora, ${h.vagasRestantes} lugares" else "$hora, lotado"
        v.setOnClickListener { onClick(hora) }
        return v
    }

    /** Visitante de fora sem conta: a API exige nome, e-mail e celular de contato. */
    private fun pedirContato(onOk: (String, String, String) -> Unit) {
        val pad = (16 * resources.displayMetrics.density).toInt()
        val nome = EditText(this).apply { hint = "Nome" }
        val email = EditText(this).apply { hint = "E-mail"; inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS }
        val celular = EditText(this).apply { hint = "Celular"; inputType = android.text.InputType.TYPE_CLASS_PHONE }
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(pad, pad / 2, pad, 0)
            addView(nome); addView(email); addView(celular)
        }
        AlertDialog.Builder(this)
            .setTitle("Seus dados de contato")
            .setView(layout)
            .setPositiveButton("Reservar") { _, _ ->
                val e = email.text.toString().trim()
                if (nome.text.isBlank() || celular.text.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(e).matches()) {
                    Toast.makeText(this, "Preencha nome, e-mail válido e celular", Toast.LENGTH_SHORT).show()
                } else {
                    SessionManager(this).saveResponsavelEmail(e) // para ver/cancelar a mesa depois
                    onOk(nome.text.toString().trim(), e, celular.text.toString().trim())
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun reservar(req: ReservaMesaRequest, sheet: BottomSheetDialog) {
        lifecycleScope.launch {
            try {
                val r = ApiClient.getApi(this@BanquetActivity).reservarMesa(req)
                val mesa = r.body()
                if (r.isSuccessful && mesa != null) {
                    sheet.dismiss()
                    val desconto = if (mesa.descontoHospedePct > 0) "\nDesconto de hóspede: ${mesa.descontoHospedePct}%" else ""
                    AlertDialog.Builder(this@BanquetActivity)
                        .setTitle("Mesa reservada! 🔥")
                        .setMessage("Código ${mesa.codigo}\n${Formatos.dataCompleta(mesa.data)} às ${mesa.horario.take(5)} · ${mesa.pessoas} pessoa(s)$desconto\n\nTolerância de 15 min · cancele até 2 h antes.")
                        .setPositiveButton("OK", null)
                        .show()
                } else {
                    // 409 VAGAS_ESGOTADAS (salão lotado), 422 estadia não confirmada, 400 data fora da estadia
                    AlertDialog.Builder(this@BanquetActivity).setTitle("Ugh!").setMessage(ApiErrors.mensagem(r))
                        .setPositiveButton("OK", null).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@BanquetActivity, ApiErrors.mensagem(e), Toast.LENGTH_LONG).show()
            }
        }
    }

    private class PratosAdapter : RecyclerView.Adapter<PratosAdapter.VH>() {
        var itens: List<Prato> = emptyList()
            set(value) { field = value; notifyDataSetChanged() }

        class VH(v: View) : RecyclerView.ViewHolder(v) {
            val nome: TextView = v.findViewById(R.id.dishName)
            val preco: TextView = v.findViewById(R.id.dishPrice)
            val desc: TextView = v.findViewById(R.id.dishDesc)
            val tag: TextView = v.findViewById(R.id.dishTag)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(LayoutInflater.from(parent.context).inflate(R.layout.item_dish, parent, false))

        override fun onBindViewHolder(h: VH, position: Int) {
            val p = itens[position]
            h.nome.text = p.nome
            h.preco.text = Formatos.reais(p.precoCentavos)
            h.desc.text = p.descricao.orEmpty()
            h.tag.text = p.tags.joinToString(" · ")
            h.tag.visibility = if (p.tags.isEmpty()) View.GONE else View.VISIBLE
        }

        override fun getItemCount() = itens.size
    }
}
