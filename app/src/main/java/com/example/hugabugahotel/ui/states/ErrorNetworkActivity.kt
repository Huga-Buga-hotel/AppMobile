package com.example.hugabugahotel.ui.states

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.hugabugahotel.databinding.ActivityErrorNetworkBinding

class ErrorNetworkActivity : AppCompatActivity() {

    private lateinit var binding: ActivityErrorNetworkBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityErrorNetworkBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}
