package com.example.hugabugahotel.ui.account

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.hugabugahotel.R
import com.example.hugabugahotel.data.model.PagamentoResumoDto
import com.example.hugabugahotel.util.Formatos

/** Histórico de pagamentos (GET /me/pagamentos). O app nunca guarda cartão: só final e bandeira. */
class SavedPaymentsAdapter(private var items: List<PagamentoResumoDto>) :
    RecyclerView.Adapter<SavedPaymentsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvPaymentTitle: TextView = view.findViewById(R.id.tvPaymentTitle)
        val tvPaymentDetails: TextView = view.findViewById(R.id.tvPaymentDetails)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_saved_payment, parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val p = items[position]
        val metodo = when (p.metodo) {
            "CARTAO" -> "Cartão ${p.cartaoBandeira.orEmpty()} final ${p.cartaoFinal.orEmpty()}".trim()
            "PIX" -> "Pix"
            else -> "Na chegada"
        }
        holder.tvPaymentTitle.text = "$metodo · ${Formatos.reais(p.valorCentavos)}"
        holder.tvPaymentDetails.text = listOfNotNull(
            p.reservaCodigo?.let { "Reserva $it" },
            Formatos.status(p.status),
            p.parcelas?.takeIf { it > 1 }?.let { "${it}x" },
            Formatos.dataCompleta(p.criadoEm)
        ).joinToString(" · ")
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<PagamentoResumoDto>) {
        items = newItems
        notifyDataSetChanged()
    }
}
