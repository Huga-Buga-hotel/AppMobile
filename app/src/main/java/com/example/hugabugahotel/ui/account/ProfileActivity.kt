package com.example.hugabugahotel.ui.account

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.hugabugahotel.R
import com.example.hugabugahotel.data.api.ApiClient
import com.example.hugabugahotel.data.api.SessionManager
import com.example.hugabugahotel.data.model.MembroTribo
import com.example.hugabugahotel.data.model.UserProfileResponse
import com.example.hugabugahotel.databinding.ActivityProfileBinding
import com.example.hugabugahotel.ui.auth.AuthActivity
import com.example.hugabugahotel.ui.caverns.CavernsListActivity
import com.example.hugabugahotel.ui.home.HomeActivity
import com.example.hugabugahotel.util.LanguageManager
import com.example.hugabugahotel.util.setupMainNavigation
import kotlinx.coroutines.launch

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding

    private val editProfileLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            fetchProfile()
            fetchMembros()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sessionManager = SessionManager(this)
        if (!sessionManager.isLoggedIn()) {
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
            return
        }

        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigation()
        setupLogout()
        setupListeners()
        fetchProfile()
        fetchMembros()
    }

    override fun onResume() {
        super.onResume()
        binding.bottomNav.selectedItemId = R.id.nav_profile
        applyLanguage()
        fetchProfile()
        fetchMembros()
    }

    private fun applyLanguage() {
        binding.tvPersonalDataTitle.text = LanguageManager.get(this, "personal_data")
        binding.btnEditPersonal.text = LanguageManager.get(this, "edit")
        binding.tvTribeMembersTitle.text = LanguageManager.get(this, "tribe_members")
        binding.btnAddMember.text = LanguageManager.get(this, "add")
        binding.menuSavedPayments.text = LanguageManager.get(this, "saved_payments")
        binding.menuFavoriteCaverns.text = LanguageManager.get(this, "favorite_caverns")
        binding.menuNotifications.text = LanguageManager.get(this, "notifications")
        binding.menuLanguage.text = LanguageManager.get(this, "language")
        binding.menuHelp.text = LanguageManager.get(this, "help")
        binding.tvLogoutText.text = LanguageManager.get(this, "logout")

        LanguageManager.updateBottomNavLanguage(this, binding.bottomNav)
    }

    private fun setupNavigation() {
        binding.bottomNav.setupMainNavigation(this, R.id.nav_profile)
    }

    private fun setupLogout() {
        binding.btnLogout.setOnClickListener {
            SessionManager(this).clearSession()
            Toast.makeText(this, "Sessão encerrada. Até logo! 🪨", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, AuthActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun setupListeners() {
        binding.btnEditPersonal.setOnClickListener {
            editProfileLauncher.launch(Intent(this, EditProfileActivity::class.java))
        }

        binding.btnAddMember.setOnClickListener {
            showAddMemberDialog()
        }

        binding.menuSavedPayments.setOnClickListener {
            startActivity(Intent(this, SavedPaymentsActivity::class.java))
        }

        binding.menuFavoriteCaverns.setOnClickListener {
            startActivity(Intent(this, FavoriteCavernsActivity::class.java))
        }

        binding.menuNotifications.setOnClickListener {
            startActivity(Intent(this, NotificationsActivity::class.java))
        }

        binding.menuLanguage.setOnClickListener {
            startActivity(Intent(this, LanguageActivity::class.java))
        }

        binding.menuHelp.setOnClickListener {
            startActivity(Intent(this, HelpActivity::class.java))
        }
    }

    private fun fetchProfile() {
        lifecycleScope.launch {
            try {
                val api = ApiClient.getApi(this@ProfileActivity)
                val response = api.getUserProfile()
                if (response.isSuccessful && response.body() != null) {
                    val profile = response.body()!!
                    bindProfile(profile)
                }

                val ossosResponse = api.getOssos()
                if (ossosResponse.isSuccessful && ossosResponse.body() != null) {
                    val ossos = ossosResponse.body()!!
                    binding.tvBonesBalance.text = ossos.saldo.toString()
                }
            } catch (e: Exception) {
                val sessionManager = SessionManager(this@ProfileActivity)
                val fallbackName = sessionManager.getUserName() ?: "Marina Souza"
                val fallbackEmail = sessionManager.getUserEmail() ?: "marina@tribo.com"
                bindFallbackProfile(fallbackName, fallbackEmail)
            }
        }
    }

    private fun fetchMembros() {
        lifecycleScope.launch {
            try {
                val api = ApiClient.getApi(this@ProfileActivity)
                val response = api.getMembrosTribo()
                if (response.isSuccessful && response.body() != null) {
                    val membros = response.body()!!
                    bindMembros(membros)
                }
            } catch (e: Exception) {
                // Em caso de erro ou sem rede
            }
        }
    }

    private fun bindProfile(profile: UserProfileResponse) {
        val fullName = profile.nome.trim()
        binding.tvProfileName.text = fullName
        binding.tvPersonalName.text = fullName

        val initials = buildInitials(profile.nome)
        binding.tvAvatarInitials.text = initials

        binding.tvProfileSubtitle.text = profile.nivel
        binding.tvBonesBalance.text = profile.saldoOssos.toString()

        val contact = "${profile.email}  ${profile.celular ?: "+55 (54) 9 0000-0000"}"
        binding.tvPersonalContact.text = contact
    }

    private fun bindFallbackProfile(name: String, email: String) {
        binding.tvProfileName.text = name
        binding.tvPersonalName.text = name
        binding.tvAvatarInitials.text = buildInitials(name)
        binding.tvProfileSubtitle.text = "Caçador(a)"
        binding.tvBonesBalance.text = "340"
        binding.tvPersonalContact.text = "$email · +55 (54) 9 0000-0000"
    }

    private fun bindMembros(membros: List<MembroTribo>) {
        binding.membersContainer.removeAllViews()
        for (membro in membros) {
            val itemView = layoutInflater.inflate(R.layout.item_tribe_member, binding.membersContainer, false)
            val tvName = itemView.findViewById<TextView>(R.id.tvMemberName)
            val tvRole = itemView.findViewById<TextView>(R.id.tvMemberRole)

            tvName?.text = membro.nome
            val tipoText = if (membro.tipo.equals("ADULTO", ignoreCase = true)) {
                "Adulto"
            } else {
                "Criança${membro.idade?.let { " ($it anos)" } ?: ""}"
            }
            tvRole?.text = tipoText

            binding.membersContainer.addView(itemView)
        }
    }

    private fun showAddMemberDialog() {
        val context = this
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 30, 40, 10)
        }

        val etName = EditText(context).apply {
            hint = "Nome do membro"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_CAP_WORDS
        }
        layout.addView(etName)

        val spinnerType = Spinner(context).apply {
            adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, listOf("Adulto", "Criança"))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = 20
                bottomMargin = 20
            }
        }
        layout.addView(spinnerType)

        val etAge = EditText(context).apply {
            hint = "Idade (opcional)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }
        layout.addView(etAge)

        AlertDialog.Builder(context)
            .setTitle("Adicionar Membro da Tribo")
            .setView(layout)
            .setPositiveButton("Adicionar") { _, _ ->
                val name = etName.text.toString().trim()
                val typeStr = if (spinnerType.selectedItemPosition == 0) "ADULTO" else "CRIANCA"
                val age = etAge.text.toString().toIntOrNull()

                if (name.isNotEmpty()) {
                    adicionarMembro(name, typeStr, age)
                } else {
                    Toast.makeText(context, "Nome não pode estar vazio", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun adicionarMembro(nome: String, tipo: String, idade: Int?) {
        lifecycleScope.launch {
            try {
                val api = ApiClient.getApi(this@ProfileActivity)
                val membro = MembroTribo(id = null, nome = nome, tipo = tipo, idade = idade)
                val response = api.adicionarMembroTribo(membro)
                if (response.isSuccessful) {
                    Toast.makeText(this@ProfileActivity, "Membro adicionado à tribo! 🪨", Toast.LENGTH_SHORT).show()
                    fetchMembros()
                } else {
                    Toast.makeText(this@ProfileActivity, "Erro ao adicionar membro", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ProfileActivity, "Erro: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun buildInitials(nome: String): String {
        val clean = nome.trim()
        val first = clean.firstOrNull()?.uppercaseChar() ?: 'M'
        val second = clean.drop(1).firstOrNull()?.uppercaseChar() ?: 'S'
        return "$first$second"
    }
}
