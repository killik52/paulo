package com.example.myapplication

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

// 1. Classe adaptadora para o RecyclerView que exibe uma lista de faturas resumidas
class FaturaRecyclerAdapter(
    private val context: Context,
    private val faturas: List<FaturaResumidaItem>
) : RecyclerView.Adapter<FaturaRecyclerAdapter.FaturaViewHolder>() {

    // 2. Formato para valores monetários no padrão brasileiro
    private val decimalFormat = DecimalFormat("R$ #,##0.00", DecimalFormatSymbols(Locale("pt", "BR")).apply {
        decimalSeparator = ','
        groupingSeparator = '.'
    })

    // 3. Método chamado para criar uma nova view para cada item da lista
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FaturaViewHolder {
        // 4. Infla o layout do item da fatura
        val view = LayoutInflater.from(context).inflate(R.layout.item_fatura, parent, false)
        // 5. Retorna uma nova instância do ViewHolder com a view inflada
        return FaturaViewHolder(view)
    }

    // 6. Método chamado para vincular os dados da fatura à view
    override fun onBindViewHolder(holder: FaturaViewHolder, position: Int) {
        // 7. Obtém a fatura na posição especificada
        val fatura = faturas[position]
        // 8. Define o número da fatura
        holder.faturaNumero.text = fatura.numeroFatura
        // 9. Define o nome do cliente
        holder.faturaCliente.text = "Cliente: ${fatura.cliente}"
        // 10. Define a data da fatura
        holder.faturaData.text = "Data: ${fatura.data}"
        // 11. Define o valor do saldo devedor formatado
        holder.faturaValor.text = decimalFormat.format(fatura.saldoDevedor)
    }

    // 12. Método que retorna o número total de faturas na lista
    override fun getItemCount(): Int = faturas.size

    // 13. Classe ViewHolder que armazena as referências aos componentes da view
    class FaturaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // 14. Referência ao TextView para o número da fatura
        val faturaNumero: TextView = itemView.findViewById(R.id.faturaNumero)
        // 15. Referência ao TextView para o nome do cliente
        val faturaCliente: TextView = itemView.findViewById(R.id.faturaCliente)
        // 16. Referência ao TextView para a data da fatura
        val faturaData: TextView = itemView.findViewById(R.id.faturaData)
        // 17. Referência ao TextView para o valor do saldo devedor
        val faturaValor: TextView = itemView.findViewById(R.id.faturaValor)
    }
}