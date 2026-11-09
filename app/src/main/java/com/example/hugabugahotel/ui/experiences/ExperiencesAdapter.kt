package com.example.hugabugahotel.ui.experiences

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.hugabugahotel.R
import com.example.hugabugahotel.data.model.AtividadeItem
import java.util.Locale

class ExperiencesAdapter(
    private var items: List<AtividadeItem> = emptyList(),
    private val onItemClick: ((AtividadeItem) -> Unit)? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_FEATURED = 0
        private const val TYPE_NORMAL = 1
    }

    class FeaturedViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.tvFeaturedTitle)
        val desc: TextView = view.findViewById(R.id.tvFeaturedDesc)
        val price: TextView = view.findViewById(R.id.tvFeaturedPrice)
        val btnReserve: View = view.findViewById(R.id.btnFeaturedReserve)
    }

    class NormalViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val photo: ImageView = view.findViewById(R.id.photo)
        val title: TextView = view.findViewById(R.id.title)
        val meta: TextView = view.findViewById(R.id.meta)
        val price: TextView = view.findViewById(R.id.price)
        val spots: TextView = view.findViewById(R.id.spots)
    }

    override fun getItemViewType(position: Int): Int {
        // The first item (position 0) is the most popular adventure (featured)
        return if (position == 0 && items.isNotEmpty()) TYPE_FEATURED else TYPE_NORMAL
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_FEATURED) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_experience_featured, parent, false)
            FeaturedViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_experience, parent, false)
            NormalViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        if (holder is FeaturedViewHolder) {
            holder.title.text = item.displayNome
            holder.desc.text = item.descricao ?: "Atividade em destaque da tribo"
            holder.price.text = if (item.precoCentavos == 0L) "Grátis" else "R$ ${String.format(Locale.getDefault(), "%,d", item.precoCentavos / 100)}"
            holder.btnReserve.setOnClickListener {
                onItemClick?.invoke(item)
            }
            holder.itemView.setOnClickListener {
                onItemClick?.invoke(item)
            }
        } else if (holder is NormalViewHolder) {
            holder.title.text = item.displayNome

            val ageStr = if (item.idadeMin != null && item.idadeMin > 0) " · ${item.idadeMin}+ anos" else ""
            holder.meta.text = "${item.categoria.uppercase(Locale.getDefault())} · ${item.displayDuracao} min$ageStr"

            holder.price.text = if (item.precoCentavos == 0L) "Grátis" else "R$ ${String.format(Locale.getDefault(), "%,d", item.precoCentavos / 100)}"
            holder.spots.text = "Reservar"

            if (!item.fotoUrl.isNullOrEmpty()) {
                holder.photo.load(item.fotoUrl) {
                    crossfade(true)
                }
            }

            holder.itemView.setOnClickListener {
                onItemClick?.invoke(item)
            }
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<AtividadeItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}
