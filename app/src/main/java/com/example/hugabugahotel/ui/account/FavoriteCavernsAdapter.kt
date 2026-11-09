package com.example.hugabugahotel.ui.account

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.hugabugahotel.R
import com.example.hugabugahotel.data.model.CavernSummary
import com.example.hugabugahotel.ui.caverns.CavernDetailActivity
import com.example.hugabugahotel.util.LanguageManager
import java.util.Locale

class FavoriteCavernsAdapter(
    initialItems: List<CavernSummary> = emptyList(),
    private val onRemoveFavorite: ((CavernSummary) -> Unit)? = null
) : ListAdapter<CavernSummary, FavoriteCavernsAdapter.ViewHolder>(CavernDiffCallback) {

    init {
        if (initialItems.isNotEmpty()) {
            submitList(initialItems)
        }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivCavernPhoto: ImageView? = view.findViewById(R.id.ivCavernPhoto)
        val ivPlaceholderIcon: ImageView? = view.findViewById(R.id.ivPlaceholderIcon)
        val tvCavernName: TextView = view.findViewById(R.id.tvCavernName)
        val tvRating: TextView = view.findViewById(R.id.tvRating)
        val tvSpecs: TextView = view.findViewById(R.id.tvSpecs)
        val tvPrice: TextView = view.findViewById(R.id.tvPrice)
        val tvTotalPrice: TextView = view.findViewById(R.id.tvTotalPrice)
        val badgeScarcity: TextView = view.findViewById(R.id.badgeScarcity)
        val btnFavorite: ImageView = view.findViewById(R.id.btnFavorite)
        val btnViewCavern: View = view.findViewById(R.id.btnViewCavern)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cavern_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val cavern = getItem(position)
        val context = holder.itemView.context

        if (!cavern.fotoUrl.isNullOrEmpty()) {
            holder.ivCavernPhoto?.load(cavern.fotoUrl) {
                crossfade(true)
                listener(
                    onSuccess = { _, _ -> holder.ivPlaceholderIcon?.visibility = View.GONE },
                    onError = { _, _ -> holder.ivPlaceholderIcon?.visibility = View.VISIBLE }
                )
            }
        } else {
            holder.ivCavernPhoto?.setImageDrawable(null)
            holder.ivPlaceholderIcon?.visibility = View.VISIBLE
        }

        holder.tvCavernName.text = cavern.nome
        holder.tvRating.text = String.format(Locale.getDefault(), "%.1f", cavern.notaMedia)

        val specsFormat = LanguageManager.get(context, "specs_format")
        holder.tvSpecs.text = String.format(Locale.getDefault(), specsFormat, cavern.capacidade, cavern.camas, cavern.areaM2)

        holder.tvPrice.text = "R$ ${String.format(Locale.getDefault(), "%,d", cavern.precoBaseCentavos / 100)}"

        val totalSuffix = LanguageManager.get(context, "total_suffix")
        holder.tvTotalPrice.text = "R$ ${String.format(Locale.getDefault(), "%,d", (cavern.totalEstadiaCentavos ?: cavern.precoBaseCentavos * 3) / 100)} $totalSuffix"

        holder.badgeScarcity.visibility = if (cavern.ultimas) View.VISIBLE else View.GONE
        holder.btnFavorite.setImageResource(R.drawable.ic_heart)

        holder.btnFavorite.setOnClickListener {
            onRemoveFavorite?.invoke(cavern)
        }

        if (holder.btnViewCavern is TextView) {
            holder.btnViewCavern.text = LanguageManager.get(context, "btn_view_cavern")
        }

        val openDetail = View.OnClickListener {
            val intent = Intent(context, CavernDetailActivity::class.java).apply {
                putExtra("CAVERN_SLUG", cavern.slug)
            }
            context.startActivity(intent)
        }
        holder.itemView.setOnClickListener(openDetail)
        holder.btnViewCavern.setOnClickListener(openDetail)
    }

    fun updateData(newItems: List<CavernSummary>) {
        submitList(newItems)
    }

    companion object {
        private val CavernDiffCallback = object : DiffUtil.ItemCallback<CavernSummary>() {
            override fun areItemsTheSame(oldItem: CavernSummary, newItem: CavernSummary): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: CavernSummary, newItem: CavernSummary): Boolean {
                return oldItem == newItem
            }
        }
    }
}
