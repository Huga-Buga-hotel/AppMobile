package com.example.hugabugahotel.ui.account

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.hugabugahotel.R
import com.example.hugabugahotel.data.api.SessionManager
import com.example.hugabugahotel.data.model.ReservaResponse
import com.example.hugabugahotel.ui.checkout.VoucherActivity
import com.example.hugabugahotel.ui.experiences.ExperienceScheduleActivity
import java.util.Locale

class BookingsAdapter(
    private var items: List<ReservaResponse>,
    private val currentTab: String = "proximas",
    private val isSelectionMode: Boolean = false,
    private val selectionButtonText: String = "Reservar",
    private val onVoucherClick: ((ReservaResponse) -> Unit)? = null,
    private val onManageClick: ((ReservaResponse) -> Unit)? = null,
    private val onCancelClick: ((ReservaResponse) -> Unit)? = null
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_booking_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val estadia = items[position]
        holder.tvBookingStatus.text = estadia.status
        holder.tvBookingCode.text = estadia.codigo
        holder.tvCavernTitle.text = estadia.cavernaNome
        holder.tvBookingDates.text = "📅 ${estadia.checkIn} – ${estadia.checkOut}"

        val sessionManager = SessionManager(holder.itemView.context)
        val baseCentavos = if (estadia.totalCentavos > 0) estadia.totalCentavos else 156000L
        val extraCentavos = sessionManager.getExtraExpenses(estadia.codigo)
        val finalCentavos = baseCentavos + extraCentavos

        val totalReais = String.format(Locale("pt", "BR"), "R$ %.2f", finalCentavos / 100.0)
        val qtdPessoas = if (estadia.adultos > 0) "${estadia.adultos} pessoas" else "2 pessoas"
        holder.tvBookingTotalAndPeople.text = "💰 Total: $totalReais   👥 $qtdPessoas na tribo"

        if (isSelectionMode) {
            holder.btnCancelBooking.visibility = View.GONE
            holder.btnManageExperiences.visibility = View.GONE
            holder.btnVoucher.visibility = View.VISIBLE
            (holder.btnVoucher as? TextView)?.text = selectionButtonText
            holder.btnVoucher.setOnClickListener {
                onVoucherClick?.invoke(estadia)
            }
            holder.itemView.setOnClickListener {
                onVoucherClick?.invoke(estadia)
            }
        } else {
            (holder.btnVoucher as? TextView)?.text = holder.itemView.context.getString(R.string.voucher_title)
            holder.itemView.setOnClickListener(null)

            if (currentTab == "proximas") {
                holder.btnManageExperiences.visibility = View.VISIBLE
                holder.btnCancelBooking.visibility = View.VISIBLE

                holder.btnManageExperiences.setOnClickListener {
                    if (onManageClick != null) {
                        onManageClick.invoke(estadia)
                    } else {
                        val context = holder.itemView.context
                        val intent = Intent(context, com.example.hugabugahotel.ui.experiences.ManageBookingActivity::class.java).apply {
                            putExtra("RESERVA_CODIGO", estadia.codigo)
                            putExtra("CAVERN_NOME", estadia.cavernaNome)
                            putExtra("CHECK_IN", estadia.checkIn)
                            putExtra("CHECK_OUT", estadia.checkOut)
                            putExtra("TOTAL_CENTAVOS", estadia.totalCentavos)
                        }
                        context.startActivity(intent)
                    }
                }

                holder.btnCancelBooking.setOnClickListener {
                    onCancelClick?.invoke(estadia)
                }
            } else {
                holder.btnManageExperiences.visibility = View.GONE
                holder.btnCancelBooking.visibility = View.GONE
            }

            holder.btnVoucher.setOnClickListener {
                if (onVoucherClick != null) {
                    onVoucherClick.invoke(estadia)
                } else {
                    val context = holder.itemView.context
                    val intent = Intent(context, VoucherActivity::class.java).apply {
                        putExtra("RESERVA_CODIGO", estadia.codigo)
                        putExtra("CAVERN_NOME", estadia.cavernaNome)
                        putExtra("CHECK_IN", estadia.checkIn)
                        putExtra("CHECK_OUT", estadia.checkOut)
                        putExtra("ADULTS", estadia.adultos)
                        putExtra("TOTAL_CENTAVOS", finalCentavos)
                    }
                    context.startActivity(intent)
                }
            }
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<ReservaResponse>) {
        items = newItems
        notifyDataSetChanged()
    }
}
