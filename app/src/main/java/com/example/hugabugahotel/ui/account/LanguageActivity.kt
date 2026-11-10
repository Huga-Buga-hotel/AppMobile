package com.example.hugabugahotel.ui.account

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.hugabugahotel.R
import com.example.hugabugahotel.util.AppScope
import com.example.hugabugahotel.data.api.ApiClient
import com.example.hugabugahotel.data.api.SessionManager
import com.example.hugabugahotel.data.model.PreferenciasDto
import kotlinx.coroutines.launch
import com.example.hugabugahotel.databinding.ActivityLanguageBinding
import com.example.hugabugahotel.ui.home.HomeActivity
import com.example.hugabugahotel.util.LanguageManager

class LanguageActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLanguageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLanguageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.appBar.root.setOnClickListener {
            finish()
        }

        val sessionManager = SessionManager(this)
        when (sessionManager.getLanguage()) {
            "cave" -> binding.rbCave.isChecked = true
            "en" -> binding.rbEn.isChecked = true
            else -> binding.rbPt.isChecked = true
        }

        binding.rgLanguage.setOnCheckedChangeListener { _, checkedId ->
            val lang = when (checkedId) {
                R.id.rbCave -> "cave"
                R.id.rbEn -> "en"
                else -> "pt"
            }
            sessionManager.saveLanguage(lang)
            // Com conta, o idioma também fica salvo no perfil (a linguagem "das cavernas" é só do app)
            if (sessionManager.isLoggedIn()) {
                val idiomaApi = if (lang == "en") "en" else "pt-BR"
                AppScope.launch {
                    runCatching { ApiClient.getApi(applicationContext).atualizarPreferencias(PreferenciasDto(idioma = idiomaApi)) }
                }
            }
            Toast.makeText(this, "Idioma alterado com sucesso!", Toast.LENGTH_SHORT).show()

            val intent = Intent(this, HomeActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        binding.tvTitleLanguage.text = LanguageManager.get(this, "language_title")
    }
}
