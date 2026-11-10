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
import com.example.hugabugahotel.data.api.ApiErrors
import com.example.hugabugahotel.data.model.RefreshRequest
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
            val session = SessionManager(this)
            binding.btnLogout.isEnabled = false
            lifecycleScope.launch {
                // revoga o refresh no servidor antes de apagar o token (melhor esforço: sai mesmo sem rede)
                session.getRefreshToken()?.let { refresh ->
                    kotlinx.coroutines.withTimeoutOrNull(3000) {
                        runCatching { ApiClient.getApi(this@ProfileActivity).logout(RefreshRequest(refresh)) }
                    }
                }
                session.clearAuth()
                Toast.makeText(this@ProfileActivity, "Sessão encerrada. Até logo!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this@ProfileActivity, AuthActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
                finish()
            }
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

        binding.menuHelp.setOnClickListener {
            startActivity(Intent(this, HelpActivity::class.java))
        }
    }

    private fun fetchProfile() {
        lifecycleScope.launch {
            try {
                val response = ApiClient.getApi(this@ProfileActivity).getUserProfile()
                val profile = response.body()
                if (response.isSuccessful && profile != null) {
                    bindProfile(profile)
                } else if (response.code() == 401) {
                    // sessão venceu e o refresh não deu: volta para o login
                    SessionManager(this@ProfileActivity).clearAuth()
                    startActivity(Intent(this@ProfileActivity, AuthActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this@ProfileActivity, ApiErrors.mensagem(response), Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                // sem rede: mostra o que está salvo no aparelho
                val session = SessionManager(this@ProfileActivity)
                bindFallbackProfile(session.getUserName().orEmpty(), session.getUserEmail().orEmpty())
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
        val fullName = listOfNotNull(profile.nome, profile.sobrenome).joinToString(" ").trim()
        binding.tvProfileName.text = fullName
        binding.tvPersonalName.text = fullName
        binding.tvAvatarInitials.text = buildInitials(fullName)
        binding.tvProfileSubtitle.text = if (profile.role == "ADMIN") "Equipe do hotel" else "Visitante da tribo"
        binding.tvPersonalContact.text = listOfNotNull(profile.email, profile.celular, profile.documentoMascarado)
            .joinToString("  ·  ")
    }

    private fun bindFallbackProfile(name: String, email: String) {
        binding.tvProfileName.text = name
        binding.tvPersonalName.text = name
        binding.tvAvatarInitials.text = buildInitials(name)
        binding.tvProfileSubtitle.text = "Visitante da tribo"
        binding.tvPersonalContact.text = email
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
            hint = "Nascimento AAAA-MM-DD (obrigatório p/ criança)"
            inputType = android.text.InputType.TYPE_CLASS_DATETIME or android.text.InputType.TYPE_DATETIME_VARIATION_DATE
        }
        layout.addView(etAge)

        AlertDialog.Builder(context)
            .setTitle("Adicionar Membro da Tribo")
            .setView(layout)
            .setPositiveButton("Adicionar") { _, _ ->
                val name = etName.text.toString().trim()
                val typeStr = if (spinnerType.selectedItemPosition == 0) "ADULTO" else "CRIANCA"
                val nascimento = etAge.text.toString().trim().ifEmpty { null }

                if (name.isNotEmpty()) {
                    adicionarMembro(name, typeStr, nascimento)
                } else {
                    Toast.makeText(context, "Nome não pode estar vazio", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun adicionarMembro(nome: String, tipo: String, nascimento: String?) {
        lifecycleScope.launch {
            try {
                val api = ApiClient.getApi(this@ProfileActivity)
                val membro = MembroTribo(id = null, nome = nome, tipo = tipo, dataNascimento = nascimento)
                val response = api.adicionarMembroTribo(membro)
                if (response.isSuccessful) {
                    Toast.makeText(this@ProfileActivity, "Membro adicionado à tribo!", Toast.LENGTH_SHORT).show()
                    fetchMembros()
                } else {
                    Toast.makeText(this@ProfileActivity, ApiErrors.mensagem(response), Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ProfileActivity, "Erro: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun buildInitials(nome: String): String {
        val clean = nome.trim()
        val partes = clean.split(" ").filter { it.isNotEmpty() }
        val first = partes.firstOrNull()?.firstOrNull()?.uppercaseChar() ?: 'H'
        val second = partes.getOrNull(1)?.firstOrNull()?.uppercaseChar() ?: (clean.getOrNull(1)?.uppercaseChar() ?: 'B')
        return "$first$second"
    }
}
