package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class ResumoMensalAdapter(
    private var itens: List<ResumoMensalItem>,
    private val onItemClick: (ResumoMensalItem) -> Unit
) : RecyclerView.Adapter<ResumoMensalAdapter.ViewHolder>() {

    private val decimalFormat = DecimalFormat("R$ #,##0.00", DecimalFormatSymbols(Locale("pt", "BR")).apply {
        decimalSeparator = ','
        groupingSeparator = '.'
    })

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // Agora referenciando os IDs do layout item_fatura.xml
        val faturaNumero: TextView = view.findViewById(R.id.faturaNumero)
        val faturaCliente: TextView = view.findViewById(R.id.faturaCliente) // Não será usado diretamente para o mês/ano
        val faturaData: TextView = view.findViewById(R.id.faturaData)     // Não será usado diretamente para o mês/ano
        val faturaValor: TextView = view.findViewById(R.id.faturaValor)
        val faturaStatusEnviado: TextView = view.findViewById(R.id.faturaStatusEnviado) // Não será usado
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_resumo_mensal, parent, false) // Note que ainda referencia item_resumo_mensal
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = itens[position]
        // Mapeando os dados de ResumoMensalItem para os campos do layout de fatura
        holder.faturaNumero.text = "Faturamento: ${item.mesAno}" // Exibindo o mês/ano como o "número da fatura"
        holder.faturaCliente.text = "" // Limpa o campo do cliente, pois não é relevante aqui
        holder.faturaCliente.visibility = View.GONE // Oculta o TextView
        holder.faturaData.text = "" // Limpa o campo de data, pois não é relevante aqui
        holder.faturaData.visibility = View.GONE // Oculta o TextView
        holder.faturaValor.text = decimalFormat.format(item.valorTotal)
        holder.faturaStatusEnviado.visibility = View.GONE // Sempre oculto para resumo mensal

        holder.itemView.setOnClickListener { onItemClick(item) }
    }

    override fun getItemCount() = itens.size

    fun updateData(newItens: List<ResumoMensalItem>) {
        itens = newItens
        notifyDataSetChanged()
    }
}