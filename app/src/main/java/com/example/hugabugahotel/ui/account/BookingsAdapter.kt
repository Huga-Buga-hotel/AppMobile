package com.example.hugabugahotel.ui.account

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.hugabugahotel.R
import com.example.hugabugahotel.data.model.ReservaResponse
import com.example.hugabugahotel.data.model.ReservaResumoDto
import com.example.hugabugahotel.ui.checkout.VoucherActivity
import com.example.hugabugahotel.ui.experiences.ManageBookingActivity
import com.example.hugabugahotel.util.Formatos

/** Uma estadia na lista, venha de /me/reservas (com conta) ou de /reservas/{codigo} (sem conta). */
data class BookingItem(
    val codigo: String,
    val cavernaNome: String,
    val checkIn: String,
    val checkOut: String,
    val pessoas: Int,
    val total: Long,
    val status: String,
    val pago: Boolean,
    val diasParaCheckIn: Int?,
    val podeAvaliar: Boolean
) {
    companion object {
        fun de(r: ReservaResumoDto) = BookingItem(r.codigo, r.cavernaNome.orEmpty(), r.checkIn, r.checkOut,
            r.adultos + r.criancas, r.total, r.status, r.pago, r.diasParaCheckIn, r.podeAvaliar)

        /** Sem conta não há avaliação (a API exige a conta que reservou). */
        fun de(r: ReservaResponse) = BookingItem(r.codigo, r.cavernaNome, r.checkIn, r.checkOut,
            r.adultos + r.criancas, r.total, r.status, r.pago, null, false)
    }
}

class BookingsAdapter(
    private var items: List<BookingItem>,
    private val currentTab: String = "proximas",
    private val isSelectionMode: Boolean = false,
    private val selectionButtonText: String = "Escolher",
    private val onSelect: ((BookingItem) -> Unit)? = null,
    private val onCancelClick: ((BookingItem) -> Unit)? = null,
    private val onReviewClick: ((BookingItem) -> Unit)? = null
) : RecyclerView.Adapter<BookingsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvBookingStatus: TextView = view.findViewById(R.id.tvBookingStatus)
        val tvBookingCode: TextView = view.findViewById(R.id.tvBookingCode)
        val tvCavernTitle: TextView = view.findViewById(R.id.tvCavernTitle)
        val tvBookingDates: TextView = view.findViewById(R.id.tvBookingDates)
        val tvBookingTotalAndPeople: TextView = view.findViewById(R.id.tvBookingTotalAndPeople)
        val btnVoucher: View = view.findViewById(R.id.btnVoucher)
        val btnManageExperiences: View = view.findViewById(R.id.btnManageExperiences)
        val btnCancelBooking: TextView = view.findViewById(R.id.btnCancelBooking)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_booking_card, parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val context = holder.itemView.context
        holder.tvBookingStatus.text = Formatos.status(item.status) +
            (item.diasParaCheckIn?.takeIf { it > 0 && currentTab == "proximas" }?.let { " · $it dias pra caverna" } ?: "")
        holder.tvBookingCode.text = item.codigo
        holder.tvCavernTitle.text = item.cavernaNome
        holder.tvBookingDates.text = "📅 ${Formatos.dataCompleta(item.checkIn)} – ${Formatos.dataCompleta(item.checkOut)}"
        holder.tvBookingTotalAndPeople.text =
            "💰 ${Formatos.reais(item.total)}${if (item.pago) " (pago)" else ""}   👥 ${item.pessoas} na tribo"

        if (isSelectionMode) {
            holder.btnCancelBooking.visibility = View.GONE
            holder.btnManageExperiences.visibility = View.GONE
            (holder.btnVoucher as? TextView)?.text = selectionButtonText
            holder.btnVoucher.setOnClickListener { onSelect?.invoke(item) }
            holder.itemView.setOnClickListener { onSelect?.invoke(item) }
            return
        }

        holder.itemView.setOnClickListener(null)
        (holder.btnVoucher as? TextView)?.text = context.getString(R.string.voucher_title)
        holder.btnVoucher.setOnClickListener {
            context.startActivity(Intent(context, VoucherActivity::class.java).putExtra("RESERVA_CODIGO", item.codigo))
        }

        val ativa = item.status == "CONFIRMADA" || item.status == "AGUARDANDO_PAGAMENTO"
        holder.btnManageExperiences.visibility = if (currentTab == "proximas" && item.status == "CONFIRMADA") View.VISIBLE else View.GONE
        holder.btnManageExperiences.setOnClickListener {
            context.startActivity(Intent(context, ManageBookingActivity::class.java).putExtra("RESERVA_CODIGO", item.codigo))
        }

        when {
            currentTab == "proximas" && ativa -> {
                holder.btnCancelBooking.visibility = View.VISIBLE
                holder.btnCancelBooking.text = "Cancelar"
                holder.btnCancelBooking.setOnClickListener { onCancelClick?.invoke(item) }
            }
            currentTab == "passadas" && item.podeAvaliar -> {
                holder.btnCancelBooking.visibility = View.VISIBLE
                holder.btnCancelBooking.text = "⭐ Avaliar estadia"
                holder.btnCancelBooking.setOnClickListener { onReviewClick?.invoke(item) }
            }
            else -> holder.btnCancelBooking.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<BookingItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}
