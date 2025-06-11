package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class ResumoClienteAdapter(
    private var itens: List<ResumoClienteItem>
    // Adicione um onItemClick se necessário para ir para detalhes do cliente
) : RecyclerView.Adapter<ResumoClienteAdapter.ViewHolder>() {

    private val decimalFormat = DecimalFormat("R$ #,##0.00", DecimalFormatSymbols(Locale("pt", "BR")))

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textViewNomeCliente: TextView = view.findViewById(R.id.textViewNomeClienteResumo)
        val textViewTotalGasto: TextView = view.findViewById(R.id.textViewTotalGastoCliente)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_resumo_cliente, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = itens[position]
        holder.textViewNomeCliente.text = item.nomeCliente
        holder.textViewTotalGasto.text = "Total Gasto: ${decimalFormat.format(item.totalGasto)}"
        // holder.itemView.setOnClickListener { onItemClick(item) } // Se houver clique
    }

    override fun getItemCount() = itens.size

    fun updateData(newItens: List<ResumoClienteItem>) {
        itens = newItens
        notifyDataSetChanged()
    }
}