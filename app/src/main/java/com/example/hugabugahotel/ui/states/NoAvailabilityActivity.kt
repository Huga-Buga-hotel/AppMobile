package com.example.hugabugahotel.ui.states

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.hugabugahotel.databinding.ActivityNoAvailabilityBinding

class NoAvailabilityActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNoAvailabilityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNoAvailabilityBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}
