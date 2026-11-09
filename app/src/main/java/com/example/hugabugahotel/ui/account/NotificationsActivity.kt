package com.example.hugabugahotel.ui.account

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.lifecycle.lifecycleScope
import com.example.hugabugahotel.data.api.ApiClient
import com.example.hugabugahotel.data.api.SessionManager
import com.example.hugabugahotel.data.model.NotificacaoDto
import com.example.hugabugahotel.databinding.ActivityNotificationsBinding
import com.example.hugabugahotel.util.LanguageManager
import kotlinx.coroutines.launch

class NotificationsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNotificationsBinding
    private lateinit var adapter: NotificationsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.appBar.root.setOnClickListener {
            finish()
        }

        setupRecyclerView()
        fetchNotifications()
    }

    override fun onResume() {
        super.onResume()
        binding.tvTitleNotifications.text = LanguageManager.get(this, "notifications_title")
        binding.tvEmptyState.text = LanguageManager.get(this, "empty_notifications")
        fetchNotifications()
    }

    private fun setupRecyclerView() {
        adapter = NotificationsAdapter(emptyList())
        binding.rvNotifications.layoutManager = LinearLayoutManager(this)
        binding.rvNotifications.adapter = adapter
    }

    private fun fetchNotifications() {
        lifecycleScope.launch {
            val apiList = try {
                val api = ApiClient.getApi(this@NotificationsActivity)
                val response = api.getNotificacoes()
                if (response.isSuccessful && response.body() != null) {
                    response.body()!!
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                emptyList()
            }

            val sessionManager = SessionManager(this@NotificationsActivity)
            val localList = sessionManager.getSavedNotifications()
            val combinedList = (localList + apiList).distinctBy { it.id ?: it.titulo }

            adapter.updateData(combinedList)
            if (combinedList.isEmpty()) {
                binding.tvEmptyState.visibility = View.VISIBLE
                binding.rvNotifications.visibility = View.GONE
            } else {
                binding.tvEmptyState.visibility = View.GONE
                binding.rvNotifications.visibility = View.VISIBLE
            }
        }
    }
}
