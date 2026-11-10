package com.example.hugabugahotel.ui.account

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.hugabugahotel.data.api.SessionManager
import com.example.hugabugahotel.databinding.ActivityNotificationsBinding
import com.example.hugabugahotel.util.LanguageManager

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

    /** Notificações locais do app (ex.: alertas de vaga criados). O backend não tem caixa de notificações. */
    private fun fetchNotifications() {
        val lista = SessionManager(this).getSavedNotifications()
        adapter.updateData(lista)
        binding.tvEmptyState.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
        binding.rvNotifications.visibility = if (lista.isEmpty()) View.GONE else View.VISIBLE
    }
}
