package com.example.hugabugahotel.ui.states

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.hugabugahotel.databinding.ActivityError404Binding

class Error404Activity : AppCompatActivity() {

    private lateinit var binding: ActivityError404Binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityError404Binding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}
