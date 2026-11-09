package com.example.hugabugahotel.ui.account

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.lifecycle.lifecycleScope
import com.example.hugabugahotel.data.api.ApiClient
import com.example.hugabugahotel.data.model.PagamentoSalvoDto
import com.example.hugabugahotel.databinding.ActivitySavedPaymentsBinding
import com.example.hugabugahotel.util.LanguageManager
import kotlinx.coroutines.launch

class SavedPaymentsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySavedPaymentsBinding
    private lateinit var adapter: SavedPaymentsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySavedPaymentsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.appBar.root.setOnClickListener {
            finish()
        }

        setupRecyclerView()
        fetchSavedPayments()
    }

    override fun onResume() {
        super.onResume()
        binding.tvTitleSavedPayments.text = LanguageManager.get(this, "saved_payments_title")
        binding.tvEmptyState.text = LanguageManager.get(this, "empty_payments")
    }

    private fun setupRecyclerView() {
        adapter = SavedPaymentsAdapter(emptyList())
        binding.rvSavedPayments.layoutManager = LinearLayoutManager(this)
        binding.rvSavedPayments.adapter = adapter
    }

    private fun fetchSavedPayments() {
        lifecycleScope.launch {
            val list = try {
                val api = ApiClient.getApi(this@SavedPaymentsActivity)
                val response = api.getPagamentosSalvos()
                if (response.isSuccessful && response.body() != null) {
                    response.body()!!
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                emptyList()
            }

            adapter.updateData(list)
            if (list.isEmpty()) {
                binding.tvEmptyState.visibility = View.VISIBLE
                binding.rvSavedPayments.visibility = View.GONE
            } else {
                binding.tvEmptyState.visibility = View.GONE
                binding.rvSavedPayments.visibility = View.VISIBLE
            }
        }
    }
}
