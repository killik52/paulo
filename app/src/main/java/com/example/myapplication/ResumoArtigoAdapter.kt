package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class ResumoArtigoAdapter(
    private var itens: List<ResumoArtigoItem>
    // Adicione um onItemClick se necessário
) : RecyclerView.Adapter<ResumoArtigoAdapter.ViewHolder>() {

    private val decimalFormat = DecimalFormat("R$ #,##0.00", DecimalFormatSymbols(Locale("pt", "BR")))

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textViewNomeArtigo: TextView = view.findViewById(R.id.textViewNomeArtigoResumo)
        val textViewQuantidade: TextView = view.findViewById(R.id.textViewQuantidadeVendidaArtigo)
        val textViewTotalVendido: TextView = view.findViewById(R.id.textViewTotalVendidoArtigo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_resumo_artigo, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = itens[position]
        holder.textViewNomeArtigo.text = item.nomeArtigo
        holder.textViewQuantidade.text = "Quantidade Vendida: ${item.quantidadeTotalVendida}"
        holder.textViewTotalVendido.text = "Total Vendido: ${decimalFormat.format(item.valorTotalVendido)}"
        // holder.itemView.setOnClickListener { onItemClick(item) } // Se houver clique
    }

    override fun getItemCount() = itens.size

    fun updateData(newItens: List<ResumoArtigoItem>) {
        itens = newItens
        notifyDataSetChanged()
    }
}