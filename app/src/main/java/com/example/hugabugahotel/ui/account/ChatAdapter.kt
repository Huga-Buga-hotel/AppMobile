package com.example.hugabugahotel.ui.account

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.hugabugahotel.R

data class ChatMessage(val text: String, val isUser: Boolean)

class ChatAdapter(private val messages: List<ChatMessage>) : RecyclerView.Adapter<ChatAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvMessageText: TextView = view.findViewById(R.id.tvMessageText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_message, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val msg = messages[position]
        holder.tvMessageText.text = msg.text
        if (msg.isUser) {
            holder.tvMessageText.setBackgroundResource(R.drawable.bg_chip_selected)
            holder.tvMessageText.setTextColor(holder.itemView.context.getColor(R.color.white))
        } else {
            holder.tvMessageText.setBackgroundResource(R.drawable.bg_card)
            holder.tvMessageText.setTextColor(holder.itemView.context.getColor(R.color.hb_ink))
        }
    }

    override fun getItemCount(): Int = messages.size
}
