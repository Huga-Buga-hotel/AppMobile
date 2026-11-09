package com.example.hugabugahotel.ui.states

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.hugabugahotel.databinding.ActivityLoadingStonesBinding

class LoadingStonesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoadingStonesBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoadingStonesBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}
