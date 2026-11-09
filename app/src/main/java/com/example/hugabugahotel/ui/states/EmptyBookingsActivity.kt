package com.example.hugabugahotel.ui.states

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.hugabugahotel.databinding.ActivityEmptyBookingsBinding

class EmptyBookingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEmptyBookingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEmptyBookingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}
