package com.example.hugabugahotel.ui.account

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.hugabugahotel.R
import com.example.hugabugahotel.data.model.PagamentoSalvoDto

class SavedPaymentsAdapter(private var items: List<PagamentoSalvoDto>) :
    RecyclerView.Adapter<SavedPaymentsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvPaymentTitle: TextView = view.findViewById(R.id.tvPaymentTitle)
        val tvPaymentDetails: TextView = view.findViewById(R.id.tvPaymentDetails)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_saved_payment, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvPaymentTitle.text = item.titulo
        holder.tvPaymentDetails.text = item.detalhes
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<PagamentoSalvoDto>) {
        items = newItems
        notifyDataSetChanged()
    }
}
