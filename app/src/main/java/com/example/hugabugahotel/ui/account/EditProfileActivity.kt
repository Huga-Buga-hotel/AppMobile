package com.example.hugabugahotel.ui.account

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.hugabugahotel.data.api.ApiClient
import com.example.hugabugahotel.data.api.ApiErrors
import com.example.hugabugahotel.data.model.UpdateProfileRequest
import com.example.hugabugahotel.databinding.ActivityEditProfileBinding
import kotlinx.coroutines.launch

class EditProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.appBar.root.setOnClickListener {
            finish()
        }

        loadCurrentProfile()

        binding.btnSave.setOnClickListener {
            salvarPerfil()
        }
    }

    private fun loadCurrentProfile() {
        lifecycleScope.launch {
            try {
                val api = ApiClient.getApi(this@EditProfileActivity)
                val response = api.getUserProfile()
                if (response.isSuccessful && response.body() != null) {
                    val p = response.body()!!
                    val fullName = "${p.nome} ${p.sobrenome ?: ""}".trim()
                    binding.etName.setText(fullName)
                    binding.etPhone.setText(p.celular ?: "")
                    binding.etBirthDate.setText(p.dataNascimento ?: "")
                    binding.cbOffers.isChecked = p.aceitaOfertas
                }
            } catch (e: Exception) {
                Toast.makeText(this@EditProfileActivity, "Erro ao carregar perfil: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun salvarPerfil() {
        val name = binding.etName.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val birthDate = binding.etBirthDate.text.toString().trim()
        val offers = binding.cbOffers.isChecked

        if (name.isEmpty()) {
            Toast.makeText(this, "O nome não pode estar vazio", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val api = ApiClient.getApi(this@EditProfileActivity)
                val partes = name.split(Regex("\\s+"))
                val request = UpdateProfileRequest(
                    nome = partes.first(),
                    sobrenome = partes.drop(1).joinToString(" ").ifEmpty { null },
                    celular = phone.ifEmpty { null },
                    aceitaOfertas = offers,
                    dataNascimento = birthDate.ifEmpty { null }
                )
                val response = api.updateUserProfile(request)
                if (response.isSuccessful) {
                    Toast.makeText(this@EditProfileActivity, "Perfil atualizado com sucesso! Uga! 🪨", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                } else {
                    Toast.makeText(this@EditProfileActivity, ApiErrors.mensagem(response), Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@EditProfileActivity, "Erro de conexão: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
