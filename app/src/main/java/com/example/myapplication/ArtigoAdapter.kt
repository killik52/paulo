package com.example.myapplication

import android.content.Context
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.parcelize.Parcelize

class ArtigoAdapter(
    private val context: Context,
    private val artigos: MutableList<ArtigoItem>,
    private val onEdit: (Int) -> Unit,
    private val onDelete: (Int) -> Unit,
    private val onLongPress: (Int) -> Unit
) : RecyclerView.Adapter<ArtigoAdapter.ArtigoViewHolder>() {

    // 1. Cria e retorna uma nova view para cada item da lista
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArtigoViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_artigo, parent, false)
        return ArtigoViewHolder(view)
    }

    // 2. Vincula os dados do artigo à view correspondente
    override fun onBindViewHolder(holder: ArtigoViewHolder, position: Int) {
        val artigo = artigos[position]
        holder.artigoNome.text = artigo.nome
        holder.artigoPreco.text = "R$ ${artigo.preco}"
        holder.artigoQuantidade.text = "Quantidade: ${artigo.quantidade}"
        holder.artigoDescricao.text = "Descrição: ${artigo.descricao ?: "Nenhuma descrição"}"
        holder.artigoNumeroSerial.text = "Número Serial: ${artigo.numeroSerial ?: "Sem número serial"}"

        holder.itemView.setOnClickListener { onEdit(position) }
        holder.itemView.setOnLongClickListener {
            onLongPress(position)
            true
        }
    }

    // 3. Retorna o número total de itens na lista
    override fun getItemCount(): Int = artigos.size

    // 4. Remove um item da lista e executa a ação de exclusão
    fun removeItem(position: Int) {
        onDelete(position)
    }

    // 5. Define o ViewHolder para armazenar referências aos componentes da view
    class ArtigoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val artigoNome: TextView = itemView.findViewById(R.id.artigoNomeTextView)
        val artigoPreco: TextView = itemView.findViewById(R.id.artigoPrecoTextView)
        val artigoQuantidade: TextView = itemView.findViewById(R.id.artigoQuantidadeTextView)
        val artigoDescricao: TextView = itemView.findViewById(R.id.artigoDescricaoTextView)
        val artigoNumeroSerial: TextView = itemView.findViewById(R.id.artigoNumeroSerialTextView)
    }
}

// 6. Define modelo de dados para ArtigoItem com serialização
@Parcelize
data class ArtigoItem(
    val id: Long,
    val nome: String,
    val quantidade: Int,
    val preco: Double,
    val numeroSerial: String?,
    val descricao: String?
) : Parcelable