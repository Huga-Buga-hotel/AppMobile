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
import androidx.appcompat.app.AlertDialog
import android.widget.EditText
import com.example.hugabugahotel.data.api.ApiClient
import com.example.hugabugahotel.data.api.ApiErrors
import com.example.hugabugahotel.data.api.SessionManager
import com.example.hugabugahotel.data.model.*
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
            binding.tvForgotPassword.visibility = View.VISIBLE
            binding.btnSubmit.text = LanguageManager.get(this, "auth_btn_login")
        } else {
            binding.tabRegister.setBackgroundResource(R.drawable.bg_chip_selected)
            binding.tabRegister.setTextColor(resources.getColor(android.R.color.white, theme))
            binding.tabLogin.setBackgroundResource(R.drawable.bg_chip)
            binding.tabLogin.setTextColor(resources.getColor(R.color.hb_ink, theme))

            binding.layoutName.visibility = View.VISIBLE
            binding.tvForgotPassword.visibility = View.GONE
            binding.btnSubmit.text = LanguageManager.get(this, "auth_btn_register")
        }
    }

    private fun setupListeners() {
        binding.btnSubmit.setOnClickListener {
            if (isLoginMode) realizarLogin() else realizarRegistro()
        }

        binding.tvForgotPassword.setOnClickListener { esqueciSenha() }

        // Visitante: navega sem conta. Nenhum token é salvo (a API recusaria um token inventado);
        // para reservar, o e-mail do responsável é pedido no checkout.
        binding.tvGuestLink.setOnClickListener {
            SessionManager(this).clearAuth()
            Toast.makeText(this, "Entrando como visitante da tribo", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }
    }

    private fun entrar(body: LoginResponse, email: String, boasVindas: String) {
        SessionManager(this).saveSession(
            accessToken = body.accessToken,
            refreshToken = body.refreshToken,
            userId = body.usuarioId,
            userName = body.nome,
            userEmail = email,
            role = body.role
        )
        Toast.makeText(this, boasVindas, Toast.LENGTH_SHORT).show()
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }

    private fun realizarLogin() {
        val email = binding.etEmail.text.toString().trim()
        val senha = binding.etPassword.text.toString()
        if (email.isEmpty() || senha.isEmpty()) {
            Toast.makeText(this, "Preencha e-mail e senha", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnSubmit.isEnabled = false
        lifecycleScope.launch {
            try {
                val response = ApiClient.getApi(this@AuthActivity).login(LoginRequest(email, senha))
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    entrar(body, email, "Uga! Bem-vindo de volta, ${body.nome}!")
                } else {
                    mostrarErro(ApiErrors.mensagem(response))
                }
            } catch (e: Exception) {
                mostrarErro(ApiErrors.mensagem(e))
            } finally {
                binding.btnSubmit.isEnabled = true
            }
        }
    }

    private fun realizarRegistro() {
        val partes = binding.etName.text.toString().trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        val email = binding.etEmail.text.toString().trim()
        val senha = binding.etPassword.text.toString()
        val celular = binding.etPhone.text.toString().trim()
        val cpf = binding.etDocument.text.toString().filter { it.isDigit() }

        when {
            partes.size < 2 -> return aviso("Informe nome e sobrenome")
            email.isEmpty() -> return aviso("Informe o e-mail")
            senha.length < 8 -> return aviso("A senha precisa ter pelo menos 8 caracteres")
            celular.isEmpty() -> return aviso("Informe o celular")
            cpf.length != 11 -> return aviso("Informe um CPF com 11 dígitos")
        }

        binding.btnSubmit.isEnabled = false
        lifecycleScope.launch {
            try {
                val request = CadastroRequest(
                    nome = partes.first(),
                    sobrenome = partes.drop(1).joinToString(" "),
                    email = email,
                    senha = senha,
                    celular = celular,
                    numeroDocumento = cpf
                )
                val response = ApiClient.getApi(this@AuthActivity).cadastro(request)
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    entrar(body, email, "Conta criada! Uga, ${body.nome}!")
                } else {
                    mostrarErro(ApiErrors.mensagem(response))
                }
            } catch (e: Exception) {
                mostrarErro(ApiErrors.mensagem(e))
            } finally {
                binding.btnSubmit.isEnabled = true
            }
        }
    }

    /** Pede o link de redefinição. Sem servidor de e-mail, o backend em perfil dev devolve o token. */
    private fun esqueciSenha() {
        val email = binding.etEmail.text.toString().trim()
        if (email.isEmpty()) return aviso("Digite seu e-mail no campo acima")
        lifecycleScope.launch {
            try {
                val api = ApiClient.getApi(this@AuthActivity)
                val response = api.esqueciSenha(EsqueciSenhaRequest(email))
                val body = response.body()
                if (!response.isSuccessful || body == null) return@launch mostrarErro(ApiErrors.mensagem(response))
                if (body.tokenDev == null) {
                    mostrarErro(body.mensagem, "Confira seu e-mail")
                } else {
                    pedirNovaSenha(body.tokenDev)
                }
            } catch (e: Exception) {
                mostrarErro(ApiErrors.mensagem(e))
            }
        }
    }

    private fun pedirNovaSenha(token: String) {
        val campo = EditText(this).apply {
            hint = "Nova senha (mín. 8 caracteres)"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        AlertDialog.Builder(this)
            .setTitle("Criar nova senha")
            .setView(campo)
            .setPositiveButton("Salvar") { _, _ ->
                lifecycleScope.launch {
                    try {
                        val r = ApiClient.getApi(this@AuthActivity)
                            .redefinirSenha(RedefinirSenhaRequest(token, campo.text.toString()))
                        if (r.isSuccessful) aviso("Senha alterada! Entre com a nova senha.")
                        else mostrarErro(ApiErrors.mensagem(r))
                    } catch (e: Exception) {
                        mostrarErro(ApiErrors.mensagem(e))
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun aviso(texto: String) {
        Toast.makeText(this, texto, Toast.LENGTH_SHORT).show()
    }

    private fun mostrarErro(texto: String, titulo: String = "Ugh!") {
        AlertDialog.Builder(this).setTitle(titulo).setMessage(texto).setPositiveButton("OK", null).show()
    }
}
