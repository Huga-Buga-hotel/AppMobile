package com.example.hugabugahotel.ui.auth

import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.hugabugahotel.R
import com.example.hugabugahotel.data.api.ApiClient
import com.example.hugabugahotel.data.api.SessionManager
import com.example.hugabugahotel.data.model.CadastroRequest
import com.example.hugabugahotel.data.model.LoginRequest
import com.example.hugabugahotel.databinding.ActivityAuthBinding
import com.example.hugabugahotel.ui.home.HomeActivity
import com.example.hugabugahotel.util.LanguageManager
import kotlinx.coroutines.launch

class AuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthBinding
    private var isLoginMode = true // true = Login, false = Register
    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupTabs()
        setupListeners()
        setupPasswordToggle()
    }

    override fun onResume() {
        super.onResume()
        applyLanguage()
    }

    private fun applyLanguage() {
        binding.tabLogin.text = LanguageManager.get(this, "auth_tab_login")
        binding.tabRegister.text = LanguageManager.get(this, "auth_tab_register")
        binding.tvGuestLink.text = LanguageManager.get(this, "auth_guest_link")
        updateAuthMode()
    }

    private fun setupPasswordToggle() {
        binding.btnTogglePassword.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                binding.etPassword.transformationMethod = HideReturnsTransformationMethod.getInstance()
            } else {
                binding.etPassword.transformationMethod = PasswordTransformationMethod.getInstance()
            }
            binding.etPassword.setSelection(binding.etPassword.text.length)
        }
    }

    private fun setupTabs() {
        updateAuthMode()

        binding.tabLogin.setOnClickListener {
            isLoginMode = true
            updateAuthMode()
        }

        binding.tabRegister.setOnClickListener {
            isLoginMode = false
            updateAuthMode()
        }
    }

    private fun updateAuthMode() {
        if (isLoginMode) {
            binding.tabLogin.setBackgroundResource(R.drawable.bg_chip_selected)
            binding.tabLogin.setTextColor(resources.getColor(android.R.color.white, theme))
            binding.tabRegister.setBackgroundResource(R.drawable.bg_chip)
            binding.tabRegister.setTextColor(resources.getColor(R.color.hb_ink, theme))

            binding.layoutName.visibility = View.GONE
            binding.btnSubmit.text = LanguageManager.get(this, "auth_btn_login")
        } else {
            binding.tabRegister.setBackgroundResource(R.drawable.bg_chip_selected)
            binding.tabRegister.setTextColor(resources.getColor(android.R.color.white, theme))
            binding.tabLogin.setBackgroundResource(R.drawable.bg_chip)
            binding.tabLogin.setTextColor(resources.getColor(R.color.hb_ink, theme))

            binding.layoutName.visibility = View.VISIBLE
            binding.btnSubmit.text = LanguageManager.get(this, "auth_btn_register")
        }
    }

    private fun setupListeners() {
        binding.btnSubmit.setOnClickListener {
            if (isLoginMode) {
                realizarLogin()
            } else {
                realizarRegistro()
            }
        }

        binding.tvGuestLink.setOnClickListener {
            SessionManager(this).saveSession(
                accessToken = "guest_token_hb",
                refreshToken = null,
                userId = "guest",
                userName = "Convidado da Tribo",
                userEmail = "convidado@tribo.com"
            )
            Toast.makeText(this, "Entrando como Visitante da Tribo 🪨", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }
    }

    private fun realizarLogin() {
        val email = binding.etEmail.text.toString().trim()
        val senha = binding.etPassword.text.toString()

        if (email.isEmpty() || senha.isEmpty()) {
            Toast.makeText(this, "Preencha e-mail e senha", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val api = ApiClient.getApi(this@AuthActivity)
                val response = api.login(LoginRequest(email, senha))
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    SessionManager(this@AuthActivity).saveSession(
                        accessToken = body.accessToken,
                        refreshToken = body.refreshToken,
                        userId = body.usuarioId,
                        userName = body.nome,
                        userEmail = email
                    )
                    Toast.makeText(this@AuthActivity, "Uga! Bem-vindo de volta!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@AuthActivity, HomeActivity::class.java))
                    finish()
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "E-mail ou senha incorretos"
                    Toast.makeText(this@AuthActivity, "Erro no login: $errorMsg", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@AuthActivity, "Falha de conexão com a API: ${e.localizedMessage ?: e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun realizarRegistro() {
        val fullName = binding.etName.text.toString().trim().ifEmpty { "Marina Souza" }
        val parts = fullName.split(" ")
        val nome = parts.firstOrNull() ?: "Marina"
        val sobrenome = parts.drop(1).joinToString(" ").ifEmpty { "Souza" }
        val email = binding.etEmail.text.toString().trim()
        val senha = binding.etPassword.text.toString()

        if (email.isEmpty() || senha.isEmpty()) {
            Toast.makeText(this, "Preencha e-mail e senha", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val api = ApiClient.getApi(this@AuthActivity)
                val request = CadastroRequest(
                    nome = nome,
                    sobrenome = sobrenome,
                    email = email,
                    senha = senha,
                    celular = "+5554999999999",
                    numeroDocumento = "12345678909",
                    dataNascimento = "1990-01-01"
                )
                val response = api.cadastro(request)
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    SessionManager(this@AuthActivity).saveSession(
                        accessToken = body.accessToken,
                        refreshToken = body.refreshToken,
                        userId = body.usuarioId,
                        userName = body.nome,
                        userEmail = email
                    )
                    Toast.makeText(this@AuthActivity, "Conta criada com sucesso! Uga!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@AuthActivity, HomeActivity::class.java))
                    finish()
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Erro ao cadastrar"
                    Toast.makeText(this@AuthActivity, "Erro no cadastro: $errorMsg", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@AuthActivity, "Falha de conexão com a API: ${e.localizedMessage ?: e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
