package com.example.hugabugahotel.ui.account

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.lifecycle.lifecycleScope
import com.example.hugabugahotel.data.api.ApiClient
import com.example.hugabugahotel.data.api.ApiErrors
import com.example.hugabugahotel.data.api.SessionManager
import com.example.hugabugahotel.data.model.PagamentoResumoDto
import android.widget.Toast
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
        if (!SessionManager(this).isLoggedIn()) return mostrar(emptyList())
        lifecycleScope.launch {
            try {
                val response = ApiClient.getApi(this@SavedPaymentsActivity).getPagamentos()
                if (response.isSuccessful) mostrar(response.body().orEmpty())
                else Toast.makeText(this@SavedPaymentsActivity, ApiErrors.mensagem(response), Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(this@SavedPaymentsActivity, ApiErrors.mensagem(e), Toast.LENGTH_LONG).show()
                mostrar(emptyList())
            }
        }
    }

    private fun mostrar(list: List<PagamentoResumoDto>) {
        adapter.updateData(list)
        binding.tvEmptyState.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        binding.rvSavedPayments.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
    }
}
