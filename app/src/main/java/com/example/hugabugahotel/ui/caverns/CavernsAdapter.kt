package com.example.hugabugahotel.ui.caverns

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
import com.example.hugabugahotel.util.LanguageManager
import java.util.Locale

class CavernsAdapter(
    initialItems: List<CavernSummary> = emptyList(),
    private val favoriteIds: MutableSet<String> = mutableSetOf(),
    private val onFavoriteClicked: ((cavern: CavernSummary, isFavorite: Boolean) -> Unit)? = null,
    private val onCavernClicked: ((cavern: CavernSummary) -> Unit)? = null
) : ListAdapter<CavernSummary, CavernsAdapter.ViewHolder>(CavernDiffCallback) {

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

        // Carregamento de imagem com Coil
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

        holder.tvPrice.text = com.example.hugabugahotel.util.Formatos.reais(cavern.precoBaseCentavos)

        val totalSuffix = LanguageManager.get(context, "total_suffix")
        // Só há total da estadia quando a busca tem datas; sem datas mostra o preço da noite
        holder.tvTotalPrice.text = cavern.totalEstadiaCentavos
            ?.let { "${com.example.hugabugahotel.util.Formatos.reais(it)} $totalSuffix" } ?: "por noite"

        holder.badgeScarcity.visibility = if (cavern.ultimas) View.VISIBLE else View.GONE

        if (holder.btnViewCavern is TextView) {
            holder.btnViewCavern.text = LanguageManager.get(context, "btn_view_cavern")
        }

        val isFavorited = favoriteIds.contains(cavern.id)
        holder.btnFavorite.setImageResource(
            if (isFavorited) R.drawable.ic_heart else R.drawable.ic_heart_outline
        )

        holder.btnFavorite.setOnClickListener {
            val newFavState = !favoriteIds.contains(cavern.id)
            if (newFavState) {
                favoriteIds.add(cavern.id)
            } else {
                favoriteIds.remove(cavern.id)
            }
            holder.btnFavorite.setImageResource(
                if (newFavState) R.drawable.ic_heart else R.drawable.ic_heart_outline
            )
            onFavoriteClicked?.invoke(cavern, newFavState)
        }

        val openDetail = View.OnClickListener {
            if (onCavernClicked != null) {
                onCavernClicked.invoke(cavern)
            } else {
                val intent = Intent(context, CavernDetailActivity::class.java).apply {
                    putExtra("CAVERN_SLUG", cavern.slug)
                    if (context is CavernsListActivity) {
                        if (context.checkIn.isNotEmpty()) putExtra("CHECK_IN", context.checkIn)
                        if (context.checkOut.isNotEmpty()) putExtra("CHECK_OUT", context.checkOut)
                        if (context.checkInIso.isNotEmpty()) putExtra("CHECK_IN_ISO", context.checkInIso)
                        if (context.checkOutIso.isNotEmpty()) putExtra("CHECK_OUT_ISO", context.checkOutIso)
                        putExtra("ADULTS", context.adults)
                        putExtra("CHILDREN", context.children)
                    }
                }
                context.startActivity(intent)
            }
        }
        holder.itemView.setOnClickListener(openDetail)
        holder.btnViewCavern.setOnClickListener(openDetail)
    }

    fun updateData(newItems: List<CavernSummary>) {
        submitList(newItems)
    }

    fun setFavoriteIds(newIds: Set<String>) {
        favoriteIds.clear()
        favoriteIds.addAll(newIds)
        notifyDataSetChanged()
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
