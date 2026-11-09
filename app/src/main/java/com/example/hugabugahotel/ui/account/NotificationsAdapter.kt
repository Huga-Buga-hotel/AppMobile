package com.example.hugabugahotel.ui.account

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.hugabugahotel.R
import com.example.hugabugahotel.data.model.NotificacaoDto

class NotificationsAdapter(private var items: List<NotificacaoDto>) :
    RecyclerView.Adapter<NotificationsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNotifTitle: TextView = view.findViewById(R.id.tvNotifTitle)
        val tvNotifDesc: TextView = view.findViewById(R.id.tvNotifDesc)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvNotifTitle.text = item.titulo
        holder.tvNotifDesc.text = item.descricao
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<NotificacaoDto>) {
        items = newItems
        notifyDataSetChanged()
    }
}
