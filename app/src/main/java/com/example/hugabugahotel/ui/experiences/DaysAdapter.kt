package com.example.hugabugahotel.ui.experiences

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.hugabugahotel.R

data class DayItem(
    val name: String,       // e.g. "SÁB"
    val number: String,     // e.g. "12"
    val fullLabel: String,  // e.g. "SÁBADO, 12 DE DEZEMBRO"
    val isoDate: String,    // e.g. "2026-12-01"
    var isSelected: Boolean = false
)

class DaysAdapter(
    private var items: List<DayItem> = emptyList(),
    private val onDaySelected: (DayItem) -> Unit
) : RecyclerView.Adapter<DaysAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val dayName: TextView = view.findViewById(R.id.dayName)
        val dayNumber: TextView = view.findViewById(R.id.dayNumber)
        val dayHint: TextView = view.findViewById(R.id.dayHint)
        val container: View = view
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_day, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val context = holder.itemView.context

        holder.dayName.text = item.name
        holder.dayNumber.text = item.number
        holder.dayHint.text = if (item.isSelected) "Ativo" else "Estadia"

        if (item.isSelected) {
            holder.container.setBackgroundResource(R.drawable.bg_chip_selected)
            holder.dayName.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.white))
            holder.dayNumber.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.white))
            holder.dayHint.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.white))
        } else {
            holder.container.setBackgroundResource(R.drawable.bg_day)
            holder.dayName.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.hb_muted))
            holder.dayNumber.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.hb_dark))
            holder.dayHint.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.hb_muted))
        }

        holder.container.setOnClickListener {
            items.forEach { it.isSelected = false }
            item.isSelected = true
            notifyDataSetChanged()
            onDaySelected(item)
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<DayItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}
