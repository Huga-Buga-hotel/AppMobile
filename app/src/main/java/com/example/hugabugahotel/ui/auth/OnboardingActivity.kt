package com.example.hugabugahotel.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.hugabugahotel.databinding.ActivityOnboardingBinding
import com.example.hugabugahotel.ui.home.HomeActivity

class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSkip.setOnClickListener {
            irParaHome()
        }

        binding.bottomBar.setOnClickListener {
            irParaHome()
        }
    }

    private fun irParaHome() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }
}
